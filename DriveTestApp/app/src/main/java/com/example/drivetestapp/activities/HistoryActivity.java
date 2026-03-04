package com.example.drivetestapp.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.example.drivetestapp.R;
import com.example.drivetestapp.database.DatabaseHelper;
import com.example.drivetestapp.models.NetworkMeasurement;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class HistoryActivity extends AppCompatActivity {
    private final ArrayList<String> logList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private DatabaseHelper db;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private final BroadcastReceiver dynamicUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkMeasurement m = intent.getParcelableExtra("data");
            if (m != null) {
                logList.add(0, formatFullDataEntry(m));
                adapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = new DatabaseHelper(this);
        ListView lv = findViewById(R.id.lv);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, logList);
        lv.setAdapter(adapter);

        loadInitialData();

        findViewById(R.id.btnClearHistory).setOnClickListener(v -> {
            db.deleteAllLogs();
            logList.clear();
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Database Cleared", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnExportExcel).setOnClickListener(v -> export("csv"));
        findViewById(R.id.btnExportKML).setOnClickListener(v -> export("kml"));
    }

    private void loadInitialData() {
        List<NetworkMeasurement> logs = db.getAllLocalLogs();
        logList.clear();
        for (NetworkMeasurement m : logs) {
            logList.add(formatFullDataEntry(m));
        }
        adapter.notifyDataSetChanged();
    }

    private String formatFullDataEntry(NetworkMeasurement m) {
        StringBuilder sb = new StringBuilder();
        sb.append("TIME: ").append(sdf.format(new Date(m.timestamp))).append("\n");
        sb.append("LOC: ").append(m.latitude).append(", ").append(m.longitude).append("\n");
        sb.append("PLMN: ").append(m.mcc).append("-").append(m.mnc).append(" (").append(m.operatorName).append(")\n");

        if ("4G".equals(m.ratType)) {
            sb.append("--- [4G LTE] ---\n");
            sb.append("RSRP: ").append(m.rsrp).append(" dBm | RSRQ: ").append(m.rsrq).append(" dB\n");
            sb.append("RSSI: ").append(m.rssi).append(" dBm | SINR: ").append(m.snr).append("\n");
            sb.append("PCI: ").append(m.pci).append(" | TAC: ").append(m.tac).append(" | EARFCN: ").append(m.earfcn);
        } else if ("3G".equals(m.ratType)) {
            sb.append("--- [3G WCDMA] ---\n");
            sb.append("RSCP: ").append(m.rscp).append(" dBm | Ec/No: ").append(m.ecNo).append(" dB\n");
            sb.append("PSC: ").append(m.psc).append(" | LAC: ").append(m.lac).append(" | UARFCN: ").append(m.uarfcn);
        } else if ("2G".equals(m.ratType)) {
            sb.append("--- [2G GSM] ---\n");
            sb.append("RxLev (RSSI): ").append(m.rxLev).append(" dBm\n");
            sb.append("RxQual (BER): ").append(m.rxQual == -1 ? "N/A" : m.rxQual).append("\n");
            sb.append("BSIC: ").append(m.bsic).append(" | LAC: ").append(m.lac).append(" | ARFCN: ").append(m.arfcn);
        }

        sb.append("\nCID: ").append(m.cellId);
        return sb.toString();
    }

    private void export(String ext) {
        File path = new File(getCacheDir(), "exports");
        path.mkdirs();
        File file = new File(path, "DriveTest_Full_Log_" + System.currentTimeMillis() + "." + ext);

        try (FileWriter w = new FileWriter(file)) {
            if (ext.equals("csv")) {
                // Header updated to Standard Telecom Naming
                w.append("Date_Time,RAT,Operator,MCC,MNC,Latitude,Longitude,Cell_ID,LAC,TAC,PCI,PSC,BSIC,")
                        .append("RSRP_dBm,RSRQ_dB,RSSI_dBm,SINR_dB,RSCP_dBm,EcNo_dB,RxLev_dBm,RxQual,EARFCN,UARFCN,ARFCN\n");

                for (NetworkMeasurement m : db.getAllLocalLogs()) {
                    w.append(sdf.format(new Date(m.timestamp))).append(",")
                            .append(m.ratType).append(",")
                            .append(m.operatorName).append(",")
                            .append(m.mcc).append(",")
                            .append(m.mnc).append(",")
                            .append(String.valueOf(m.latitude)).append(",")
                            .append(String.valueOf(m.longitude)).append(",")
                            .append(String.valueOf(m.cellId)).append(",")
                            .append(String.valueOf(m.lac)).append(",")
                            .append(String.valueOf(m.tac)).append(",")
                            .append(String.valueOf(m.pci)).append(",")
                            .append(String.valueOf(m.psc)).append(",")
                            .append(String.valueOf(m.bsic)).append(",")
                            .append(String.valueOf(m.rsrp)).append(",")
                            .append(String.valueOf(m.rsrq)).append(",")
                            .append(String.valueOf(m.rssi)).append(",")
                            .append(String.valueOf(m.snr)).append(",")
                            .append(String.valueOf(m.rscp)).append(",")
                            .append(String.valueOf(m.ecNo)).append(",")
                            .append(String.valueOf(m.rxLev)).append(",")
                            .append(String.valueOf(m.rxQual)).append(",")
                            .append(String.valueOf(m.earfcn)).append(",")
                            .append(String.valueOf(m.uarfcn)).append(",")
                            .append(String.valueOf(m.arfcn)).append("\n");
                }
            } else {
                // KML with HTML description and requested Color Logic
                w.append("<?xml version=\"1.0\"?><kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document>");
                for (NetworkMeasurement m : db.getAllLocalLogs()) {
                    int s = m.ratType.equals("4G") ? m.rsrp : (m.ratType.equals("3G") ? m.rscp : m.rxLev);

                    // Color mapping: Red (Signal), Yellow (Good), Green (Excellent)
                    // Telecom standard thresholds for RSRP/RSCP:
                    // Excellent > -85dBm, Good > -100dBm, Fair/Poor <= -100dBm
                    String color = s > -85 ? "ff00ff00" : (s > -100 ? "ff00ffff" : "ff0000ff");

                    w.append("<Placemark><name>").append(m.ratType).append("</name>")
                            .append("<description><![CDATA[")
                            .append(formatFullDataEntry(m).replace("\n", "<br/>"))
                            .append("]]></description>")
                            .append("<Style><IconStyle><scale>1.2</scale><color>").append(color).append("</color></IconStyle></Style>")
                            .append("<Point><coordinates>").append(String.valueOf(m.longitude)).append(",").append(String.valueOf(m.latitude)).append(",0</coordinates></Point></Placemark>");
                }
                w.append("</Document></kml>");
            }
            w.flush();
            share(file);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void share(File f) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
        Intent intent = new Intent(Intent.ACTION_SEND).setType("*/*")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Export via..."));
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(dynamicUpdateReceiver, new IntentFilter("UI_UPDATE"), Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(dynamicUpdateReceiver);
    }
}