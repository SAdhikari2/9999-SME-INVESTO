package com.saitechnology.investo.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.saitechnology.investo.R;

public class InvestmentDetailActivity extends AppCompatActivity {

    private TextView accountNumberView, depositAmountView, maturityAmountView, bankNameView,
            branchNameView, depositDateView, maturityDateView, statusView, remarksView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_investment_detail);

        // Initialize UI components
        accountNumberView = findViewById(R.id.accountNumberView);
        depositAmountView = findViewById(R.id.depositAmountView);
        maturityAmountView = findViewById(R.id.maturityAmountView);
        bankNameView = findViewById(R.id.bankNameView);
        branchNameView = findViewById(R.id.branchNameView);
        depositDateView = findViewById(R.id.depositDateView);
        maturityDateView = findViewById(R.id.maturityDateView);
        statusView = findViewById(R.id.statusView);
        remarksView = findViewById(R.id.remarksView);

        // Get the current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            loadInvestmentDetails(user.getUid());
        }
        // Add this in the InvestmentDetailActivity where you want to trigger the update
        Button updateDetailsButton = findViewById(R.id.updateButton);
        updateDetailsButton.setOnClickListener(v -> {
            Intent intent = new Intent(InvestmentDetailActivity.this, UpdateInvestmentActivity.class);
            intent.putExtra("investmentId", getIntent().getStringExtra("investmentId"));
            startActivity(intent);
        });

    }

    private void loadInvestmentDetails(String userId) {
        String investmentId = getIntent().getStringExtra("investmentId"); // Get the investmentId from the intent
        if (investmentId == null) {
            Toast.makeText(this, "Investment ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                .child("InvestmentWarehouse")
                .child(userId)
                .child(investmentId); // Access the specific investment ID

        databaseReference.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String accountNumber = snapshot.child("accountNumber").getValue(String.class);
                    String depositAmount = snapshot.child("depositAmount").getValue(String.class);
                    String maturityAmount = snapshot.child("maturityAmount").getValue(String.class);
                    String bankName = snapshot.child("bankName").getValue(String.class);
                    String branchName = snapshot.child("branchName").getValue(String.class);
                    String depositDate = snapshot.child("depositDate").getValue(String.class);
                    String maturityDate = snapshot.child("maturityDate").getValue(String.class);
                    String status = snapshot.child("status").getValue(String.class);
                    String remarks = snapshot.child("remarks").getValue(String.class);

                    // Set data to TextViews
                    accountNumberView.setText("Account Number: " + accountNumber);
                    depositAmountView.setText("Deposit Amount: " + depositAmount);
                    maturityAmountView.setText("Maturity Amount: " + maturityAmount);
                    bankNameView.setText("Bank Name: " + bankName);
                    branchNameView.setText("Branch Name: " + branchName);
                    depositDateView.setText("Deposit Date: " + depositDate);
                    maturityDateView.setText("Maturity Date: " + maturityDate);
                    statusView.setText("Status: " + status);
                    remarksView.setText("Remarks: " + remarks);
                } else {
                    Toast.makeText(InvestmentDetailActivity.this, "Investment details not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("InvestmentDetailActivity", "Database error: " + error.getMessage());
                Toast.makeText(InvestmentDetailActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
