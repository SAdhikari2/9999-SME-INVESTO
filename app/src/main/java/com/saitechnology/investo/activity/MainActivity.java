package com.saitechnology.investo.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.saitechnology.investo.R;
import com.saitechnology.investo.entity.TransactionHistory;
import com.saitechnology.investo.util.ProgressUtils;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener {
    private TextView resultTv, solutionTv;
    private DrawerLayout drawer;
    private ProgressBar progressBar;
    private boolean isProgressVisible = false;
    private Context jsContext;
    private Scriptable jsScope;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        progressBar = findViewById(R.id.idPBLoading);
        resultTv = findViewById(R.id.result_tv);
        solutionTv = findViewById(R.id.solution_tv);
        solutionTv.setMovementMethod(new ScrollingMovementMethod());

        initJavaScript();

        assignId(R.id.button_c);
        assignId(R.id.button_open_bracket);
        assignId(R.id.button_close_bracket);
        assignId(R.id.button_divide);
        assignId(R.id.button_multiply);
        assignId(R.id.button_plus);
        assignId(R.id.button_minus);
        assignId(R.id.button_equals);
        assignId(R.id.button_0);
        assignId(R.id.button_1);
        assignId(R.id.button_2);
        assignId(R.id.button_3);
        assignId(R.id.button_4);
        assignId(R.id.button_5);
        assignId(R.id.button_6);
        assignId(R.id.button_7);
        assignId(R.id.button_8);
        assignId(R.id.button_9);
        assignId(R.id.button_ac);
        assignId(R.id.button_dot);
        assignId(R.id.cash_in);
        assignId(R.id.cash_out);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void initJavaScript() {
        jsContext = Context.enter();
        jsContext.setOptimizationLevel(-1);
        jsScope = jsContext.initStandardObjects();
    }

    void assignId(int id) {
        MaterialButton btn = findViewById(id);
        btn.setOnClickListener(this);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onClick(View view) {
        MaterialButton button = (MaterialButton) view;
        String buttonText = button.getText().toString();

        int buttonId = button.getId();

        String dataToCalculate = solutionTv.getText().toString();

        if (buttonText.equals("C")) {
            solutionTv.setText("");
            resultTv.setText("0");
            return;
        }
        if (buttonText.equals("%")) {
            // Ensure there is some data to calculate
            if (!dataToCalculate.isEmpty() && !dataToCalculate.equals("0")) {
                // Remove any whitespace
                dataToCalculate = dataToCalculate.trim();
                String expression = dataToCalculate + "%";

                // Find the index of the last %
                int lastIndexPercentage = expression.lastIndexOf('%');
                if (lastIndexPercentage != -1) {
                    // Find the index of the last operator before %
                    char lastOperator = ' ';
                    for (int i = lastIndexPercentage - 1; i >= 0; i--) {
                        char c = dataToCalculate.charAt(i);
                        if (c == '+' || c == '-' || c == '*' || c == '/') {
                            lastOperator = c;
                            break;
                        }
                    }

                    // Ensure an operator before %
                    if (lastOperator != ' ') {
                        // Extract the substring from the start of the expression to the last operator before %
                        String expressionBeforeOperator = dataToCalculate.substring(0, dataToCalculate.lastIndexOf(lastOperator));

                        // Extract the substring starting from the operator to the end
                        String numberString = dataToCalculate.substring(dataToCalculate.lastIndexOf(lastOperator) + 1, lastIndexPercentage);

                        // Get the number before the operator
                        String numberBeforeOperator = getResult(expressionBeforeOperator);

                        try {
                            // Parse the numbers
                            double number = Double.parseDouble(numberString);
                            double numberBefore = Double.parseDouble(numberBeforeOperator);

                            // Calculate the percentage
                            double percentage = number * 0.01 * numberBefore;

                            // Subtract the percentage from the original number
                            double result = 0;
                            if (lastOperator == '+'){
                                result = numberBefore + percentage;
                            } else if (lastOperator == '-') {
                                result = numberBefore - percentage;
                            }

                            // Display the result
                            solutionTv.setText(expression);
                            resultTv.setText(String.valueOf(result));
                        } catch (NumberFormatException e) {
                            // Handle parsing errors
                            e.getMessage();
                        }
                    }
                }
            }
            return;
        }

        if (buttonId == R.id.button_c) {
            // Check if dataToCalculate is empty or equals "0" before deleting the digit
            if (!dataToCalculate.isEmpty() && !dataToCalculate.equals("0")) {
                dataToCalculate = dataToCalculate.substring(0, dataToCalculate.length() - 1);
            }
        } else {
            // Append the button text to dataToCalculate
            dataToCalculate = buttonText.equals("Cash In +") || buttonText.equals("Cash Out -") ? dataToCalculate : dataToCalculate + buttonText;
        }

        solutionTv.setText(dataToCalculate);

        String finalResult = getResult(dataToCalculate);

        if (!finalResult.isEmpty() && !finalResult.equals("Err")) {
            resultTv.setText(finalResult);
        }

        // Proceed to payment if Cash In or Cash Out button is clicked
        if (buttonText.equals("Cash In +") || buttonText.equals("Cash Out -")) {
            isProgressVisible = ProgressUtils.toggleProgressVisibility(progressBar, isProgressVisible);
            proceedToPayment(dataToCalculate, buttonText);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Exit JavaScript context when activity is destroyed
        Context.exit();
    }

    private String getResult(String data) {
        try {
            if (data.isEmpty()) {
                return "0";
            }
            double finalResult = (double) jsContext.evaluateString(jsScope, data, "Javascript", 1, null);
            return String.valueOf(finalResult);
        } catch (Exception e) {
            e.getMessage();
            return "Err";
        }
    }

    void proceedToPayment(String dataToCalculate, String buttonText) {
        Intent intent = new Intent(MainActivity.this, PaymentActivity.class);

        TransactionHistory transactionHistory = TransactionHistory.builder()
                .transactionId(UUID.randomUUID().toString())
                .itemValues(dataToCalculate)
                .totalValue(buttonText.equals("Cash Out -") ? "-".concat(resultTv.getText().toString()) : resultTv.getText().toString())
                .cashEntry(buttonText)
                .paymentType("")
                .paymentStatus("")
                .transactionTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()))
                .build();

        intent.putExtra("transactionHistoryKey", transactionHistory);
        startActivity(intent);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here
        switch (item.getItemId()) {
            case R.id.nav_account_statement:
                startActivity(new Intent(MainActivity.this, PdfGenerateActivity.class));
                break;
            case R.id.nav_cash_flow_analysis:
                startActivity(new Intent(MainActivity.this, CashFlowAnalysisActivity.class));
                break;
            case R.id.nav_logout:
                startActivity(new Intent(MainActivity.this, LogoutActivity.class));
                finish();
                break;
            case R.id.nav_business_details:
                startActivity(new Intent(MainActivity.this, BusinessDetailsActivity.class));
                break;
            case R.id.nav_contact_us:
                startActivity(new Intent(MainActivity.this, ContactUsActivity.class));
                break;
            case R.id.nav_share:
                startActivity(new Intent(MainActivity.this, ShareActivity.class));
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
