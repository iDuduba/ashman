package com.laic.ashman.app.provider;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by duduba on 14-5-6.
 */
public class PhotoTable {

    public static final String TABLE_NAME = "photo";

    public static final String COL_ID = "_id";
    public static final String COL_TASKID = "taskid";
    public static final String COL_NAME = "name";
    public static final String COL_UPFLAG = "upflag";

    public static final String[] COLUMNS = {
            COL_TASKID,
            COL_NAME,
            COL_UPFLAG,
            COL_ID
    };

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COL_ID + " integer primary key autoincrement," +
                    COL_TASKID + " text," +
                    COL_NAME + " text," +
                    COL_UPFLAG + " smallint DEFAULT 0);";

    private static final String SQL_DELETE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(SQL_CREATE_TABLE);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(PhotoTable.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        database.execSQL(SQL_DELETE_TABLE);
        onCreate(database);
    }

}
