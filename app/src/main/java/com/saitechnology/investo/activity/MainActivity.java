package com.saitechnology.investo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

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
import com.saitechnology.investo.util.ProgressUtils;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ProgressBar progressBar;
    boolean isProgressVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        progressBar = findViewById(R.id.idPBLoading);

        MobileAds.initialize(this, initializationStatus -> {});
        AdView adView1 = findViewById(R.id.adView1);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView1.loadAd(adRequest);

        assignClickListener(R.id.button_add_investment);
        assignClickListener(R.id.button_view_investment);
        assignClickListener(R.id.button_due);
    }

    private void assignClickListener(int id) {
        findViewById(id).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view instanceof AppCompatButton) {
            startActivity(new Intent(MainActivity.this, CashFlowAnalysisActivity.class));;
        }
    }

    private void saveTransactionAndNavigate(String userId, String buttonText, TransactionHistory transactionHistory) {
        Intent intentMain = new Intent(MainActivity.this, MainActivity.class);
        transactionHistory.setPaymentType(buttonText);
        transactionHistory.setUserId(userId);
        transactionHistory.setPaymentStatus(buttonText.equals("Cash") ? "Paid" : buttonText);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("TransactionHistory").child(userId);
        databaseReference.child(transactionHistory.getTransactionTime()).setValue(transactionHistory)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        isProgressVisible = ProgressUtils.toggleProgressVisibility(progressBar, isProgressVisible);
                        Toast.makeText(MainActivity.this, "Successfully Updated", Toast.LENGTH_SHORT).show();
                        startActivity(intentMain);
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to update transaction data", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
