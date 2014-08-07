package com.laic.ashman.app.provider;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by duduba on 14-5-6.
 */
public class ReportTable {

    public static final String TABLE_NAME = "report";

    public static final String COL_ID = "_id";
    public static final String COL_TASKID = "taskid";
    public static final String COL_LZ = "lz";
    public static final String COL_HLB = "hlb";
    public static final String COL_GLS = "gls";
    public static final String COL_HLZCJ = "hlzcj";
    public static final String COL_FXB = "fxb";
    public static final String COL_FZSLKB = "fzslkb";
    public static final String COL_SM = "sm";
    public static final String COL_LH = "lh";
    public static final String COL_CP = "cp";
    public static final String COL_DL = "dl";
    public static final String COL_GQ = "gq";
    public static final String COL_CBSHL = "cbshl";
    public static final String COL_UPFLAG = "upflag";

    public static final String[] COLUMNS = {
            COL_TASKID,
            COL_LZ,
            COL_HLB,
            COL_GLS,
            COL_HLZCJ,
            COL_FXB,
            COL_FZSLKB,
            COL_SM,
            COL_LH,
            COL_CP,
            COL_DL,
            COL_GQ,
            COL_CBSHL,
            COL_UPFLAG,
            COL_ID
    };

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COL_ID + " integer primary key autoincrement," +
                    COL_TASKID + " text," +
                    COL_LZ + " smallint DEFAULT 0," +
                    COL_HLB + " smallint DEFAULT 0," +
                    COL_GLS + " smallint DEFAULT 0," +
                    COL_HLZCJ + " smallint DEFAULT 0," +
                    COL_FXB + " smallint DEFAULT 0," +
                    COL_FZSLKB + " smallint DEFAULT 0," +
                    COL_SM + " smallint DEFAULT 0," +
                    COL_LH + " smallint DEFAULT 0," +
                    COL_CP + " smallint DEFAULT 0," +
                    COL_DL + " smallint DEFAULT 0," +
                    COL_GQ + " smallint DEFAULT 0," +
                    COL_CBSHL + " smallint DEFAULT 0," +
                    COL_UPFLAG + " smallint DEFAULT 0);";

    private static final String SQL_DELETE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(SQL_CREATE_TABLE);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(ReportTable.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        database.execSQL(SQL_DELETE_TABLE);
        onCreate(database);
    }

}
