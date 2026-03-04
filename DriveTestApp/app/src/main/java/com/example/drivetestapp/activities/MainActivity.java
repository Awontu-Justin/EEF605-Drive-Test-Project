package com.example.drivetestapp.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.example.drivetestapp.R;
import com.example.drivetestapp.models.NetworkMeasurement;
import com.example.drivetestapp.services.DriveTestService;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int PERM_CODE = 101;
    private TextView tvOp, tvRat, tvLoc, tvStatus, tvTime;
    private View statusColorBar;
    private final TextView[] grids = new TextView[8];
    private boolean isUiUpdating = true;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private final BroadcastReceiver uiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isUiUpdating) return;
            NetworkMeasurement m = intent.getParcelableExtra("data");
            if (m != null) updateDisplay(m);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvOp = findViewById(R.id.tvOp);
        tvRat = findViewById(R.id.tvRat);
        tvLoc = findViewById(R.id.tvLoc);
        tvStatus = findViewById(R.id.tvStatus);
        tvTime = findViewById(R.id.tvTime);
        statusColorBar = findViewById(R.id.statusColorBar);

        for (int i = 0; i < 8; i++) {
            int resId = getResources().getIdentifier("s" + (i + 1), "id", getPackageName());
            grids[i] = findViewById(resId);
        }

        // STILL ASKS FOR PERMISSIONS
        if (checkAndRequestPermissions()) {
            attemptStartService();
        }

        findViewById(R.id.btnStart).setOnClickListener(v -> {
            if (checkAndRequestPermissions()) {
                isUiUpdating = true;
                attemptStartService();
            }
        });

        findViewById(R.id.btnStop).setOnClickListener(v -> isUiUpdating = false);
        findViewById(R.id.btnHistory).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));
    }

    private boolean checkAndRequestPermissions() {
        List<String> listPermissionsNeeded = new ArrayList<>();
        listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listPermissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> listToRequest = new ArrayList<>();
        for (String perm : listPermissionsNeeded) {
            if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                listToRequest.add(perm);
            }
        }

        if (!listToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, listToRequest.toArray(new String[0]), PERM_CODE);
            return false;
        }
        return true;
    }

    private void attemptStartService() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isGpsEnabled = lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        // Starts service anyway, but prompts for GPS if disabled
        Intent serviceIntent = new Intent(this, DriveTestService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        if (!isGpsEnabled) {
            showGpsDialog();
        }
    }

    private void showGpsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("GPS Recommended")
                .setMessage("GPS is currently disabled. You can still see measurements, but location data will be 0.0.")
                .setPositiveButton("Settings", (d, w) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("Ignore", null).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_CODE && grantResults.length > 0) {
            attemptStartService();
        }
    }

    private void updateDisplay(NetworkMeasurement m) {
        tvTime.setText("LATEST: " + timeFormat.format(new Date(m.timestamp)));
        tvOp.setText(m.operatorName + " [" + m.mcc + "-" + m.mnc + "]");
        tvRat.setText("TECH: " + m.ratType);

        // Show 0.0 if GPS is not yet available
        tvLoc.setText(String.format(Locale.US, "GPS: %.5f, %.5f", m.latitude, m.longitude));

        int primarySignal;
        if ("4G".equals(m.ratType)) {
            primarySignal = m.rsrp;
            grids[0].setText("RSRP: " + m.rsrp + " dBm");
            grids[1].setText("RSRQ: " + m.rsrq + " dB");
            grids[2].setText("RSSI: " + m.rssi + " dBm");
            grids[3].setText("SINR: " + m.snr);
            grids[4].setText("PCI: " + m.pci);
            grids[5].setText("TAC: " + m.tac);
            grids[6].setText("EARFCN: " + m.earfcn);
            grids[7].setText("CID: " + m.cellId);
        } else if ("3G".equals(m.ratType)) {
            primarySignal = m.rscp;
            grids[0].setText("RSCP: " + m.rscp + " dBm");
            grids[1].setText("Ec/No: " + m.ecNo + " dB");
            grids[2].setText("RSSI: " + m.rssi + " dBm");
            grids[3].setText("PSC: " + m.psc);
            grids[4].setText("LAC: " + m.lac);
            grids[5].setText("UARFCN: " + m.uarfcn);
            grids[6].setText("CID: " + m.cellId);
            grids[7].setText("TYPE: WCDMA");
        } else {
            primarySignal = m.rxLev;
            grids[0].setText("RxLev: " + m.rxLev + " dBm");
            grids[1].setText("RxQual: " + (m.rxQual == -1 ? "N/A" : m.rxQual));
            grids[2].setText("RSSI: " + m.rssi + " dBm");
            grids[3].setText("BSIC: " + m.bsic);
            grids[4].setText("LAC: " + m.lac);
            grids[5].setText("ARFCN: " + m.arfcn);
            grids[6].setText("CID: " + m.cellId);
            grids[7].setText("TYPE: GSM");
        }

        if (primarySignal > -90) {
            statusColorBar.setBackgroundColor(Color.GREEN);
            tvStatus.setText("EXCELLENT COVERAGE");
        } else if (primarySignal > -105) {
            statusColorBar.setBackgroundColor(Color.YELLOW);
            tvStatus.setText("STABLE COVERAGE");
        } else {
            statusColorBar.setBackgroundColor(Color.RED);
            tvStatus.setText("POOR COVERAGE");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(uiReceiver, new IntentFilter("UI_UPDATE"), Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { unregisterReceiver(uiReceiver); } catch (Exception e) {}
    }
}