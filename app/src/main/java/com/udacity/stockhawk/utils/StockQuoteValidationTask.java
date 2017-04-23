package com.udacity.stockhawk.utils;

import android.os.AsyncTask;
import android.support.annotation.IntDef;
import android.util.Log;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;

public class StockQuoteValidationTask extends AsyncTask<StockQuoteValidationTask.StockQuoteValidationRequest, Void, StockQuoteValidationTask.StockQuoteValidationResult> {

    private static final String TAG = StockQuoteValidationTask.class.getName();

    private final ValidationListener mListener;

    public StockQuoteValidationTask(ValidationListener listener) {
        mListener = listener;
    }

    @Override
    protected StockQuoteValidationResult doInBackground(StockQuoteValidationRequest... params) {
        StockQuoteValidationRequest request = params[0];
        try {
            Stock stock = YahooFinance.get(request.stockCode);
            Log.d(TAG, "StockQuoteValidationTask.doInBackground: " + stock);
            return new StockQuoteValidationResult(request.stockCode, stock, getCode(stock));
        } catch (IOException e) {
            Log.e(TAG, "StockQuoteValidationTask.doInBackground: Stock not found " + request.stockCode);
            return new StockQuoteValidationResult(request.stockCode, null, STOCK_QUOTE_NOT_EXIST);
        }
    }

    private int getCode(Stock stock) {
        if (stock == null || stock.getQuote() == null || stock.getQuote().getPrice() == null)
            return STOCK_QUOTE_DATA_NOT_EXIST;
        return STOCK_QUOTE_OK;
    }

    @Override
    protected void onPostExecute(StockQuoteValidationResult result) {
        mListener.onValidationResult(result);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STOCK_QUOTE_NOT_EXIST, STOCK_QUOTE_DATA_NOT_EXIST, STOCK_QUOTE_OK})
    public @interface StockQuoteValidationCode {
    }

    public static final int STOCK_QUOTE_NOT_EXIST = 0;
    public static final int STOCK_QUOTE_DATA_NOT_EXIST = 1;
    public static final int STOCK_QUOTE_OK = 2;

    public interface ValidationListener {
        void onValidationResult(StockQuoteValidationResult result);
    }

    public static class StockQuoteValidationRequest {
        public String stockCode;

        public StockQuoteValidationRequest(String stockCode) {
            this.stockCode = stockCode;
        }
    }

    public static class StockQuoteValidationResult {
        public final String stockCode;
        public final @StockQuoteValidationCode int code;
        public final Stock stock;

        public StockQuoteValidationResult(String stockCode, Stock stock, @StockQuoteValidationCode int code) {
            this.stockCode = stockCode;
            this.stock = stock;
            this.code = code;
        }
    }
}
