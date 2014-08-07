package com.laic.ashman.app.provider;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.Date;

/**
 * Created by duduba on 14-5-6.
 */
public class PositionTable {

    public static final String TABLE_NAME = "position";

    public static final String COL_ID = "_id";
    public static final String COL_TASKID = "taskId";
    public static final String COL_TIME = "gatherTime";
    public static final String COL_LONGITUDE = "longitude";
    public static final String COL_LATITUDE = "latitude";
    public static final String COL_UPFLAG = "upFlag";

    public static final String[] COLUMNS = {
            COL_TASKID,
            COL_TIME,
            COL_LONGITUDE,
            COL_LATITUDE,
            COL_UPFLAG,
            COL_ID
    };

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COL_ID + " integer primary key autoincrement," +
                    COL_TASKID + " text," +
                    COL_TIME + " text," +
                    COL_LONGITUDE + " text," +
                    COL_LATITUDE + " text," +
                    COL_UPFLAG + " smallint DEFAULT 0);";

    private static final String SQL_DELETE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(SQL_CREATE_TABLE);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(PositionTable.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        database.execSQL(SQL_DELETE_TABLE);
        onCreate(database);
    }

}
