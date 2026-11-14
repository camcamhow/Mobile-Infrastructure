package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPhoneCache;

public final class PhoneColors {
    public static final int SAMSUNG_BG_PRIMARY = 0xFF000000;
    public static final int SAMSUNG_BG_SECONDARY = 0xFF1C1C1E;
    public static final int SAMSUNG_BG_TERTIARY = 0xFF2C2C2E;
    public static final int SAMSUNG_TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int SAMSUNG_TEXT_SECONDARY = 0xFF8E8E93;
    public static final int SAMSUNG_ACCENT = 0xFF1F7AE0;
    public static final int SAMSUNG_SUCCESS = 0xFF34C759;
    public static final int SAMSUNG_WARN = 0xFFFF9F0A;
    public static final int SAMSUNG_ERROR = 0xFFFF453A;
    public static final int SAMSUNG_DIVIDER = 0xFF38383A;

    public static final int IOS_BG_PRIMARY = 0xFF000000;
    public static final int IOS_BG_SECONDARY = 0xFF1C1C1E;
    public static final int IOS_BG_TERTIARY = 0xFF2C2C2E;
    public static final int IOS_TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int IOS_TEXT_SECONDARY = 0xFF8E8E93;
    public static final int IOS_ACCENT = 0xFF0A84FF;
    public static final int IOS_SUCCESS = 0xFF30D158;
    public static final int IOS_WARN = 0xFFFF9F0A;
    public static final int IOS_ERROR = 0xFFFF453A;
    public static final int IOS_DIVIDER = 0xFF38383A;

    private static final int DEFAULT_BG_PRIMARY = 0xFF101014;
    private static final int DEFAULT_BG_SECONDARY = 0xFF202030;
    private static final int DEFAULT_BG_TERTIARY = 0xFF2A2A38;
    private static final int DEFAULT_TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int DEFAULT_TEXT_SECONDARY = 0xFFB0B0B0;
    private static final int DEFAULT_ACCENT = 0xFF7FD4FF;
    private static final int DEFAULT_SUCCESS = 0xFF7CFF7C;
    private static final int DEFAULT_WARN = 0xFFFFD966;
    private static final int DEFAULT_ERROR = 0xFFFF5050;
    private static final int DEFAULT_DIVIDER = 0xFF505060;
    private static final int IOS_SURFACE_PRIMARY = 0xFFF2F2F7;
    private static final int IOS_SURFACE_SECONDARY = 0xFFFFFFFF;
    private static final int IOS_SURFACE_TERTIARY = 0xFFE5E5EA;
    private static final int IOS_LABEL_PRIMARY = 0xFF1C1C1E;
    private static final int IOS_LABEL_SECONDARY = 0xFF6C6C70;
    private static final int IOS_TINT = 0xFF007AFF;
    private static final int IOS_SEPARATOR = 0xFFD1D1D6;

    public static int PRIMARY_TEXT = 0xFFFFFFFF;
    public static int SECONDARY_TEXT = 0xFFB0B0B0;
    public static int ACCENT_TEXT = 0xFF7FD4FF;
    public static int SUCCESS_TEXT = 0xFF7CFF7C;
    public static int WARNING_TEXT = 0xFFFFD966;
    public static int ERROR_TEXT = 0xFFFF5050;
    public static int PRICE_TEXT = 0xFFFFD966;
    public static int ROW_BG = 0x80202030;
    public static int ROW_BORDER = 0xFF505060;

    private PhoneColors() {
    }

    public static int bgPrimary(PhoneStyle style) {
        return switch (style) {
            case IOS -> IOS_SURFACE_PRIMARY;
            case DEFAULT -> DEFAULT_BG_PRIMARY;
            case SAMSUNG_ONE_UI -> SAMSUNG_BG_PRIMARY;
        };
    }

    public static int bgSecondary(PhoneStyle style) {
        return switch (style) {
            case IOS -> IOS_SURFACE_SECONDARY;
            case DEFAULT -> DEFAULT_BG_SECONDARY;
            case SAMSUNG_ONE_UI -> SAMSUNG_BG_SECONDARY;
        };
    }

    public static int bgTertiary(PhoneStyle style) {
        return switch (style) {
            case IOS -> IOS_SURFACE_TERTIARY;
            case DEFAULT -> DEFAULT_BG_TERTIARY;
            case SAMSUNG_ONE_UI -> SAMSUNG_BG_TERTIARY;
        };
    }

    public static int textPrimary(PhoneStyle style) {
        return switch (style) {
            case IOS -> IOS_LABEL_PRIMARY;
            case DEFAULT -> DEFAULT_TEXT_PRIMARY;
            case SAMSUNG_ONE_UI -> SAMSUNG_TEXT_PRIMARY;
        };
    }

    public static int textSecondary(PhoneStyle style) {
        return switch (style) {
            case IOS -> IOS_LABEL_SECONDARY;
            case DEFAULT -> DEFAULT_TEXT_SECONDARY;
            case SAMSUNG_ONE_UI -> SAMSUNG_TEXT_SECONDARY;
        };
    }

    public static int accent(PhoneStyle style) {
        return switch (style) {
            case IOS -> IOS_TINT;
            case DEFAULT -> DEFAULT_ACCENT;
            case SAMSUNG_ONE_UI -> SAMSUNG_ACCENT;
        };
    }

    public static int success(PhoneStyle style) {
        return switch (style) {
            case IOS -> IOS_SUCCESS;
            case DEFAULT -> DEFAULT_SUCCESS;
            case SAMSUNG_ONE_UI -> SAMSUNG_SUCCESS;
        };
    }

    public static int warn(PhoneStyle style) {
        return switch (style) {
            case IOS -> IOS_WARN;
            case DEFAULT -> DEFAULT_WARN;
            case SAMSUNG_ONE_UI -> SAMSUNG_WARN;
        };
    }

    public static int error(PhoneStyle style) {
        return switch (style) {
            case IOS -> IOS_ERROR;
            case DEFAULT -> DEFAULT_ERROR;
            case SAMSUNG_ONE_UI -> SAMSUNG_ERROR;
        };
    }

    public static int divider(PhoneStyle style) {
        return switch (style) {
            case IOS -> IOS_SEPARATOR;
            case DEFAULT -> DEFAULT_DIVIDER;
            case SAMSUNG_ONE_UI -> SAMSUNG_DIVIDER;
        };
    }

    /** Returns the appropriate text color for buttons filled with the accent color. */
    public static int buttonTextOnAccent(PhoneStyle style) {
        return switch (style) {
            case SAMSUNG_ONE_UI, IOS -> SAMSUNG_TEXT_PRIMARY;
            case DEFAULT -> textPrimary(style);
        };
    }

    /** Returns the appropriate text color for buttons filled with the error color. */
    public static int buttonTextOnDestructive(PhoneStyle style) {
        return switch (style) {
            case SAMSUNG_ONE_UI, IOS -> SAMSUNG_TEXT_PRIMARY;
            case DEFAULT -> textPrimary(style);
        };
    }

    /** Returns the appropriate text color for buttons filled with the tertiary background color. */
    public static int buttonTextOnSecondary(PhoneStyle style) {
        return textPrimary(style);
    }

    /** Returns the semi-transparent black shadow color for the selected style. */
    public static int shadow(PhoneStyle style) {
        return switch (style) {
            case SAMSUNG_ONE_UI, IOS -> 0x66000000;
            case DEFAULT -> 0x40000000;
        };
    }

    public static void applyTheme() {
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        PRIMARY_TEXT = textPrimary(style);
        SECONDARY_TEXT = textSecondary(style);
        ACCENT_TEXT = accent(style);
        SUCCESS_TEXT = success(style);
        WARNING_TEXT = warn(style);
        ERROR_TEXT = error(style);
        PRICE_TEXT = warn(style);
        ROW_BG = bgTertiary(style);
        ROW_BORDER = divider(style);
    }
}
