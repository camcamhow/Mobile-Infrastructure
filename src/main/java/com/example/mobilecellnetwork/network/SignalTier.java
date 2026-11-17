package com.example.mobilecellnetwork.network;

public enum SignalTier {
    FIVE_G_PLUS("5G+", true),
    FIVE_G("5G", true),
    FOUR_G_PLUS("4G+", true),
    FOUR_G("4G", true),
    LTE("LTE", true),
    THREE_G("3G", true),
    H_PLUS("H+", true),
    H("H", true),
    TWO_G("2G", true),
    E_PLUS("E+", true),
    E("E", true),
    NO_SERVICE("No Service", false);

    private final String displayName;
    private final boolean dataCapable;

    SignalTier(String displayName, boolean dataCapable) {
        this.displayName = displayName;
        this.dataCapable = dataCapable;
    }

    public String displayName() {
        return displayName;
    }

    public static String displayName(String enumName) {
        try {
            return valueOf(enumName).displayName();
        } catch (IllegalArgumentException | NullPointerException exception) {
            return enumName == null ? "" : enumName;
        }
    }

    public boolean isDataCapable() {
        return dataCapable;
    }
}
