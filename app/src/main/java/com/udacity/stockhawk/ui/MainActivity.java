package com.udacity.stockhawk.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.QuoteSyncJob;
import com.udacity.stockhawk.utils.StockQuoteValidationTask;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.udacity.stockhawk.utils.NetworkUtils.isNotConnected;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        StockAdapter.StockAdapterOnClickHandler {

    private static final int STOCK_LOADER = 0;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.recycler_view)
    RecyclerView stockRecyclerView;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.error)
    TextView error;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.loading_layer)
    View loadingLayer;

    private StockAdapter adapter;

    @Override
    public void onClick(StockAdapter.StockViewHolder stockViewHolder) {
        Intent intent = new Intent(this, DetailsActivity.class).setData(stockViewHolder.contentUri);
        ActivityOptionsCompat activityOptions =
                ActivityOptionsCompat.makeSceneTransitionAnimation(this,
                        new Pair<View, String>(stockViewHolder.itemView, getString(R.string.transition_stock_quote)));
        ActivityCompat.startActivity(this, intent, activityOptions.toBundle());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        adapter = new StockAdapter(this, this);
        stockRecyclerView.setAdapter(adapter);
        stockRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);
        onRefresh();

        QuoteSyncJob.initialize(this);
        getSupportLoaderManager().initLoader(STOCK_LOADER, null, this);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                String symbol = adapter.getSymbolAtPosition(viewHolder.getAdapterPosition());
                PrefUtils.removeStock(MainActivity.this, symbol);
                getContentResolver().delete(Contract.Quote.makeUriForStock(symbol), null, null);
            }
        }).attachToRecyclerView(stockRecyclerView);
        supportPostponeEnterTransition();
    }

    @Override
    public void onRefresh() {
        QuoteSyncJob.syncImmediately(this);

        //If there are some stocks added but not yet loaded - we display view with message and progress bar
        if (adapter.getItemCount() == 0 && PrefUtils.getStocks(this).size() > 0) {
            loadingLayer.setVisibility(View.VISIBLE);
            stockRecyclerView.setVisibility(View.GONE);
        }

        if (isNotConnected(this) && adapter.getItemCount() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_network));
            error.setVisibility(View.VISIBLE);
        } else if (isNotConnected(this)) {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, R.string.toast_no_connectivity, Toast.LENGTH_LONG).show();
        } else if (PrefUtils.getStocks(this).size() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_stocks));
            error.setVisibility(View.VISIBLE);
        } else {
            error.setVisibility(View.GONE);
        }
    }

    void addStock(final String symbol) {
        if (symbol == null || symbol.isEmpty())
            return;

        if (PrefUtils.getStocks(this).contains(symbol)) {
            Toast.makeText(this, getString(R.string.error_duplicated_stock, symbol), Toast.LENGTH_LONG).show();
            return;
        }

        if (isNotConnected(this)) {
            Toast.makeText(MainActivity.this, getString(R.string.toast_stock_no_internet_connection, symbol), Toast.LENGTH_LONG).show();
            return;
        }

        swipeRefreshLayout.setRefreshing(true);

        new StockQuoteValidationTask(new StockQuoteValidationTask.ValidationListener() {
            @Override
            public void onValidationResult(StockQuoteValidationTask.StockQuoteValidationResult result) {
                int messageId = R.string.toast_stock_added;
                switch (result.code) {
                    case StockQuoteValidationTask.STOCK_QUOTE_DATA_NOT_EXIST:
                        messageId = R.string.toast_stock_data_not_exist;
                        swipeRefreshLayout.setRefreshing(false);
                        break;
                    case StockQuoteValidationTask.STOCK_QUOTE_NOT_EXIST:
                        messageId = R.string.toast_stock_not_exist;
                        swipeRefreshLayout.setRefreshing(false);
                        break;
                    case StockQuoteValidationTask.STOCK_QUOTE_OK:
                        PrefUtils.addStock(MainActivity.this, symbol);
                        QuoteSyncJob.syncImmediately(MainActivity.this);
                        break;
                }
                Toast.makeText(MainActivity.this, getString(messageId, symbol), Toast.LENGTH_LONG).show();
            }
        }).execute(new StockQuoteValidationTask.StockQuoteValidationRequest(symbol));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                Contract.Quote.URI,
                Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                null, null, Contract.Quote.COLUMN_SYMBOL);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swipeRefreshLayout.setRefreshing(false);
        adapter.setCursor(data);
        if (data.getCount() == 0)
            supportStartPostponedEnterTransition();
        else {
            error.setVisibility(View.GONE);
            loadingLayer.setVisibility(View.GONE);
            stockRecyclerView.setVisibility(View.VISIBLE);
            stockRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (stockRecyclerView.getChildCount() > 0) {
                        supportStartPostponedEnterTransition();
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swipeRefreshLayout.setRefreshing(false);
        adapter.setCursor(null);
    }

    private void setDisplayModeMenuItemIcon(MenuItem item) {
        if (PrefUtils.getDisplayMode(this)
                .equals(getString(R.string.pref_display_mode_absolute_key))) {
            item.setIcon(R.drawable.ic_percentage);
        } else {
            item.setIcon(R.drawable.ic_dollar);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_settings, menu);
        MenuItem item = menu.findItem(R.id.action_change_units);
        setDisplayModeMenuItemIcon(item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_stock:
                new AddStockDialog().show(getFragmentManager(), "StockDialogFragment");
                return true;
            case R.id.action_change_units:
                PrefUtils.toggleDisplayMode(this);
                setDisplayModeMenuItemIcon(item);
                adapter.notifyDataSetChanged();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
