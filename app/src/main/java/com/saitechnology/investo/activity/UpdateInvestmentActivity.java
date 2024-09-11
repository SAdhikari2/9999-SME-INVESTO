package com.saitechnology.investo.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.saitechnology.investo.util.ProfileImageUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class UpdateInvestmentActivity extends AppCompatActivity {

    private EditText accountNumberView, depositAmountView, maturityAmountView;
    private EditText bankNameView, branchNameView, depositDateView, maturityDateView;
    private EditText statusView, remarksView;
    private DatabaseReference databaseReference;
    private SimpleDateFormat dateFormat;
    private Calendar depositCalendar;
    private Calendar maturityCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_investment);

        // Initialize SimpleDateFormat
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Initialize Calendars
        depositCalendar = Calendar.getInstance();
        maturityCalendar = Calendar.getInstance();

        // Initialize views
        accountNumberView = findViewById(R.id.accountNumberView);
        depositAmountView = findViewById(R.id.depositAmountView);
        maturityAmountView = findViewById(R.id.maturityAmountView);
        bankNameView = findViewById(R.id.bankNameView);
        branchNameView = findViewById(R.id.branchNameView);
        depositDateView = findViewById(R.id.depositDateView);
        maturityDateView = findViewById(R.id.maturityDateView);
        statusView = findViewById(R.id.statusView);
        remarksView = findViewById(R.id.remarksView);

        ImageView userProfileIcon = findViewById(R.id.userProfileIcon);
        ProfileImageUtil.loadProfileImage(this, userProfileIcon);

        // Get investment ID passed from the previous activity
        String investmentId = getIntent().getStringExtra("investmentId");

        if (investmentId == null || investmentId.isEmpty()) {
            Toast.makeText(this, "Invalid investment ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get the current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            databaseReference = FirebaseDatabase.getInstance().getReference()
                    .child("InvestmentWarehouse")
                    .child(user.getUid())
                    .child(investmentId);

            loadInvestmentDetails();
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Set up date pickers
        depositDateView.setOnClickListener(v -> showDatePickerDialog(depositCalendar, depositDateView));
        maturityDateView.setOnClickListener(v -> showDatePickerDialog(maturityCalendar, maturityDateView));

        // Set up the update button click listener
        findViewById(R.id.updateButton).setOnClickListener(v -> updateInvestmentDetails());
        // Make the profile icon clickable
        ProfileImageUtil.setupProfileIconClick(this, userProfileIcon);
    }

    /**
     * Loads investment details from Firebase and populates the views.
     */
    private void loadInvestmentDetails() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    accountNumberView.setText(getStringValue(snapshot, "accountNumber"));
                    depositAmountView.setText(getStringValue(snapshot, "depositAmount"));
                    maturityAmountView.setText(getStringValue(snapshot, "maturityAmount"));
                    bankNameView.setText(getStringValue(snapshot, "bankName"));
                    branchNameView.setText(getStringValue(snapshot, "branchName"));
                    statusView.setText(getStringValue(snapshot, "status"));
                    remarksView.setText(getStringValue(snapshot, "remarks"));

                    // Parse and set deposit date
                    String depositDateStr = getStringValue(snapshot, "depositDate");
                    try {
                        depositCalendar.setTime(Objects.requireNonNull(dateFormat.parse(depositDateStr)));
                        depositDateView.setText(depositDateStr);
                    } catch (ParseException e) {
                        depositDateView.setText("");
                        e.printStackTrace();
                    }

                    // Parse and set maturity date
                    String maturityDateStr = getStringValue(snapshot, "maturityDate");
                    try {
                        maturityCalendar.setTime(dateFormat.parse(maturityDateStr));
                        maturityDateView.setText(maturityDateStr);
                    } catch (ParseException e) {
                        maturityDateView.setText("");
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(UpdateInvestmentActivity.this, "Investment details not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("UpdateInvestmentActivity", "Database error: " + error.getMessage());
                Toast.makeText(UpdateInvestmentActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Retrieves a string value from a DataSnapshot.
     */
    private String getStringValue(DataSnapshot snapshot, String key) {
        String value = snapshot.child(key).getValue(String.class);
        return value != null ? value : "";
    }

    /**
     * Shows a DatePickerDialog and sets the selected date to the provided EditText.
     */
    private void showDatePickerDialog(Calendar calendar, EditText dateEditText) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                UpdateInvestmentActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    calendar.set(selectedYear, selectedMonth, selectedDay);
                    String selectedDate = formatDate(selectedYear, selectedMonth, selectedDay);
                    dateEditText.setText(selectedDate);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    /**
     * Formats the date as yyyy-MM-dd.
     */
    private String formatDate(int year, int month, int day) {
        return String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day);
    }

    /**
     * Validates and updates investment details in Firebase.
     */
    private void updateInvestmentDetails() {
        String accountNumber = accountNumberView.getText().toString().trim();
        String depositAmountStr = depositAmountView.getText().toString().trim();
        String maturityAmountStr = maturityAmountView.getText().toString().trim();
        String bankName = bankNameView.getText().toString().trim();
        String branchName = branchNameView.getText().toString().trim();
        String depositDateStr = depositDateView.getText().toString().trim();
        String maturityDateStr = maturityDateView.getText().toString().trim();
        String status = statusView.getText().toString().trim();
        String remarks = remarksView.getText().toString().trim();

        // Input Validation
        if (accountNumber.isEmpty()) {
            accountNumberView.setError("Account Number is required");
            accountNumberView.requestFocus();
            return;
        }

        if (depositAmountStr.isEmpty()) {
            depositAmountView.setError("Deposit Amount is required");
            depositAmountView.requestFocus();
            return;
        }

        if (maturityAmountStr.isEmpty()) {
            maturityAmountView.setError("Maturity Amount is required");
            maturityAmountView.requestFocus();
            return;
        }

        if (bankName.isEmpty()) {
            bankNameView.setError("Bank Name is required");
            bankNameView.requestFocus();
            return;
        }

        if (branchName.isEmpty()) {
            branchNameView.setError("Branch Name is required");
            branchNameView.requestFocus();
            return;
        }

        if (depositDateStr.isEmpty()) {
            depositDateView.setError("Deposit Date is required");
            depositDateView.requestFocus();
            return;
        }

        if (maturityDateStr.isEmpty()) {
            maturityDateView.setError("Maturity Date is required");
            maturityDateView.requestFocus();
            return;
        }

        // Date Validation
        try {
            Date depositDate = dateFormat.parse(depositDateStr);
            Date maturityDate = dateFormat.parse(maturityDateStr);

            assert depositDate != null;
            if (depositDate.after(maturityDate)) {
                Toast.makeText(this, "Deposit Date cannot be after Maturity Date", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (ParseException e) {
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare data for update
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("accountNumber", accountNumber);
        updates.put("depositAmount", depositAmountStr);
        updates.put("maturityAmount", maturityAmountStr);
        updates.put("bankName", bankName);
        updates.put("branchName", branchName);
        updates.put("depositDate", depositDateStr);
        updates.put("maturityDate", maturityDateStr);
        updates.put("status", status);
        updates.put("remarks", remarks);

        // Update data in Firebase
        databaseReference.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(UpdateInvestmentActivity.this, "Investment details updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(UpdateInvestmentActivity.this, "Failed to update investment details", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(UpdateInvestmentActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("UpdateInvestmentActivity", "Error updating details", e);
        });
    }
}
