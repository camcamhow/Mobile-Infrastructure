package com.example.mobilecellnetwork.util;

import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;

public final class CurrencyUtil {

    private CurrencyUtil() {}

    /**
     * Formats a long amount into a currency string.
     * e.g. "$1000"
     *
     * @param amount the amount to format
     * @return the formatted currency string
     */
    public static String format(long amount) {
        String number = Long.toString(amount);
        String symbol = MobileCellNetworkConfig.CURRENCY_SYMBOL.get();
        return number.startsWith("-") ? "-" + symbol + number.substring(1) : symbol + number;
    }

    /**
     * Formats a long amount into a currency string with the currency name.
     * e.g. "1000 dollars" (pluralizes when amount != 1)
     *
     * @param amount the amount to format
     * @return the formatted currency string with name
     */
    public static String formatLong(long amount) {
        String name = amount == 1
                ? MobileCellNetworkConfig.CURRENCY_NAME.get()
                : MobileCellNetworkConfig.CURRENCY_NAME_PLURAL.get();
        return Long.toString(amount) + " " + name;
    }
}