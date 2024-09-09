package com.saitechnology.investo.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class ViewInvestmentActivity extends AppCompatActivity {

    private LinearLayout recordList;
    private List<DataSnapshot> allRecords = new ArrayList<>();
    @SuppressLint("SimpleDateFormat")
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private CheckBox checkBoxActive, checkBoxMatured, checkBoxClosed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_investment);

        recordList = findViewById(R.id.recordList);

        // Initialize the checkboxes
        checkBoxActive = findViewById(R.id.checkBoxActive);
        checkBoxMatured = findViewById(R.id.checkBoxMatured);
        checkBoxClosed = findViewById(R.id.checkBoxClosed);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("InvestmentWarehouse").child(user.getUid());

        // Fetch investment data from Firebase
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                recordList.removeAllViews(); // Clear the list to avoid duplicates
                allRecords.clear();

                // Log retrieved data
                Log.d("ViewInvestmentActivity", "Fetched records from Firebase: " + dataSnapshot.getChildrenCount());

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    allRecords.add(snapshot);
                }

                // Initially display all records
                filterAndDisplayRecords();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ViewInvestmentActivity", "Database error: " + databaseError.getMessage());
                Toast.makeText(ViewInvestmentActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Set up listeners for checkboxes to filter records
        checkBoxActive.setOnCheckedChangeListener((buttonView, isChecked) -> filterAndDisplayRecords());
        checkBoxMatured.setOnCheckedChangeListener((buttonView, isChecked) -> filterAndDisplayRecords());
        checkBoxClosed.setOnCheckedChangeListener((buttonView, isChecked) -> filterAndDisplayRecords());
    }

    // Method to filter and display records based on selected checkboxes
    @SuppressLint("SetTextI18n")
    private void filterAndDisplayRecords() {
        recordList.removeAllViews(); // Clear current displayed records

        // Get selected statuses
        boolean showActive = checkBoxActive.isChecked();
        boolean showMatured = checkBoxMatured.isChecked();
        boolean showClosed = checkBoxClosed.isChecked();

        List<DataSnapshot> filteredRecords = new ArrayList<>();

        // Filter records based on status
        for (DataSnapshot snapshot : allRecords) {
            String status = snapshot.child("status").getValue(String.class);

            if (status != null) {
                if ((showActive && status.equalsIgnoreCase("Active")) ||
                        (showMatured && status.equalsIgnoreCase("Matured")) ||
                        (showClosed && status.equalsIgnoreCase("Closed"))) {
                    filteredRecords.add(snapshot);
                }
            }
        }

        if (filteredRecords.isEmpty()) {
            // Show message if no records found
            Toast.makeText(ViewInvestmentActivity.this, "No records found for the selected statuses", Toast.LENGTH_SHORT).show();
        }

        // Sort records by maturity date
        Collections.sort(filteredRecords, new Comparator<DataSnapshot>() {
            @Override
            public int compare(DataSnapshot o1, DataSnapshot o2) {
                try {
                    String date1Str = o1.child("maturityDate").getValue(String.class);
                    String date2Str = o2.child("maturityDate").getValue(String.class);

                    if (date1Str == null) return 1;
                    if (date2Str == null) return -1;

                    Date date1 = dateFormat.parse(date1Str);
                    Date date2 = dateFormat.parse(date2Str);

                    assert date1 != null;
                    return date1.compareTo(date2); // Ascending order
                } catch (ParseException e) {
                    e.printStackTrace();
                    return 0;
                }
            }
        });

        // Display the filtered and sorted records
        for (DataSnapshot snapshot : filteredRecords) {
            String accountNumber = snapshot.child("accountNumber").getValue(String.class);
            String maturityDate = snapshot.child("maturityDate").getValue(String.class);

            TextView recordView = new TextView(ViewInvestmentActivity.this);
            recordView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            recordView.setText("Account Number: " + (accountNumber != null ? accountNumber : "N/A") +
                    "\nMaturity Date: " + (maturityDate != null ? maturityDate : "N/A"));
            recordView.setPadding(16, 16, 16, 16);
            recordView.setTextSize(18f);
            recordView.setTextColor(getResources().getColor(android.R.color.black));
            recordView.setBackgroundResource(R.drawable.edittext_background);
            recordView.setOnClickListener(v -> {
                // Create an intent to view full details
                Intent intent = new Intent(ViewInvestmentActivity.this, InvestmentDetailActivity.class);
                intent.putExtra("investmentId", snapshot.getKey());
                startActivity(intent);
            });

            // Add the TextView to the LinearLayout
            recordList.addView(recordView);
        }
    }
}