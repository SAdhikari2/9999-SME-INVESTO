package com.saitechnology.investo.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.saitechnology.investo.R;
import com.saitechnology.investo.entity.InvestmentWarehouse;
import com.saitechnology.investo.util.ProfileImageUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddInvestmentActivity extends AppCompatActivity {
    private Button btnDepositDate, btnMaturityDate;
    private EditText accountNumberEditText, bankNameEditText, branchNameEditText, depositAmountEditText, maturityAmountEditText, remarksEditText;
    private DatabaseReference databaseReference;
    private int selectedYear, selectedMonth, selectedDay;
    private int selectedEndYear, selectedEndMonth, selectedEndDay;
    private Spinner statusSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_investment);

        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        // Initialize views
        accountNumberEditText = findViewById(R.id.accountIdEditText);
        bankNameEditText = findViewById(R.id.bankNameEditText);
        branchNameEditText = findViewById(R.id.branchNameEditText);
        remarksEditText = findViewById(R.id.specialNoteEditText);
        depositAmountEditText = findViewById(R.id.depositAmountEditText);
        maturityAmountEditText = findViewById(R.id.maturityAmountEditText);
        statusSpinner = findViewById(R.id.statusSpinner);
        btnDepositDate = findViewById(R.id.depositDateBtn);
        btnMaturityDate = findViewById(R.id.maturityDateBtn);

        // Setup the spinner with status options
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.status_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);

        ImageView userProfileIcon = findViewById(R.id.userProfileIcon);
        ProfileImageUtil.loadProfileImage(this, userProfileIcon);

        Button addInvestmentButton = findViewById(R.id.addInvestment);

        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // User is not logged in
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("InvestmentWarehouse").child(currentUser.getUid());

        btnDepositDate.setOnClickListener(this::depositDate);
        btnMaturityDate.setOnClickListener(this::maturityDate);
        // Set click listener for the update profile button
        addInvestmentButton.setOnClickListener(v -> addInvestment());
        // Make the profile icon clickable
        ProfileImageUtil.setupProfileIconClick(this, userProfileIcon);
    }

    private void addInvestment() {
        Intent intentMain = new Intent(AddInvestmentActivity.this, MainActivity.class);

        // Get All the text values
        String accountId = accountNumberEditText.getText().toString().trim();
        String bankName = bankNameEditText.getText().toString().trim();
        String branchName = branchNameEditText.getText().toString().trim();
        String depositDate = formatDate(selectedYear, selectedMonth, selectedDay);
        String maturityDate = formatDate(selectedEndYear, selectedEndMonth, selectedEndDay);
        String specialNote = remarksEditText.getText().toString().trim();
        String depositAmount = depositAmountEditText.getText().toString().trim();
        String maturityAmount = maturityAmountEditText.getText().toString().trim();
        String status = statusSpinner.getSelectedItem().toString();

        InvestmentWarehouse investmentWarehouse =
                InvestmentWarehouse.builder()
                        .accountNumber(accountId)
                        .bankName(bankName)
                        .branchName(branchName)
                        .depositDate(depositDate)
                        .maturityDate(maturityDate)
                        .depositAmount(depositAmount)
                        .maturityAmount(maturityAmount)
                        .status(status)
                        .remarks(specialNote)
                        .transactionTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()))
                        .build();

        databaseReference.child(investmentWarehouse.getTransactionTime()).setValue(investmentWarehouse)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(AddInvestmentActivity.this, "Successfully Updated", Toast.LENGTH_SHORT).show();
                        startActivity(intentMain);
                        finish();
                    } else {
                        Toast.makeText(AddInvestmentActivity.this, "Failed to update transaction data", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddInvestmentActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String formatDate(int year, int month, int day) {
        return String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day);
    }

    public void depositDate(View view) {
        final Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (datePicker, year, month, day) -> {
                    selectedYear = year;
                    selectedMonth = month;
                    selectedDay = day;

                    // Ensure that the selected start date is not after the end date
                    if (selectedEndYear != 0 && (year > selectedEndYear || (year == selectedEndYear && month > selectedEndMonth) || (year == selectedEndYear && month == selectedEndMonth && day > selectedEndDay))) {
                        Toast.makeText(this, "Deposit date cannot be after maturity date", Toast.LENGTH_SHORT).show();
                    } else {
                        btnDepositDate.setText(getString(R.string.selected_date_format, year, month + 1, day));
                    }
                }, currentYear, currentMonth, currentDay);

        // Set the maximum date to today for deposit date
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    public void maturityDate(View view) {
        final Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (datePicker, year, month, day) -> {
                    selectedEndYear = year;
                    selectedEndMonth = month;
                    selectedEndDay = day;

                    // Ensure that the maturity date is not before the deposit date
                    if (year < selectedYear || (year == selectedYear && month < selectedMonth) || (year == selectedYear && month == selectedMonth && day < selectedDay)) {
                        Toast.makeText(this, "Maturity date cannot be before deposit date", Toast.LENGTH_SHORT).show();
                    } else {
                        btnMaturityDate.setText(getString(R.string.selected_date_format, year, month + 1, day));
                    }
                }, currentYear, currentMonth, currentDay);

        // Set the minimum date to the selected deposit date
        calendar.set(selectedYear, selectedMonth, selectedDay);
        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
        datePickerDialog.show();
    }
}
