package com.saitechnology.investo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.saitechnology.investo.R;
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
            handleButtonClick((AppCompatButton) view);
        }
    }

    private void handleButtonClick(AppCompatButton button) {
        String buttonText = button.getText().toString();

        if (buttonText.equals("Add Investment")) {
            isProgressVisible = ProgressUtils.toggleProgressVisibility(progressBar, isProgressVisible);
            startActivity(new Intent(MainActivity.this, CashFlowAnalysisActivity.class));
        } else if (buttonText.equals("View Investment")) {
            isProgressVisible = ProgressUtils.toggleProgressVisibility(progressBar, isProgressVisible);
            startActivity(new Intent(MainActivity.this, CashFlowAnalysisActivity.class));
        }
    }
}
