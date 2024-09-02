package com.saitechnology.investo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.saitechnology.investo.R;
import com.saitechnology.investo.entity.TransactionHistory;
import com.saitechnology.investo.util.ProgressUtils;

public class PaymentActivity extends AppCompatActivity implements View.OnClickListener {

    private ProgressBar progressBar;
    boolean isProgressVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        progressBar = findViewById(R.id.idPBLoading);

        MobileAds.initialize(this, initializationStatus -> {});
        AdView adView1 = findViewById(R.id.adView1);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView1.loadAd(adRequest);

        assignClickListener(R.id.button_cash);
        assignClickListener(R.id.button_online);
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

        Intent intent = new Intent(PaymentActivity.this, QrActivity.class);
        TransactionHistory transactionHistory = (TransactionHistory) getIntent().getSerializableExtra("transactionHistoryKey");
        assert transactionHistory != null;
        intent.putExtra("transactionHistoryKey", transactionHistory);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (buttonText.equals("Online")) {
            areVpaAndNamePresent(user.getUid(), areFieldsPresent -> {
                if (!areFieldsPresent) {
                    showAddVpaDialog(user.getUid());
                } else {
                    isProgressVisible = ProgressUtils.toggleProgressVisibility(progressBar, isProgressVisible);
                    startQrActivity(intent, transactionHistory);
                }
            });
        } else if (buttonText.equals("Cash") || buttonText.equals("Due")) {
            saveTransactionAndNavigate(user.getUid(), buttonText, transactionHistory);
        }
    }

    private void startQrActivity(Intent intent, TransactionHistory transactionHistory) {
        transactionHistory.setPaymentType("Online");
        intent.putExtra("transactionHistoryKey", transactionHistory);
        startActivity(intent);
    }

    private void saveTransactionAndNavigate(String userId, String buttonText, TransactionHistory transactionHistory) {
        Intent intentMain = new Intent(PaymentActivity.this, MainActivity.class);
        transactionHistory.setPaymentType(buttonText);
        transactionHistory.setUserId(userId);
        transactionHistory.setPaymentStatus(buttonText.equals("Cash") ? "Paid" : buttonText);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("TransactionHistory").child(userId);
        databaseReference.child(transactionHistory.getTransactionTime()).setValue(transactionHistory)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        isProgressVisible = ProgressUtils.toggleProgressVisibility(progressBar, isProgressVisible);
                        Toast.makeText(PaymentActivity.this, "Successfully Updated", Toast.LENGTH_SHORT).show();
                        startActivity(intentMain);
                        finish();
                    } else {
                        Toast.makeText(PaymentActivity.this, "Failed to update transaction data", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(PaymentActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void areVpaAndNamePresent(String userId, VpaNamePresenceListener listener) {
        if (userId == null) {
            listener.onVpaNamePresence(false);
            return;
        }

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("TransactionHistory").child(userId);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean areFieldsPresent = dataSnapshot.child("name").exists() && dataSnapshot.child("vpa").exists();
                listener.onVpaNamePresence(areFieldsPresent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onVpaNamePresence(false);
            }
        });
    }

    interface VpaNamePresenceListener {
        void onVpaNamePresence(boolean areFieldsPresent);
    }

    private void showAddVpaDialog(String userId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_vpa, null);
        builder.setView(dialogView);

        EditText editTextVpa = dialogView.findViewById(R.id.edit_text_vpa);
        EditText editTextName = dialogView.findViewById(R.id.edit_text_name);
        TextView buttonCancel = dialogView.findViewById(R.id.button_cancel);
        TextView buttonConfirm = dialogView.findViewById(R.id.button_confirm);

        AlertDialog dialog = builder.create();

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        buttonConfirm.setOnClickListener(v -> {
            String vpa = editTextVpa.getText().toString().trim();
            String name = editTextName.getText().toString().trim();
            if (!vpa.isEmpty() && !name.isEmpty()) {
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("TransactionHistory").child(userId);
                userRef.child("vpa").setValue(vpa);
                userRef.child("name").setValue(name);

                Toast.makeText(this, "VPA and name added successfully", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please enter VPA and name", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
