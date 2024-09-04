package com.saitechnology.investo.activity;

import android.Manifest;
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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        MobileAds.initialize(this, initializationStatus -> {
        });
        AdView adView1 = findViewById(R.id.adView1);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView1.loadAd(adRequest);

        assignClickListener(R.id.button_add_investment);
        assignClickListener(R.id.button_view_investment);
        assignClickListener(R.id.button_export_pdf);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        mDatabase = FirebaseDatabase.getInstance().getReference().child("InvestmentWarehouse").child(user.getUid());
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

        switch (buttonText) {
            case "Add Investment":
                startActivity(new Intent(MainActivity.this, AddInvestmentActivity.class));
                break;
            case "View Investment":
                startActivity(new Intent(MainActivity.this, ViewInvestmentActivity.class));
                break;
            case "Export Investment Pdf":
                requestPermissionsAndExportPdf();
                break;
        }
    }

    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = true;

                for (boolean isGranted : permissions.values()) {
                    if (!isGranted) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted) {
                    generateInvestmentPdf();
                } else {
                    Toast.makeText(this, "Please allow all permissions", Toast.LENGTH_SHORT).show();
                }
            });

    private void requestPermissionsAndExportPdf() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.POST_NOTIFICATIONS
            };

        } else {
            permissions = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };

        }
        requestPermissionsIfNeeded(permissions);
    }

    private void requestPermissionsIfNeeded(String[] permissions) {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (permissionsToRequest.isEmpty()) {
            generateInvestmentPdf();
        } else {
            requestPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }

    private void generateInvestmentPdf() {
        Toast.makeText(this, "Generating PDF", Toast.LENGTH_SHORT).show();  // Debugging

        Document document = new Document(PageSize.A4.rotate());

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date currentDate = new Date();
            String formattedDate = dateFormat.format(currentDate);

            String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    + File.separator + "Investment_Statement_" + formattedDate + ".pdf";

            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));

            // Retrieve the current user's UID
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            assert user != null;
            String userPassword = Objects.requireNonNull(user.getEmail()).split("@")[0];

            // Set password protection on the PDF
            writer.setEncryption(userPassword.getBytes(), userPassword.getBytes(),
                    PdfWriter.ALLOW_PRINTING, PdfWriter.ENCRYPTION_AES_128);

            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font footerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);

            List<String> tableHeaders = Arrays.asList(
                    "Account Number", "Bank Name", "Branch Name", "Deposit Amount",
                    "Maturity Amount", "Deposit Date", "Maturity Date", "Status", "Remarks"
            );

            HeaderFooterPageEvent event = new HeaderFooterPageEvent(headerFont, footerFont);
            writer.setPageEvent(event);

            document.open();

            PdfPTable table = new PdfPTable(tableHeaders.size());
            table.setWidthPercentage(100);

            // The first header row is added manually for the first page
            addTableHeader(table);

            mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            String accountNumber = snapshot.child("accountNumber").getValue(String.class);
                            String bankName = snapshot.child("bankName").getValue(String.class);
                            String branchName = snapshot.child("branchName").getValue(String.class);
                            String depositAmount = snapshot.child("depositAmount").getValue(String.class);
                            String maturityAmount = snapshot.child("maturityAmount").getValue(String.class);
                            String depositDate = snapshot.child("depositDate").getValue(String.class);
                            String maturityDate = snapshot.child("maturityDate").getValue(String.class);
                            String status = snapshot.child("status").getValue(String.class);
                            String remarks = snapshot.child("remarks").getValue(String.class);

                            // Add investment details to the table
                            addRow(table, accountNumber, bankName, branchName, depositAmount,
                                    maturityAmount, depositDate, maturityDate, status, remarks);
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }

                    try {
                        document.add(table);
                        document.close();
                        createNotification(filePath);
                    } catch (DocumentException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(MainActivity.this, "Failed to retrieve data from Firebase", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (FileNotFoundException | DocumentException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void addTableHeader(PdfPTable table) {
        String[] headers = {"Account Number", "Bank Name", "Branch Name", "Deposit Amount", "Maturity Amount", "Deposit Date", "Maturity Date", "Status", "Remarks"};
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

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Investment Statement")
                .setContentText("PDF generated successfully")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("default", "Investment Notification", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1, builder.build());
    }
}
