package com.gannahisab.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private DB db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = new DB(this);

        findViewById(R.id.btnCustomer).setOnClickListener(v -> showCustomerDialog());
        findViewById(R.id.btnCustomerHistory).setOnClickListener(v -> showCustomerHistoryDialog());
        findViewById(R.id.btnTrip).setOnClickListener(v -> showTripDialog());
        findViewById(R.id.btnTrolleyHistory).setOnClickListener(v -> showTrolleyHistoryDialog());
        findViewById(R.id.btnMillSummary).setOnClickListener(v -> showMillSummaryDialog());
        findViewById(R.id.btnDashboard).setOnClickListener(v -> showDashboardDialog());
        findViewById(R.id.btnHistory).setOnClickListener(v -> showHistoryDialog());
        findViewById(R.id.btnExpense).setOnClickListener(v -> showExpenseDialog());
        findViewById(R.id.btnPayment).setOnClickListener(v -> showPaymentDialog());
        findViewById(R.id.btnReceipt).setOnClickListener(v -> showReceiptDialog());
        findViewById(R.id.btnPurchase).setOnClickListener(v -> showPurchaseDialog());
        findViewById(R.id.btnExportCsv).setOnClickListener(v -> exportCsv());
    }

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private double parseD(EditText e) {
        String s = e.getText().toString().trim();
        if (s.isEmpty()) return 0;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(v * d);
    }

    private EditText newField(LinearLayout parent, String hint, boolean numeric) {
        EditText e = new EditText(this);
        e.setHint(hint);
        if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        e.setLayoutParams(lp);
        parent.addView(e);
        return e;
    }

    // ---------------------------------------------------------------
    // ADD LOAD / TRIP  ->  mill & driver are picked from saved lists,
    // every save creates a NEW row, nothing old is ever overwritten.
    // ---------------------------------------------------------------
    private void showTripDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        layout.setPadding(pad, pad, pad, pad);

        TextView millLabel = new TextView(this);
        millLabel.setText("Mill ka naam");
        layout.addView(millLabel);
        AutoCompleteTextView millView = new AutoCompleteTextView(this);
        millView.setThreshold(1);
        millView.setHint("Mill ka naam likhein ya list se chunein");
        List<String> mills = db.getMillNames();
        millView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, mills));
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mlp.bottomMargin = dp(10);
        millView.setLayoutParams(mlp);
        layout.addView(millView);

        TextView driverLabel = new TextView(this);
        driverLabel.setText("Driver ka naam");
        layout.addView(driverLabel);
        AutoCompleteTextView driverView = new AutoCompleteTextView(this);
        driverView.setThreshold(1);
        driverView.setHint("Driver ka naam likhein ya list se chunein");
        List<String> drivers = db.getDriverNames();
        driverView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, drivers));
        driverView.setLayoutParams(mlp);
        layout.addView(driverView);

        EditText phoneView = newField(layout, "Driver ka number", false);
        phoneView.setInputType(InputType.TYPE_CLASS_PHONE);

        // when an existing driver is picked, auto-fill their saved phone number
        driverView.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            String phone = db.getDriverPhone(selected);
            if (phone != null && !phone.isEmpty()) phoneView.setText(phone);
        });

        EditText truckView = newField(layout, "Trolley / Truck number (optional)", false);
        EditText maundView = newField(layout, "Kitna maund (wazan)", true);
        EditText driverCostView = newField(layout, "Driver ka kharcha (Rs)", true);
        EditText otherCostView = newField(layout, "Doosra kharcha (Rs)", true);
        EditText saleRateView = newField(layout, "Rate fi maund (agar maloom hai)", true);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);

        new AlertDialog.Builder(this)
                .setTitle("Naya Load / Trip")
                .setView(scroll)
                .setPositiveButton("Save", (dialog, which) -> {
                    String mill = millView.getText().toString().trim();
                    String driver = driverView.getText().toString().trim();
                    String phone = phoneView.getText().toString().trim();
                    String truck = truckView.getText().toString().trim();
                    double maund = parseD(maundView);

                    if (mill.isEmpty()) {
                        Toast.makeText(this, "Mill ka naam likhna zaroori hai", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (maund <= 0) {
                        Toast.makeText(this, "Maund (wazan) likhna zaroori hai", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double driverCost = parseD(driverCostView);
                    double otherCost = parseD(otherCostView);
                    double saleRate = parseD(saleRateView);
                    double saleTotal = saleRate > 0 ? saleRate * maund : 0;

                    // save mill & driver to the master lists so next time they just get picked
                    db.addMillIfNotExists(mill);
                    if (!driver.isEmpty()) db.addOrUpdateDriver(driver, phone);

                    db.insertTrip(today(), truck, driver, phone, maund, mill,
                            driverCost, otherCost, saleRate, saleTotal, "pending");

                    Toast.makeText(this, "Record save ho gaya", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------------------------------------------------------------
    // CUSTOMER (ganna seller) ADD ENTRY -> picked from saved customer
    // list, every save stacks a NEW row under that customer, nothing
    // old is ever overwritten.
    // ---------------------------------------------------------------
    private void showCustomerDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        layout.setPadding(pad, pad, pad, pad);

        TextView nameLabel = new TextView(this);
        nameLabel.setText("Customer ka naam");
        layout.addView(nameLabel);
        AutoCompleteTextView nameView = new AutoCompleteTextView(this);
        nameView.setThreshold(1);
        nameView.setHint("Naam likhein ya list se chunein");
        nameView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, db.getCustomerNames()));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        nameView.setLayoutParams(lp);
        layout.addView(nameView);

        EditText grossView = newField(layout, "Gross wazan", true);
        EditText emptyView = newField(layout, "Khali wazan", true);
        EditText bandhanView = newField(layout, "Bandhan", true);
        EditText rateView = newField(layout, "Rate fi man", true);
        EditText laborView = newField(layout, "Labor (Rs)", true);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);

        new AlertDialog.Builder(this)
                .setTitle("Customer Ganna Entry")
                .setView(scroll)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameView.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Customer ka naam likhna zaroori hai", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double gross = parseD(grossView);
                    if (gross <= 0) {
                        Toast.makeText(this, "Gross wazan likhna zaroori hai", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double empty = parseD(emptyView);
                    double bandhan = parseD(bandhanView);
                    double rate = parseD(rateView);
                    double labor = parseD(laborView);

                    double remaining = gross - empty;
                    double net = remaining - bandhan;
                    double total = net * rate;
                    double finalAmount = total - labor;

                    db.addCustomerIfNotExists(name);
                    db.insertCustomerEntry(today(), name, gross, empty, remaining, bandhan, net, rate,
                            total, labor, finalAmount);

                    Toast.makeText(this, "Entry save ho gayi", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------------------------------------------------------------
    // CUSTOMER SHEETS -> pick a customer, see their whole running
    // sheet with running totals of safi wazan and raqam.
    // ---------------------------------------------------------------
    private void showCustomerHistoryDialog() {
        List<String> names = db.getCustomerNames();
        if (names.isEmpty()) {
            Toast.makeText(this, "Abhi koi customer add nahi hua", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Customer Chunein")
                .setItems(names.toArray(new String[0]), (dialog, which) -> showCustomerSheet(names.get(which)))
                .setNegativeButton("Band Karein", null)
                .show();
    }

    private void showCustomerSheet(String name) {
        Cursor c = db.getEntriesForCustomer(name);
        List<String> rows = new ArrayList<>();
        double totalNet = 0, totalFinal = 0;
        while (c.moveToNext()) {
            String date = c.getString(0);
            double gross = c.getDouble(1);
            double empty = c.getDouble(2);
            double remaining = c.getDouble(3);
            double bandhan = c.getDouble(4);
            double net = c.getDouble(5);
            double rate = c.getDouble(6);
            double total = c.getDouble(7);
            double labor = c.getDouble(8);
            double finalAmount = c.getDouble(9);
            totalNet += net;
            totalFinal += finalAmount;
            rows.add(String.format(Locale.getDefault(),
                    "%s\nGross: %.2f   Khali: %.2f   Baqi: %.2f\nBandhan: %.2f   Safi: %.2f   Rate: %.2f\nKul: Rs %.2f   Labor: Rs %.2f   Raqam: Rs %.2f",
                    date, gross, empty, remaining, bandhan, net, rate, total, labor, finalAmount));
        }
        c.close();

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        TextView summary = new TextView(this);
        int pad = dp(16);
        summary.setPadding(pad, pad, pad, pad);
        summary.setText(String.format(Locale.getDefault(),
                "%s\nTotal Safi Wazan: %.2f   |   Total Raqam: Rs %.2f",
                name, totalNet, totalFinal));
        container.addView(summary);

        ListView listView = new ListView(this);
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows));
        container.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(500)));

        new AlertDialog.Builder(this)
                .setTitle(name + " ki Sheet")
                .setView(container)
                .setPositiveButton("Band Karein", null)
                .show();
    }

    // ---------------------------------------------------------------
    // TROLLEY SHEETS -> pick a trolley/truck number, see its whole
    // running sheet (every load it carried) with running totals.
    // ---------------------------------------------------------------
    private void showTrolleyHistoryDialog() {
        List<String> trucks = db.getTruckNumbers();
        if (trucks.isEmpty()) {
            Toast.makeText(this, "Abhi koi trolley record nahi hai", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Trolley Chunein")
                .setItems(trucks.toArray(new String[0]), (dialog, which) -> showTrolleySheet(trucks.get(which)))
                .setNegativeButton("Band Karein", null)
                .show();
    }

    private void showTrolleySheet(String truck) {
        Cursor c = db.getTripsForTruck(truck);
        List<String> rows = new ArrayList<>();
        double totalMaund = 0, totalCost = 0;
        while (c.moveToNext()) {
            String date = c.getString(0);
            String mill = c.getString(1);
            String driver = c.getString(2);
            String phone = c.getString(3);
            double maund = c.getDouble(5);
            double driverCost = c.getDouble(6);
            double otherCost = c.getDouble(7);
            totalMaund += maund;
            totalCost += driverCost + otherCost;
            rows.add(String.format(Locale.getDefault(),
                    "%s\nMill: %s\nDriver: %s (%s)\nWazan: %.2f   Kharcha: Rs %.2f",
                    date, mill, driver == null ? "" : driver, phone == null ? "" : phone,
                    maund, driverCost + otherCost));
        }
        c.close();

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        TextView summary = new TextView(this);
        int pad = dp(16);
        summary.setPadding(pad, pad, pad, pad);
        summary.setText(String.format(Locale.getDefault(),
                "Trolley: %s\nTotal Wazan: %.2f   |   Total Kharcha: Rs %.2f",
                truck, totalMaund, totalCost));
        container.addView(summary);

        ListView listView = new ListView(this);
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows));
        container.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(500)));

        new AlertDialog.Builder(this)
                .setTitle(truck + " ki Sheet")
                .setView(container)
                .setPositiveButton("Band Karein", null)
                .show();
    }

    // ---------------------------------------------------------------
    // MILL HISAB -> every mill, how many trolleys and how much wazan
    // went to it, and which trucks carried it.
    // ---------------------------------------------------------------
    private void showMillSummaryDialog() {
        List<String> mills = db.getMillNames();
        if (mills.isEmpty()) {
            Toast.makeText(this, "Abhi koi mill record nahi hai", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> rows = new ArrayList<>();
        for (String mill : mills) {
            double[] totals = db.getMillTotals(mill);
            List<String> trucks = db.getTrucksForMill(mill);
            rows.add(String.format(Locale.getDefault(),
                    "%s\nTrolleys: %d   |   Total Wazan: %.2f\nTrucks: %s",
                    mill, (int) totals[0], totals[1],
                    trucks.isEmpty() ? "-" : android.text.TextUtils.join(", ", trucks)));
        }

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        ListView listView = new ListView(this);
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows));
        container.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(500)));

        new AlertDialog.Builder(this)
                .setTitle("Mill Hisab")
                .setView(container)
                .setPositiveButton("Band Karein", null)
                .show();
    }

    // ---------------------------------------------------------------
    // DASHBOARD -> total paid out, kharcha, aur munafa, pichle
    // 6 mahine ka.
    // ---------------------------------------------------------------
    private void showDashboardDialog() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.MONTH, -6);
        String since = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

        double customerPaid = db.sumSince("customer_entries", "finalAmount", since);
        double tripCosts = db.sumTripCostsSince(since);
        double tripSales = db.sumSince("trips", "saleTotal", since);
        double otherExpenses = db.sumSince("expenses", "amount", since);
        double millReceipts = db.sumSince("receipts", "amount", since);

        double totalIncome = tripSales + millReceipts;
        double totalOutgoing = customerPaid + tripCosts + otherExpenses;
        double profit = totalIncome - totalOutgoing;

        String msg = String.format(Locale.getDefault(),
                "Pichle 6 Mahine ka Hisab\n\n" +
                        "Customers ko Diye Gaye: Rs %.2f\n" +
                        "Trolley Kharcha: Rs %.2f\n" +
                        "Doosre Kharche: Rs %.2f\n" +
                        "-----------------------------\n" +
                        "Kul Kharcha: Rs %.2f\n\n" +
                        "Mill Se Aamdani: Rs %.2f\n" +
                        "Trip Sale: Rs %.2f\n" +
                        "-----------------------------\n" +
                        "Kul Aamdani: Rs %.2f\n\n" +
                        "Munafa: Rs %.2f",
                customerPaid, tripCosts, otherExpenses, totalOutgoing,
        
