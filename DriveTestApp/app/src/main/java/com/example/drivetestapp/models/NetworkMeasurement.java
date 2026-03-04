package com.example.drivetestapp.models;

import android.os.Parcel;
import android.os.Parcelable;

public class NetworkMeasurement implements Parcelable {
    public long timestamp = System.currentTimeMillis();
    public String ratType = "", operatorName = "", mcc = "0", mnc = "0";
    public double latitude = 0, longitude = 0;
    public int cellId = 0, lac = 0, tac = 0, pci = 0, psc = 0, bsic = 0;

    // Signal Metrics
    public int rsrp, rsrq, snr, rssi; // 4G
    public int rscp, ecNo; // 3G
    public int rxLev, rxQual; // 2G (RxQual: 0-7)

    // Frequency
    public int arfcn = 0, earfcn = 0, uarfcn = 0;

    public NetworkMeasurement() {}

    protected NetworkMeasurement(Parcel in) {
        timestamp = in.readLong(); ratType = in.readString(); operatorName = in.readString();
        mcc = in.readString(); mnc = in.readString(); latitude = in.readDouble();
        longitude = in.readDouble(); cellId = in.readInt(); lac = in.readInt();
        tac = in.readInt(); pci = in.readInt(); psc = in.readInt(); bsic = in.readInt();
        rsrp = in.readInt(); rsrq = in.readInt(); snr = in.readInt(); rssi = in.readInt();
        rscp = in.readInt(); ecNo = in.readInt(); rxLev = in.readInt(); rxQual = in.readInt();
        arfcn = in.readInt(); earfcn = in.readInt(); uarfcn = in.readInt();
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(timestamp); dest.writeString(ratType); dest.writeString(operatorName);
        dest.writeString(mcc); dest.writeString(mnc); dest.writeDouble(latitude);
        dest.writeDouble(longitude); dest.writeInt(cellId); dest.writeInt(lac);
        dest.writeInt(tac); dest.writeInt(pci); dest.writeInt(psc); dest.writeInt(bsic);
        dest.writeInt(rsrp); dest.writeInt(rsrq); dest.writeInt(snr); dest.writeInt(rssi);
        dest.writeInt(rscp); dest.writeInt(ecNo); dest.writeInt(rxLev); dest.writeInt(rxQual);
        dest.writeInt(arfcn); dest.writeInt(earfcn); dest.writeInt(uarfcn);
    }

    @Override public int describeContents() { return 0; }
    public static final Creator<NetworkMeasurement> CREATOR = new Creator<NetworkMeasurement>() {
        @Override public NetworkMeasurement createFromParcel(Parcel in) { return new NetworkMeasurement(in); }
        @Override public NetworkMeasurement[] newArray(int size) { return new NetworkMeasurement[size]; }
    };
}