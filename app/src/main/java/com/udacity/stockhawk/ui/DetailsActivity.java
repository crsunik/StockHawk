package com.udacity.stockhawk.ui;

import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.udacity.stockhawk.utils.FormatUtils.dollarFormat;
import static com.udacity.stockhawk.utils.FormatUtils.dollarFormatWithPlus;
import static com.udacity.stockhawk.utils.FormatUtils.percentageFormat;

public class DetailsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int STOCK_DETAILS_LOADER = 1;
    private static final String TAG = DetailsActivity.class.getName();
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.symbol)
    TextView symbol;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.price)
    TextView price;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.change)
    TextView change;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.stock_chart)
    LineChart chart;

    private Uri mStockContentUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        ButterKnife.bind(this);
        supportPostponeEnterTransition();
        mStockContentUri = getIntent().getData();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        getSupportLoaderManager().initLoader(STOCK_DETAILS_LOADER, null, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                mStockContentUri,
                Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                null, null, null);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst())
            load(data);
        supportStartPostponedEnterTransition();
    }

    private void load(Cursor data) {
        symbol.setText(data.getString(Contract.Quote.POSITION_SYMBOL));
        price.setText(dollarFormat(data.getFloat(Contract.Quote.POSITION_PRICE)));
        float rawAbsoluteChange = data.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
        float percentageChange = data.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

        if (rawAbsoluteChange > 0)
            change.setBackgroundResource(R.drawable.percent_change_pill_green);
        else
            change.setBackgroundResource(R.drawable.percent_change_pill_red);

        if (PrefUtils.getDisplayMode(this).equals(getString(R.string.pref_display_mode_absolute_key)))
            change.setText(dollarFormatWithPlus(rawAbsoluteChange));
        else
            change.setText(percentageFormat(percentageChange / 100));

        loadHistory(data.getString(Contract.Quote.POSITION_SYMBOL), data.getString(Contract.Quote.POSITION_HISTORY));
    }

    void loadHistory(String stockSymbol, String history) {
        List<Entry> entries = new ArrayList<>();
        List<String> rev = Arrays.asList(history.split("\n"));
        Collections.reverse(rev);
        for (String datedStock : rev) {
            String[] stockData = datedStock.split(",");
            entries.add(new Entry(Float.parseFloat(stockData[0].trim()), Float.parseFloat(stockData[1].trim())));
        }

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.label_stock_prices));
        chart.getXAxis().setTextColor(Color.WHITE);
        chart.getXAxis().setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                Long date = (long) value;
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(date);
                return (new SimpleDateFormat(getString(R.string.util_date_format), Locale.getDefault())).format(cal.getTime());
            }
        });
        chart.getAxisLeft().setTextColor(Color.WHITE);
        chart.getAxisRight().setTextColor(Color.WHITE);
        chart.getLegend().setTextColor(Color.WHITE);
        chart.getDescription().setEnabled(false);
        chart.setContentDescription(getString(R.string.stock_chart_description, stockSymbol));
        chart.setData(new LineData(dataSet));
        chart.invalidate();
    }
}
