package com.saitechnology.investo.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

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
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.saitechnology.investo.R;
import com.saitechnology.investo.util.HeaderFooterPageEvent;
import com.saitechnology.investo.util.ProgressUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfGenerateActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private Button btnStartDate, btnEndDate;
    private int selectedYear, selectedMonth, selectedDay;
    private int selectedEndYear, selectedEndMonth, selectedEndDay;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_pdf);

        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        MobileAds.initialize(this, initializationStatus -> {});
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        assert user != null;
        mDatabase = FirebaseDatabase.getInstance().getReference().child("TransactionHistory").child(user.getUid());

        // Initialize views
        btnStartDate = findViewById(R.id.btnStartDate);
        btnEndDate = findViewById(R.id.btnEndDate);
        Spinner paymentStatusSpinner = findViewById(R.id.paymentStatusSpinner);
        Spinner paymentTypeSpinner = findViewById(R.id.paymentTypeSpinner);
        Button generatePdfButton = findViewById(R.id.btnGeneratePDF);

        // Populate payment status spinner
        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.payment_status_options, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        paymentStatusSpinner.setAdapter(statusAdapter);

        // Populate payment type spinner
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.payment_type_options, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        paymentTypeSpinner.setAdapter(typeAdapter);

        // Handle PDF generation button click
        generatePdfButton.setOnClickListener(v -> {
            if (selectedYear == 0 || selectedEndYear == 0) {
                Toast.makeText(this, "Please select start and end dates", Toast.LENGTH_SHORT).show();
                return;
            }
            myPermissions();
        });

        // Set onClick listeners for date selection buttons
        btnStartDate.setOnClickListener(this::selectStartDate);
        btnEndDate.setOnClickListener(this::selectEndDate);
    }

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permission -> {
                boolean allGranted = true;

                for (boolean isGranted : permission.values()) {
                    if (!isGranted) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted) {
                    // If all permissions are granted, call the method to generate the PDF
                    generatePdf();
                }
            });

    private void myPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String [] permissions = new String[] {
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_AUDIO,
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                    android.Manifest.permission.POST_NOTIFICATIONS
            };

            List<String> permissionsToRequest = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }

            if (permissionsToRequest.isEmpty()) {
                generatePdf();
            } else {
                String[] permissionsArray = permissionsToRequest.toArray(new String[0]);
                boolean shouldShowRational = false;

                for (String permission : permissionsArray) {
                    if (shouldShowRequestPermissionRationale(permission)) {
                        shouldShowRational = true;
                        break;
                    }
                }

                if (shouldShowRational) {
                    new AlertDialog.Builder(this)
                            .setMessage("Please allow all permissions")
                            .setCancelable(false)
                            .setPositiveButton("YES", (dialogInterface, i) -> requestPermissionLauncher.launch(permissionsArray))

                            .setNegativeButton("NO", (dialogInterface, i) -> dialogInterface.dismiss())
                            .show();
                } else {
                    requestPermissionLauncher.launch(permissionsArray);
                }
            }
        } else {
            String [] permissions = new String[] {
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };

            List<String> permissionsToRequest = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }

            if (permissionsToRequest.isEmpty()) {
                generatePdf();
            } else {
                String[] permissionsArray = permissionsToRequest.toArray(new String[0]);
                boolean shouldShowRational = false;

                for (String permission : permissionsArray) {
                    if (shouldShowRequestPermissionRationale(permission)) {
                        shouldShowRational = true;
                        break;
                    }
                }

                if (shouldShowRational) {
                    new AlertDialog.Builder(this)
                            .setMessage("Please allow all permissions")
                            .setCancelable(false)
                            .setPositiveButton("YES", (dialogInterface, i) -> requestPermissionLauncher.launch(permissionsArray))

                            .setNegativeButton("NO", (dialogInterface, i) -> dialogInterface.dismiss())
                            .show();
                } else {
                    requestPermissionLauncher.launch(permissionsArray);
                }
            }
        }
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


    private void generatePdf() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date startDate = null;
        Date endDate = null;
        Date endDateFilePath = null;
        String desiredPaymentStatus;
        String desiredPaymentType;

        // Get selected dates
        try {
            int startYear = selectedYear;
            int startMonth = selectedMonth;
            int startDay = selectedDay;
            startDate = dateFormat.parse(String.format(Locale.getDefault(), "%04d-%02d-%02d", startYear, startMonth + 1, startDay));

            // Adjust the end date to the end of the selected day
            Calendar endCalendar = Calendar.getInstance();
            endCalendar.set(selectedEndYear, selectedEndMonth, selectedEndDay, 23, 59, 59);
            endCalendar.add(Calendar.DAY_OF_MONTH, 1); // Add one calendar day to include transactions up to the end of the selected day
            endDate = endCalendar.getTime();

            // Subtract one day from end date only for file path creation
            endCalendar.add(Calendar.DAY_OF_MONTH, -1);
            endDateFilePath = endCalendar.getTime();
        } catch (ParseException e) {
            e.getMessage();
            return; // Exit the method if parsing dates fails
        }

        // Get desired payment status and type
        Spinner paymentStatusSpinner = findViewById(R.id.paymentStatusSpinner);
        Spinner paymentTypeSpinner = findViewById(R.id.paymentTypeSpinner);

        desiredPaymentStatus = paymentStatusSpinner.getSelectedItem().toString();
        desiredPaymentType = paymentTypeSpinner.getSelectedItem().toString();

        Document document = new Document(PageSize.A4.rotate());

        try {
            String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    + File.separator + "Acc_Statement "+ formatDate(startDate) + " To " + formatDate(endDateFilePath) +".pdf";

            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));

            // Define fonts for header and footer
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font footerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);

            // Set the page event
            HeaderFooterPageEvent event = new HeaderFooterPageEvent(headerFont, footerFont);
            writer.setPageEvent(event);

            document.open();

            PdfPTable table = new PdfPTable(7); // 7 columns for each transaction detail
            table.setWidthPercentage(100); // Make table fill the width of the page

            // Add table headers
            addTableHeader(table);

            Date finalStartDate = startDate;
            Date finalEndDate = endDate;
            mDatabase.orderByKey().startAt(formatDate(startDate)).endAt(formatDate(endDate)).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            String transactionTime = snapshot.child("transactionTime").getValue(String.class);
                            if (isWithinDateRange(transactionTime, finalStartDate, finalEndDate)) {
                                String cashEntry = snapshot.child("cashEntry").getValue(String.class);
                                String itemValues = snapshot.child("itemValues").getValue(String.class);
                                String paymentStatus = snapshot.child("paymentStatus").getValue(String.class);
                                String paymentType = snapshot.child("paymentType").getValue(String.class);
                                String totalValue = snapshot.child("totalValue").getValue(String.class);
                                String transactionId = snapshot.child("transactionId").getValue(String.class);

                                // Check if the transaction meets the desired conditions
                                boolean meetsConditions = isMeetsConditions(paymentType, paymentStatus);

                                if (meetsConditions) {
                                    // Add transaction details to the table
                                    addRow(table, transactionTime, transactionId, cashEntry, itemValues, paymentStatus, paymentType, totalValue);
                                }
                            }
                        } catch (NullPointerException e) {
                            e.getMessage();
                        }
                    }

                    // Add the table to the document
                    try {
                        document.add(table);
                    } catch (DocumentException e) {
                        throw new RuntimeException(e);
                    }
                    document.close();
                    Toast.makeText(PdfGenerateActivity.this, "PDF generated successfully", Toast.LENGTH_SHORT).show();
                    createNotification(filePath);
                }

                private boolean isMeetsConditions(String paymentType, String paymentStatus) {
                    boolean meetsConditions;
                    if (desiredPaymentStatus.equals("All Transactions") && desiredPaymentType.equals("All Transactions")) {
                        meetsConditions = true; // Include all transactions
                    } else if (desiredPaymentStatus.equals("All Transactions")) {
                        meetsConditions = desiredPaymentType.equals(paymentType); // Filter by payment type only
                    } else if (desiredPaymentType.equals("All Transactions")) {
                        meetsConditions = desiredPaymentStatus.equals(paymentStatus); // Filter by payment status only
                    } else {
                        meetsConditions = desiredPaymentStatus.equals(paymentStatus) && desiredPaymentType.equals(paymentType); // Filter by both payment status and type
                    }
                    return meetsConditions;
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(PdfGenerateActivity.this, "Failed to retrieve data from Firebase", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (FileNotFoundException | DocumentException e) {
            e.getMessage();
            Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(date);
    }

    private boolean isWithinDateRange(String transactionTime, Date startDate, Date endDate) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = dateFormat.parse(transactionTime);
            assert date != null;

            // Adjust end date to the end of the day
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(endDate);
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            Date adjustedEndDate = calendar.getTime();

            return date.after(startDate) && date.before(adjustedEndDate);
        } catch (ParseException e) {
            e.getMessage();
            return false;
        }
    }


    private void addTableHeader(PdfPTable table) {
        String[] headers = {"Date", "Transaction ID", "Cash Entry", "Item Values", "Payment Status", "Payment Type", "Total Value"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell();
            cell.setPhrase(new Phrase(header));
            table.addCell(cell);
        }
    }

    private void addRow(PdfPTable table, String... rowData) {
        for (String data : rowData) {
            table.addCell(data);
        }
    }

    private void createNotification(String filePath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", new File(filePath));
        intent.setDataAndType(uri, "application/pdf");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "channel_id";
            String channelName = "Channel Name";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel_id")
                .setContentTitle("mPOS Account Statement")
                .setContentText("Click to open the Account Statement")
                .setSmallIcon(R.drawable.icon_save)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(123, builder.build());
    }
}
