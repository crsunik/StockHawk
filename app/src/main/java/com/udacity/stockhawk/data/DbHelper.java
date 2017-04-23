package com.udacity.stockhawk.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.udacity.stockhawk.data.Contract.Quote;


class DbHelper extends SQLiteOpenHelper {

    private static final String NAME = "StockHawk.db";
    private static final int VERSION = 1;

    private static final String INIT_DB_SQL = "CREATE TABLE " + Quote.TABLE_NAME + " ("
            + Quote._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + Quote.COLUMN_SYMBOL + " TEXT NOT NULL, "
            + Quote.COLUMN_PRICE + " REAL NOT NULL, "
            + Quote.COLUMN_ABSOLUTE_CHANGE + " REAL NOT NULL, "
            + Quote.COLUMN_PERCENTAGE_CHANGE + " REAL NOT NULL, "
            + Quote.COLUMN_HISTORY + " TEXT NOT NULL, "
            + "UNIQUE (" + Quote.COLUMN_SYMBOL + ") ON CONFLICT REPLACE);";


    DbHelper(Context context) {
        super(context, NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(INIT_DB_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
