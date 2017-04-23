package com.udacity.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.utils.FormatUtils;

import java.util.ArrayList;

import timber.log.Timber;


public class StockQuotesWidgetService extends RemoteViewsService {
    private class StockQuoteItem {
        String symbol;
        float price;
        float absoluteChange;
        int percentageChange;

        @Override
        public String toString() {
            return "StockQuoteItem{" +
                    "symbol='" + symbol + '\'' +
                    ", price=" + price +
                    ", absoluteChange=" + absoluteChange +
                    ", percentageChange=" + percentageChange +
                    '}';
        }
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private ArrayList<StockQuoteItem> mList = new ArrayList<>();

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {
                Timber.d("loading refreshed list of stocks");
                Cursor cursor = getApplicationContext().getContentResolver().query(
                        Contract.Quote.URI,
                        Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                        null, null, Contract.Quote.COLUMN_SYMBOL
                );

                if (cursor == null) {
                    Timber.d("cursor is null");
                    return;
                }

                mList.clear();

                while (cursor.moveToNext()) {
                    StockQuoteItem item = new StockQuoteItem();
                    item.symbol = cursor.getString(Contract.Quote.POSITION_SYMBOL);
                    item.absoluteChange = cursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
                    item.percentageChange = cursor.getInt(Contract.Quote.POSITION_PERCENTAGE_CHANGE);
                    item.price = cursor.getFloat(Contract.Quote.POSITION_PRICE);
                    mList.add(item);
                }

                Timber.d("created %d entries", mList.size());

                cursor.close();
            }

            @Override
            public void onDestroy() {

            }

            @Override
            public int getCount() {
                return mList.size();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                Timber.d("getting view at position " + position);
                RemoteViews remoteView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_list_item_quote);
                StockQuoteItem stock = mList.get(position);
                Timber.d("used stock item: " + stock);
                remoteView.setTextViewText(R.id.symbol, stock.symbol);
                remoteView.setTextViewText(R.id.price, FormatUtils.dollarFormat(stock.price));
                remoteView.setInt(
                        R.id.change,
                        "setBackgroundResource",
                        stock.absoluteChange > 0 ? R.drawable.percent_change_pill_green : R.drawable.percent_change_pill_red
                );
                if (PrefUtils.getDisplayMode(getApplicationContext()).equals(getApplicationContext().getString(R.string.pref_display_mode_absolute_key))) {
                    remoteView.setTextViewText(R.id.change, FormatUtils.dollarFormatWithPlus(stock.absoluteChange));
                } else {
                    remoteView.setTextViewText(R.id.change, FormatUtils.percentageFormat(stock.percentageChange / 100));
                }


                final Intent fillIntent = new Intent();
                Uri stockUri = Contract.Quote.makeUriForStock(stock.symbol);
                fillIntent.setData(stockUri);
                remoteView.setOnClickFillInIntent(R.id.widget_list_item, fillIntent);

                return remoteView;
            }

            @Override
            public RemoteViews getLoadingView() {
                return null;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return false;
            }
        };
    }
}