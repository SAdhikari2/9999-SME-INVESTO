package com.saitechnology.investo.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.saitechnology.investo.R;
import com.saitechnology.investo.entity.TransactionHistory;

import java.util.HashMap;
import java.util.Map;

public class BusinessDetailsActivity extends AppCompatActivity {

    private EditText businessNameEditText, bankAccountNameEditText, upiIdEditText;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_details);

        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        // Initialize views
        businessNameEditText = findViewById(R.id.editText);
        bankAccountNameEditText = findViewById(R.id.editText1);
        upiIdEditText = findViewById(R.id.editText2);
        Button updateProfileButton = findViewById(R.id.updateProfile);

        MobileAds.initialize(this, initializationStatus -> {});
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // User is not logged in
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("TransactionHistory").child(currentUser.getUid());

        // Retrieve existing values from Firebase database and populate EditText fields
        retrieveProfileDetails();

        // Set click listener for the update profile button
        updateProfileButton.setOnClickListener(v -> updateProfile());
    }

    private void retrieveProfileDetails() {
        databaseReference.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                TransactionHistory userProfile = snapshot.getValue(TransactionHistory.class);
                if (userProfile != null) {
                    businessNameEditText.setText(userProfile.getBusinessName());
                    bankAccountNameEditText.setText(userProfile.getName());
                    upiIdEditText.setText(userProfile.getVpa());
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to retrieve profile details", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateProfile() {
        // Get user inputs
        String businessName = businessNameEditText.getText().toString().trim();
        String bankAccountName = bankAccountNameEditText.getText().toString().trim();
        String upiId = upiIdEditText.getText().toString().trim();

        // Validate inputs
        if (businessName.isEmpty() || bankAccountName.isEmpty() || upiId.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update user profile in Firebase database
        Map<String, Object> updates = new HashMap<>();
        updates.put("businessName", businessName);
        updates.put("name", bankAccountName);
        updates.put("vpa", upiId);

        databaseReference.updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(BusinessDetailsActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(BusinessDetailsActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

