package com.saitechnology.investo.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.saitechnology.investo.R;
import com.saitechnology.investo.entity.TransactionHistory;
import com.saitechnology.investo.util.ProgressUtils;
import com.saitechnology.investo.util.TransactionHistoryManager;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import lombok.NonNull;

public class QrActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "QRActivity";
    private ImageView qrCodeImageView;
    private TextView noQrMessageTextView;
    private ProgressBar progressBar;
    boolean isProgressVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);

        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        progressBar = findViewById(R.id.idPBLoading);
        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        noQrMessageTextView = findViewById(R.id.noQrMessageTextView);
        TextView amountTextView = findViewById(R.id.amountTextView);

        assignClickListener(R.id.paidButton);
        assignClickListener(R.id.cancelButton);

        MobileAds.initialize(this, initializationStatus -> {});
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("TransactionHistory").child(user.getUid());

        reference.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    TransactionHistory transactionHistory = (TransactionHistory) getIntent().getSerializableExtra("transactionHistoryKey");
                    assert transactionHistory != null;
                    @SuppressLint("DefaultLocale") String amount = String.format("%.2f", Double.parseDouble(transactionHistory.getTotalValue()));
                    String apiUrl = "https://upiqr.in/api/qr" +
                            "?vpa=" + dataSnapshot.child("vpa").getValue(String.class) +
                            "&amount=" + amount +
                            "&name=" + dataSnapshot.child("name").getValue(String.class);
                    amountTextView.setText("Amount: ₹" + amount);
                    new FetchQRCodeTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, apiUrl);
                } else {
                    Log.d(TAG, "No data found at the specified path");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
            }
        });
    }

    void assignClickListener(int id){
        findViewById(id).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        AppCompatButton button = (AppCompatButton) view;
        String buttonText = button.getText().toString();
        isProgressVisible = ProgressUtils.toggleProgressVisibility(progressBar, isProgressVisible);

        TransactionHistoryManager transactionHistoryManager = new TransactionHistoryManager();
        Intent intent = new Intent(QrActivity.this, MainActivity.class);
        TransactionHistory transactionHistory = (TransactionHistory) getIntent().getSerializableExtra("transactionHistoryKey");
        assert transactionHistory != null;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        writeTransactionDataToDatabase(transactionHistory, intent, user.getUid());

        transactionHistoryManager.updateTransactionField(
                transactionHistory.getUserId(),
                transactionHistory.getTransactionTime(),
                "paymentStatus",
                buttonText,
                new TransactionHistoryManager.TransactionUpdateListener() {
                    @Override
                    public void onTransactionUpdateSuccess() {
                        startActivity(intent);
                    }

                    @Override
                    public void onTransactionUpdateFailure(String errorMessage) {
                        Toast.makeText(QrActivity.this, "Failed to update payment status: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void writeTransactionDataToDatabase(TransactionHistory transactionHistory, Intent intent, String uid) {
        transactionHistory.setUserId(uid);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("TransactionHistory").child(uid);
        databaseReference.child(transactionHistory.getTransactionTime()).setValue(transactionHistory)
                .addOnCompleteListener(task -> {
                    isProgressVisible = ProgressUtils.toggleProgressVisibility(progressBar, isProgressVisible);
                    if (task.isSuccessful()) {
                        Toast.makeText(QrActivity.this, "Successfully Updated", Toast.LENGTH_SHORT).show();
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(QrActivity.this, "Failed to update transaction data", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(QrActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @SuppressLint("StaticFieldLeak")
    private class FetchQRCodeTask extends AsyncTask<String, Void, Drawable> {
        @Override
        protected Drawable doInBackground(String... urls) {
            String urlString = urls[0];
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    SVG svg = SVG.getFromInputStream(inputStream);
                    inputStream.close();
                    svg.setDocumentWidth("400px");
                    svg.setDocumentHeight("400px");
                    return new PictureDrawable(svg.renderToPicture());
                } else {
                    Log.e(TAG, "HTTP error code: " + urlConnection.getResponseCode());
                }
            } catch (IOException | SVGParseException e) {
                Log.e(TAG, "Error fetching QR code: " + e.getMessage());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            super.onPostExecute(drawable);
            if (drawable != null) {
                qrCodeImageView.setImageDrawable(drawable);
                qrCodeImageView.setVisibility(View.VISIBLE);
                noQrMessageTextView.setVisibility(View.GONE);
            } else {
                qrCodeImageView.setVisibility(View.GONE);
                noQrMessageTextView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isProgressVisible) {
            return;
        }
        super.onBackPressed();
    }
}
