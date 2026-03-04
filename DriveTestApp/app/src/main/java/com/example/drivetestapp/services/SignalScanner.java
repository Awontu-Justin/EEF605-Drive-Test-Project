package com.example.drivetestapp.services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.*;
import com.example.drivetestapp.models.NetworkMeasurement;
import java.util.List;

public class SignalScanner {
    private final Context context;
    private final TelephonyManager tm;
    private final LocationManager lm;
    private Location currentBestLocation = null;

    public SignalScanner(Context context) {
        this.context = context;
        this.tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        this.lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        initLastKnownLocation();
        startActiveLocationUpdates();
    }

    @SuppressLint("MissingPermission")
    private void initLastKnownLocation() {
        Location gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (gpsLoc != null) currentBestLocation = gpsLoc;
        else if (netLoc != null) currentBestLocation = netLoc;
    }

    @SuppressLint("MissingPermission")
    private void startActiveLocationUpdates() {
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                currentBestLocation = location;
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) {}
        };

        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);
        }
        if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 0, locationListener);
        }
    }

    @SuppressLint("MissingPermission")
    public NetworkMeasurement getLatestMeasurement() {
        // CONSTRAINT REMOVED: We no longer return null if location is missing.
        NetworkMeasurement m = new NetworkMeasurement();
        m.timestamp = System.currentTimeMillis();

        if (currentBestLocation != null) {
            m.latitude = currentBestLocation.getLatitude();
            m.longitude = currentBestLocation.getLongitude();
        } else {
            // Fallback to 0.0 so we can still see signal data
            m.latitude = 0.0;
            m.longitude = 0.0;
        }

        String networkOperator = tm.getNetworkOperator();
        if (networkOperator != null && networkOperator.length() >= 5) {
            m.mcc = networkOperator.substring(0, 3);
            m.mnc = networkOperator.substring(3);
        }
        m.operatorName = tm.getNetworkOperatorName();

        List<CellInfo> cellInfoList = tm.getAllCellInfo();
        if (cellInfoList != null) {
            for (CellInfo info : cellInfoList) {
                if (info.isRegistered()) {
                    if (info instanceof CellInfoLte) mapLteData((CellInfoLte) info, m);
                    else if (info instanceof CellInfoWcdma) mapWcdmaData((CellInfoWcdma) info, m);
                    else if (info instanceof CellInfoGsm) mapGsmData((CellInfoGsm) info, m);
                    break;
                }
            }
        }
        return m;
    }

    private void mapLteData(CellInfoLte lte, NetworkMeasurement m) {
        m.ratType = "4G";
        CellIdentityLte id = lte.getCellIdentity();
        CellSignalStrengthLte ss = lte.getCellSignalStrength();
        m.cellId = id.getCi();
        m.pci = id.getPci();
        m.tac = id.getTac();
        m.earfcn = id.getEarfcn();
        m.rsrp = ss.getRsrp();
        m.rsrq = ss.getRsrq();
        m.snr = ss.getRssnr();
        m.rssi = ss.getRssi();
    }

    private void mapWcdmaData(CellInfoWcdma wcdma, NetworkMeasurement m) {
        m.ratType = "3G";
        CellIdentityWcdma id = wcdma.getCellIdentity();
        CellSignalStrengthWcdma ss = wcdma.getCellSignalStrength();
        m.cellId = id.getCid();
        m.psc = id.getPsc();
        m.lac = id.getLac();
        m.uarfcn = id.getUarfcn();
        m.rscp = ss.getDbm();
        m.ecNo = ss.getEcNo();
    }

    private void mapGsmData(CellInfoGsm gsm, NetworkMeasurement m) {
        m.ratType = "2G";
        CellIdentityGsm id = gsm.getCellIdentity();
        CellSignalStrengthGsm ss = gsm.getCellSignalStrength();
        m.cellId = id.getCid();
        m.lac = id.getLac();
        m.bsic = id.getBsic();
        m.arfcn = id.getArfcn();
        m.rxLev = ss.getDbm();
        m.rssi = m.rxLev;
        m.rxQual = ss.getBitErrorRate();
    }
}