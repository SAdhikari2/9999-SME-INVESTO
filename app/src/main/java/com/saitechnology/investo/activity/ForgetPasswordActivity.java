package com.saitechnology.investo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.saitechnology.investo.R;
import com.saitechnology.investo.util.ProgressUtils;

import java.util.Objects;

public class ForgetPasswordActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private FirebaseAuth auth;
    private String email;
    private ProgressBar progressBar;
    boolean isProgressVisible = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        progressBar = findViewById(R.id.idPBLoading);

        MobileAds.initialize(this, initializationStatus -> {});

        AdView adView1 = findViewById(R.id.adView1);
        AdRequest adRequest1 = new AdRequest.Builder().build();
        adView1.loadAd(adRequest1);

        editTextEmail = findViewById(R.id.editTextEmail);
        Button forgetPasswordButton = findViewById(R.id.forgetPasswordButton);
        auth = FirebaseAuth.getInstance();

        forgetPasswordButton.setOnClickListener(view -> {
            isProgressVisible = ProgressUtils.toggleProgressVisibility(progressBar, isProgressVisible);
            validateData();
        });
    }

    private void validateData() {
        email = editTextEmail.getText().toString();
        if (email.isEmpty()) {
            editTextEmail.setError("Email is Required");
        } else {
            forgetPassword();
        }
    }

    private void forgetPassword() {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgetPasswordActivity.this, "Please check your email", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(ForgetPasswordActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(ForgetPasswordActivity.this, "Error : "+ Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
