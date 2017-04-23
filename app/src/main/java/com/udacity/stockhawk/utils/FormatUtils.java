package com.udacity.stockhawk.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class FormatUtils {
    private static final DecimalFormat sDollarFormat;
    private static final DecimalFormat sDollarFormatWithPlus;
    private static final DecimalFormat sPercentageFormat;

    static {
        sDollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        sDollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        sDollarFormatWithPlus.setPositivePrefix("+$");
        sPercentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        sPercentageFormat.setMaximumFractionDigits(2);
        sPercentageFormat.setMinimumFractionDigits(2);
        sPercentageFormat.setPositivePrefix("+");
    }

    public static String dollarFormat(float f) {
        return sDollarFormat.format(f);
    }

    public static String dollarFormatWithPlus(float f) {
        return sDollarFormatWithPlus.format(f);
    }

    public static String percentageFormat(float f) {
        return sPercentageFormat.format(f);
    }
}
