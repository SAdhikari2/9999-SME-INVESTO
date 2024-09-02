package com.saitechnology.investo.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
    @SuppressLint("SimpleDateFormat")
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); // Assuming date is in "yyyy-MM-dd" format

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_investment);

        recordList = findViewById(R.id.recordList);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Fetch transaction data from Firebase
        assert user != null;
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("InvestmentWarehouse").child(user.getUid());

        // Retrieve and display data
        databaseReference.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                recordList.removeAllViews(); // Clear the list to avoid duplicates

                List<DataSnapshot> records = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    records.add(snapshot);
                }

                // Sort records by maturity date
                Collections.sort(records, new Comparator<DataSnapshot>() {
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

                // Display sorted records
                for (DataSnapshot snapshot : records) {
                    String accountNumber = snapshot.child("accountNumber").getValue(String.class);
                    String maturityDate = snapshot.child("maturityDate").getValue(String.class);

                    // Creating a new TextView for each record
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
                        // Create an intent to view the full details
                        Intent intent = new Intent(ViewInvestmentActivity.this, InvestmentDetailActivity.class);
                        intent.putExtra("investmentId", snapshot.getKey());
                        startActivity(intent);
                    });

                    // Add the TextView to the LinearLayout
                    recordList.addView(recordView);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle possible errors
                Log.e("ViewInvestmentActivity", "Database error: " + databaseError.getMessage());
                Toast.makeText(ViewInvestmentActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
