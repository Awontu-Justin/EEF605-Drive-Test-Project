package com.example.drivetestapp.services;

import android.Manifest;
import android.app.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.example.drivetestapp.R;
import com.example.drivetestapp.database.DatabaseHelper;
import com.example.drivetestapp.models.NetworkMeasurement;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;

public class DriveTestService extends Service {
    private DatabaseHelper db;
    private DatabaseReference fb;
    private SignalScanner scanner;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int totalSynced = 0;
    private static final String CHANNEL_ID = "DriveTestSyncChannel";
    private static final int NOTIF_ID = 1;

    private Runnable scanTask = new Runnable() {
        @Override
        public void run() {
            NetworkMeasurement m = scanner.getLatestMeasurement();

            if (m != null) {
                db.saveAllData(m);
                Intent intent = new Intent("UI_UPDATE");
                intent.putExtra("data", m);
                sendBroadcast(intent);
                syncToFirebase();

                // RESTORED & ENHANCED NOTIFICATION LOGIC
                String gpsStatus = (m.latitude == 0.0 && m.longitude == 0.0) ? "Searching GPS" : "GPS Locked";
                updateNotification("Active (" + gpsStatus + ") | Synced: " + totalSynced);
            }

            handler.postDelayed(this, 3000);
        }
    };

    private void syncToFirebase() {
        List<NetworkMeasurement> pending = db.getUnsyncedData(10);
        for (NetworkMeasurement record : pending) {
            fb.child(String.valueOf(record.timestamp)).setValue(record)
                    .addOnSuccessListener(aVoid -> {
                        db.markAsSynced(record.timestamp);
                        totalSynced++;
                        // Immediate update to show progress as it happens
                        updateNotification("Syncing... Total: " + totalSynced);
                    });
        }
    }

    private void updateNotification(String status) {
        Notification notification = createNotificationBuilder(status).build();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, notification);
    }

    private NotificationCompat.Builder createNotificationBuilder(String content) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Drive Test Active")
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        db = new DatabaseHelper(this);
        fb = FirebaseDatabase.getInstance().getReference("logs");
        scanner = new SignalScanner(this);

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Sync Status", NotificationManager.IMPORTANCE_LOW);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.createNotificationChannel(channel);

        Notification notification = createNotificationBuilder("Initializing Scanner...").build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIF_ID, notification);
        }

        handler.post(scanTask);
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        startService(restartServiceIntent);
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(scanTask);
        super.onDestroy();
    }
}