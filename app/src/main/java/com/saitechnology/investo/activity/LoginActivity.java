package com.saitechnology.investo.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.saitechnology.investo.R;
import com.saitechnology.investo.util.ProgressUtils;

import java.util.Calendar;
import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private static final long FINGERPRINT_AUTH_TIMEOUT = 30 * 1000; // 30 secs in milliseconds
    private EditText emailEditText, passwordEditText;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private GoogleSignInClient googleSignInClient;
    private ProgressBar progressBar;
    boolean isProgressVisible = false;
    int RC_SIGN_IN = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        // Initialize views
        progressBar = findViewById(R.id.idPBLoading);
        TextView googleAuth = findViewById(R.id.textView7);
        TextView forgetPassword = findViewById(R.id.textView5);
        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        Button loginButton = findViewById(R.id.loginButton);

        MobileAds.initialize(this, initializationStatus -> {});
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        // Initialize Firebase authentication
        mAuth = FirebaseAuth.getInstance();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("login_info", MODE_PRIVATE);

        // Initialize Google sign-in
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set click listeners
        forgetPassword.setOnClickListener(view -> {
            isProgressVisible = ProgressUtils.toggleProgressVisibility(progressBar, isProgressVisible);
            startActivity(new Intent(LoginActivity.this, ForgetPasswordActivity.class));
        });
        googleAuth.setOnClickListener(view -> googleSignIn());
        loginButton.setOnClickListener(v -> loginUser());

        // Initialize BiometricPrompt
        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(LoginActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                loginUserWithEmailPassword();
            }
        });

        // Configure BiometricPrompt
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Fingerprint Authentication")
                .setSubtitle("Log in using your fingerprint")
                .setNegativeButtonText("Cancel")
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isFingerprintAuthRequired()) {
            biometricPrompt.authenticate(promptInfo);
        } else {
            loginUser();
        }
    }

    private boolean isFingerprintAuthRequired() {
        long lastLoginTimestamp = sharedPreferences.getLong("last_login_timestamp", 0);
        long currentTime = Calendar.getInstance().getTimeInMillis();
        return (currentTime - lastLoginTimestamp) >= FINGERPRINT_AUTH_TIMEOUT;
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putLong("last_login_timestamp", Calendar.getInstance().getTimeInMillis());
                        editor.apply();
                        loginUserWithEmailPassword();
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loginUserWithEmailPassword() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    isProgressVisible = ProgressUtils.toggleProgressVisibility(progressBar, isProgressVisible);
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Failed to check user account status.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void googleSignIn(){
        Intent intent = googleSignInClient.getSignInIntent();
        startActivityForResult(intent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                fireBaseAuth(account.getIdToken());

            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fireBaseAuth(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        isProgressVisible = ProgressUtils.toggleProgressVisibility(progressBar, isProgressVisible);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    } else {
                        Toast.makeText(this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
