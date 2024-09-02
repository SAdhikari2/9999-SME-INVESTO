package com.saitechnology.investo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.saitechnology.investo.R;

public class ContactUsActivity extends AppCompatActivity {

    // Change this value to your admin email address
    private static final String ADMIN_EMAIL = "prithvirajadhikari163@gmail.com";
    private static final String APP_NAME = "mPOS";
    private EditText issueEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_us);

        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        issueEditText = findViewById(R.id.messageEditText);
        Button sendButton = findViewById(R.id.sendButton);

        MobileAds.initialize(this, initializationStatus -> {});
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        sendButton.setOnClickListener(v -> sendEmail());

        // Configure Google API client
        configureGoogleApiClient();
    }

    private void configureGoogleApiClient() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        new GoogleApiClient.Builder(this)
                .enableAutoManage(this, connectionResult -> Toast.makeText(ContactUsActivity.this, "Google Play services connection failed.", Toast.LENGTH_SHORT).show())
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    private void sendEmail() {
        String issue = issueEditText.getText().toString().trim();
        if (issue.isEmpty()) {
            Toast.makeText(this, "Please enter your issue", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the currently signed-in user
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            Toast.makeText(this, "No user signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create an intent to send an email
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{ADMIN_EMAIL});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Issue reported in " + APP_NAME);
        emailIntent.putExtra(Intent.EXTRA_TEXT, issue);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send email"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }
}



