package com.saitechnology.investo.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.saitechnology.investo.R;
import com.saitechnology.investo.util.ProgressUtils;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class PLStatementActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    boolean isProgressVisible = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cash_flow_analysis);

        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        progressBar = findViewById(R.id.idPBLoading);

        MobileAds.initialize(this, initializationStatus -> {});
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        Button gotoHomeButton = findViewById(R.id.gotoHomeButton);
        gotoHomeButton.setOnClickListener(view -> {
            isProgressVisible = ProgressUtils.toggleProgressVisibility(progressBar, isProgressVisible);
            startActivity(new Intent(PLStatementActivity.this, MainActivity.class));
            finish();
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Fetch transaction data from Firebase
        assert user != null;
        FirebaseDatabase.getInstance().getReference().child("TransactionHistory")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // Process the transaction data here
                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            try {
                                if (Objects.equals(userSnapshot.getKey(), user.getUid())) {
                                    Map<String, Double> analysis = calculateAnalysis(userSnapshot);
                                    displayAnalysis(analysis);
                                }
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle database error
                        Log.e("PLStatementActivity", "Database error: " + databaseError.getMessage());
                        Toast.makeText(PLStatementActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Map<String, Double> calculateAnalysis(DataSnapshot userSnapshot) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date startDate = null;
        Date endDate = null;
        // Get start and end dates from intent extras
        Intent intent = getIntent();
        String startDateString = intent.getStringExtra("START_DATE");
        String endDateString = intent.getStringExtra("END_DATE");

        if (startDateString != null && endDateString != null) {
            startDate = dateFormat.parse(startDateString);
            // Adjust the end date to the end of the selected day
            Calendar endCalendar = Calendar.getInstance();
            endCalendar.setTime(Objects.requireNonNull(dateFormat.parse(endDateString)));
            endCalendar.add(Calendar.DAY_OF_MONTH, 1); // Add one calendar day
            endDate = endCalendar.getTime();
        } else {
            // Handle null start or end date string
            Log.e("PLStatementActivity", "Start or end date string is null");
            return new HashMap<>(); // Return empty analysis
        }

        double totalCashIn = 0;
        double totalCashOut = 0;

        for (DataSnapshot transactionSnapshot : userSnapshot.getChildren()) {
            Object transactionValue = transactionSnapshot.getValue();
            if (transactionValue instanceof Map) {
                Map<String, Object> transaction = (Map<String, Object>) transactionValue;
                String cashEntry = (String) transaction.get("cashEntry");
                String totalValue = (String) transaction.get("totalValue");
                String transactionTime = transactionSnapshot.child("transactionTime").getValue(String.class);
                String paymentStatus = (String) transaction.get("paymentStatus");

                // Check if payment status is blank, empty, or "Cancelled"
                if (paymentStatus == null || paymentStatus.isEmpty() || paymentStatus.equals("Cancelled")) {
                    continue; // Skip this transaction
                }

                if (transactionTime != null && isWithinDateRange(transactionTime, startDate, endDate)) {
                    if (cashEntry != null && totalValue != null) {
                        try {
                            double value = Double.parseDouble(totalValue);
                            if ("Cash In +".equals(cashEntry)) {
                                totalCashIn += value;
                            } else if ("Cash Out -".equals(cashEntry)) {
                                totalCashOut += value;
                            }
                        } catch (NumberFormatException e) {
                            // Log error or handle invalid number format
                            Log.e("CashFlowAnalysis", "Invalid number format: " + totalValue, e);
                        }
                    } else {
                        // Log error for missing values
                        Log.e("CashFlowAnalysis", "Missing cash entry or total value");
                    }
                }
            } else {
                // Log error for unexpected data type
                assert transactionValue != null;
                Log.e("CashFlowAnalysis", "Unexpected data type for transaction: " + transactionValue.getClass().getSimpleName());
            }
        }

        double totalProfit = totalCashIn + totalCashOut;

        Map<String, Double> analysis = new HashMap<>();
        analysis.put("totalCashIn", totalCashIn);
        analysis.put("totalCashOut", totalCashOut);
        analysis.put("totalProfit", totalProfit);

        return analysis;
    }

    private boolean isWithinDateRange(String transactionTime, Date startDate, Date endDate) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = dateFormat.parse(transactionTime);
            assert date != null;
            return date.after(startDate) && date.before(endDate);
        } catch (ParseException e) {
            e.getMessage();
            return false;
        }
    }


    @SuppressLint("SetTextI18n")
    private void displayAnalysis(Map<String, Double> analysis) {

        Intent intent = getIntent();
        String startDateString = intent.getStringExtra("START_DATE");
        String endDateString = intent.getStringExtra("END_DATE");

        TextView startEndDate = findViewById(R.id.start_end_date);
        startEndDate.setText("From : "+ startDateString +" To : "+ endDateString);

        DecimalFormat decimalFormat = new DecimalFormat("#.##");

        TextView cashInTextView = findViewById(R.id.total_cash_in);
        cashInTextView.setText(decimalFormat.format(analysis.get("totalCashIn")));

        TextView cashOutTextView = findViewById(R.id.total_cash_out);
        cashOutTextView.setText(decimalFormat.format(analysis.get("totalCashOut")));

        TextView profitTextView = findViewById(R.id.total_profit);
        profitTextView.setText(decimalFormat.format(analysis.get("totalProfit")));
    }
}
