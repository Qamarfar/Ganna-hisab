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

        findViewById(R.id.btnTrip).setOnClickListener(v -> showTripDialog());
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
    // FULL HISTORY  -> every trip ever saved, newest first, forever
    // (until you delete the app data - nothing auto-deletes).
    // ---------------------------------------------------------------
    private void showHistoryDialog() {
        Cursor c = db.getAllTrips();
        List<String> rows = new ArrayList<>();
        double totalMaund = 0, totalCost = 0;

        while (c.moveToNext()) {
            String date = c.getString(0);
            String mill = c.getString(1);
            String driver = c.getString(2);
            String phone = c.getString(3);
            String truck = c.getString(4);
            double maund = c.getDouble(5);
            double driverCost = c.getDouble(6);
            double otherCost = c.getDouble(7);

            totalMaund += maund;
            totalCost += driverCost + otherCost;

            StringBuilder line = new StringBuilder();
            line.append(date).append("  |  Mill: ").append(mill);
            if (driver != null && !driver.isEmpty()) {
                line.append("\nDriver: ").append(driver);
                if (phone != null && !phone.isEmpty()) line.append(" (").append(phone).append(")");
            }
            if (truck != null && !truck.isEmpty()) line.append("\nTruck: ").append(truck);
            line.append(String.format(Locale.getDefault(), "\nManu: %.2f   Kharcha: Rs %.2f",
                    maund, (driverCost + otherCost)));
            rows.add(line.toString());
        }
        c.close();

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        TextView summary = new TextView(this);
        int pad = dp(16);
        summary.setPadding(pad, pad, pad, pad);
        summary.setText(String.format(Locale.getDefault(),
                "Total Loads: %d   |   Total Maund: %.2f   |   Total Kharcha: Rs %.2f",
                rows.size(), totalMaund, totalCost));
        container.addView(summary);

        ListView listView = new ListView(this);
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows));
        container.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(500)));

        new AlertDialog.Builder(this)
                .setTitle("Sara Record")
                .setView(container)
                .setPositiveButton("Band Karein", null)
                .show();
    }

    // ---------------------------------------------------------------
    // Simple add-record dialogs for expenses / payments / receipts / purchases
    // ---------------------------------------------------------------
    private void showExpenseDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        layout.setPadding(pad, pad, pad, pad);
        EditText title = newField(layout, "Kharche ka naam", false);
        EditText amount = newField(layout, "Amount (Rs)", true);

        new AlertDialog.Builder(this)
                .setTitle("Kharcha Add Karein")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    ContentValues cv = new ContentValues();
                    cv.put("date", today());
                    cv.put("title", title.getText().toString().trim());
                    cv.put("amount", parseD(amount));
                    db.getWritableDatabase().insert("expenses", null, cv);
                    Toast.makeText(this, "Kharcha save ho gaya", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPaymentDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        layout.setPadding(pad, pad, pad, pad);
        EditText person = newField(layout, "Kis ko payment (naam)", false);
        EditText amount = newField(layout, "Amount (Rs)", true);
        EditText note = newField(layout, "Note (optional)", false);

        new AlertDialog.Builder(this)
                .setTitle("Payment Add Karein")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    ContentValues cv = new ContentValues();
                    cv.put("date", today());
                    cv.put("person", person.getText().toString().trim());
                    cv.put("amount", parseD(amount));
                    cv.put("note", note.getText().toString().trim());
                    db.getWritableDatabase().insert("payments", null, cv);
                    Toast.makeText(this, "Payment save ho gaya", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showReceiptDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        layout.setPadding(pad, pad, pad, pad);
        AutoCompleteTextView millView = new AutoCompleteTextView(this);
        millView.setHint("Mill ka naam");
        millView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, db.getMillNames()));
        layout.addView(millView);
        EditText amount = newField(layout, "Amount (Rs)", true);
        EditText note = newField(layout, "Note (optional)", false);

        new AlertDialog.Builder(this)
                .setTitle("Mill Receipt Add Karein")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String mill = millView.getText().toString().trim();
                    db.addMillIfNotExists(mill);
                    ContentValues cv = new ContentValues();
                    cv.put("date", today());
                    cv.put("mill", mill);
                    cv.put("amount", parseD(amount));
                    cv.put("note", note.getText().toString().trim());
                    db.getWritableDatabase().insert("receipts", null, cv);
                    Toast.makeText(this, "Receipt save ho gaya", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPurchaseDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        layout.setPadding(pad, pad, pad, pad);
        EditText seller = newField(layout, "Bechne wale ka naam", false);
        EditText maund = newField(layout, "Maund", true);
        EditText rate = newField(layout, "Rate", true);
        EditText paid = newField(layout, "Kitna paid kiya (Rs)", true);

        new AlertDialog.Builder(this)
                .setTitle("Purchase Add Karein")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    double m = parseD(maund);
                    double r = parseD(rate);
                    double total = m * r;
                    ContentValues cv = new ContentValues();
                    cv.put("date", today());
                    cv.put("seller", seller.getText().toString().trim());
                    cv.put("maund", m);
                    cv.put("rate", r);
                    cv.put("total", total);
                    cv.put("paid", parseD(paid));
                    db.getWritableDatabase().insert("purchases", null, cv);
                    Toast.makeText(this, "Purchase save ho gaya", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------------------------------------------------------------
    // Export every saved trip to a CSV file in Downloads.
    // ---------------------------------------------------------------
    private void exportCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("Date,Mill,Driver,DriverPhone,Truck,Maund,DriverCost,OtherCost,SaleRate,SaleTotal,Status\n");
        Cursor c = db.getAllTrips();
        while (c.moveToNext()) {
            for (int i = 0; i < c.getColumnCount(); i++) {
                sb.append(csvEscape(c.getString(i)));
                sb.append(i == c.getColumnCount() - 1 ? "\n" : ",");
            }
        }
        c.close();

        String fileName = "GannaHisab_" + System.currentTimeMillis() + ".csv";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                cv.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (uri != null) {
                    try (OutputStream out = resolver.openOutputStream(uri)) {
                        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                    }
                    Toast.makeText(this, "CSV Downloads mein save ho gayi: " + fileName, Toast.LENGTH_LONG).show();
                    return;
                }
            }
            // fallback for older Android versions
            java.io.File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            java.io.File file = new java.io.File(dir, fileName);
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
                out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            }
            Toast.makeText(this, "CSV Downloads mein save ho gayi: " + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export nahi ho saka: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String csvEscape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
