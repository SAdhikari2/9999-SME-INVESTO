package com.saitechnology.investo.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
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
import com.saitechnology.investo.R;
import com.saitechnology.investo.util.ProgressUtils;

import java.util.Calendar;
import java.util.Locale;

public class CashFlowAnalysisActivity extends AppCompatActivity {

    private Button btnStartDate, btnEndDate;
    private int selectedYear, selectedMonth, selectedDay;
    private int selectedEndYear, selectedEndMonth, selectedEndDay;
    private ProgressBar progressBar;
    boolean isProgressVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cash_flow_filter);

        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        progressBar = findViewById(R.id.idPBLoading);

        MobileAds.initialize(this, initializationStatus -> {});
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Initialize views
        btnStartDate = findViewById(R.id.btnStartDate);
        btnEndDate = findViewById(R.id.btnEndDate);

        Button btnGeneratePDF = findViewById(R.id.btnViewPLStatement);
        btnGeneratePDF.setOnClickListener(v -> {
            if (selectedYear == 0 || selectedEndYear == 0) {
                Toast.makeText(this, "Please select start and end dates", Toast.LENGTH_SHORT).show();
                return;
            }
            isProgressVisible = ProgressUtils.toggleProgressVisibility(progressBar, isProgressVisible);
            // Create Intent to start PLStatementActivity
            Intent intent = new Intent(CashFlowAnalysisActivity.this, PLStatementActivity.class);

            // Add start and end dates as extras to the intent
            intent.putExtra("START_DATE", formatDate(selectedYear, selectedMonth, selectedDay));
            intent.putExtra("END_DATE", formatDate(selectedEndYear, selectedEndMonth, selectedEndDay));
            startActivity(intent);
        });

        btnStartDate.setOnClickListener(this::selectStartDate);
        btnEndDate.setOnClickListener(this::selectEndDate);
    }

    private String formatDate(int year, int month, int day) {
        return String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day);
    }


    public void selectStartDate(View view) {
        final Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (datePicker, year, month, day) -> {
                    if (selectedEndYear != 0 && (year > selectedEndYear || (year == selectedEndYear && month > selectedEndMonth) || (year == selectedEndYear && month == selectedEndMonth && day > selectedEndDay))) {
                        // If the selected start date is after the selected end date, show a toast and return
                        Toast.makeText(this, "Start date cannot be after end date", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    selectedYear = year;
                    selectedMonth = month;
                    selectedDay = day;
                    btnStartDate.setText(getString(R.string.selected_date_format, year, month + 1, day));
                }, currentYear, currentMonth, currentDay);

        // Set the maximum date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        // If an end date is selected, set the maximum date for the start date picker to the selected end date
        if (selectedEndYear != 0) {
            calendar.set(selectedEndYear, selectedEndMonth, selectedEndDay);
            datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());
        }

        datePickerDialog.show();
    }



    public void selectEndDate(View view) {
        final Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (datePicker, year, month, day) -> {
                    if (year < selectedYear || (year == selectedYear && month < selectedMonth) || (year == selectedYear && month == selectedMonth && day < selectedDay)) {
                        // If the selected end date is less than the selected start date, show a toast and return
                        Toast.makeText(this, "End date cannot be less than start date", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    selectedEndYear = year;
                    selectedEndMonth = month;
                    selectedEndDay = day;
                    btnEndDate.setText(getString(R.string.selected_date_format, year, month + 1, day));
                }, currentYear, currentMonth, currentDay);

        // Set the minimum date to the selected start date
        calendar.set(selectedYear, selectedMonth, selectedDay);
        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());

        // Set the maximum date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }


}
