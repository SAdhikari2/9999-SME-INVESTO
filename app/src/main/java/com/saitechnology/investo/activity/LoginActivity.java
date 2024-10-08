package com.saitechnology.investo.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.saitechnology.investo.R;
import com.saitechnology.investo.util.ProfileImageUtil;

import java.util.Calendar;
import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 20;
    private static final long FINGERPRINT_AUTH_TIMEOUT = 30 * 1000; // 30 seconds timeout

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private SharedPreferences sharedPreferences;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private ImageView userProfileIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase and UI components
        mAuth = FirebaseAuth.getInstance();

        userProfileIcon = findViewById(R.id.userProfileIcon);
        ProfileImageUtil.loadProfileImage(this, userProfileIcon);

        sharedPreferences = getSharedPreferences("login_info", MODE_PRIVATE);

        // Initialize Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set up Google Sign-In button
        LinearLayout googleSignInButton = findViewById(R.id.linearLayout3);
        googleSignInButton.setOnClickListener(v -> googleSignIn());

        // Initialize and set up "Use Fingerprint" link
        TextView useFingerprintLink = findViewById(R.id.useFingerprintLink);
        useFingerprintLink.setOnClickListener(v -> biometricPrompt.authenticate(promptInfo));

        // Set up Biometric Authentication
        setupBiometricAuthentication();
        ProfileImageUtil.setupProfileIconClick(this, userProfileIcon);
    }

    // Start Google Sign-In process
    private void googleSignIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }
    }

    // Authenticate with Firebase using Google credentials
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Save the last login timestamp
                            sharedPreferences.edit().putLong("last_login_timestamp", Calendar.getInstance().getTimeInMillis()).apply();
                            ProfileImageUtil.loadProfileImage(this, userProfileIcon);
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Biometric Authentication Setup
    private void setupBiometricAuthentication() {
        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(LoginActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                loginUserWithEmailPassword();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(LoginActivity.this, "Fingerprint Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });

        // Biometric Prompt setup
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Sign in using fingerprint")
                .setNegativeButtonText("Choose Another Account")
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        biometricPrompt.authenticate(promptInfo);

        // If user is already signed in, check for fingerprint authentication
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            long lastLoginTimestamp = sharedPreferences.getLong("last_login_timestamp", 0);
            long currentTime = Calendar.getInstance().getTimeInMillis();
            if ((currentTime - lastLoginTimestamp) < FINGERPRINT_AUTH_TIMEOUT) {
                // Automatically authenticate using fingerprint when the app starts
                biometricPrompt.authenticate(promptInfo);  // Authenticate using fingerprint
            } else {
                // If fingerprint timeout, load the user's profile picture and continue normally with Google Sign-In
                ProfileImageUtil.loadProfileImage(this, userProfileIcon);
            }
        }
    }


    // Log in the user using email/password (or other methods)
    private void loginUserWithEmailPassword() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    sharedPreferences.edit().putLong("last_login_timestamp", Calendar.getInstance().getTimeInMillis()).apply();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Failed to check user account status.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
