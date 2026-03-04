package com.example.drivetestapp.utils;

public class OperatorMapper {
    public static String getOperatorName(String mccMnc) {
        if (mccMnc == null) return "Unknown";
        switch (mccMnc) {
            case "62401": return "MTN";
            case "62402": return "Orange";
            case "62404": return "Nexttel";
            case "62405": return "BLUE (Camtel)";
            default: return "Other (" + mccMnc + ")";
        }
    }
}