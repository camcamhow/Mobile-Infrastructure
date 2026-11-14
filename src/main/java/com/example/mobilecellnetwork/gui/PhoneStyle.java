package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;

public enum PhoneStyle {
    SAMSUNG_ONE_UI,
    IOS,
    DEFAULT;

    public static PhoneStyle fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return DEFAULT;
        }
    }

    public static PhoneStyle configuredDefault() {
        return MobileCellNetworkConfig.DEFAULT_PHONE_STYLE.get();
    }
}
