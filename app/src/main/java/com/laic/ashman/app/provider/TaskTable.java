package com.laic.ashman.app.provider;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by duduba on 14-5-6.
 */
public class TaskTable {

    public static final int TASK_NEW = 1;
    public static final int TASK_START = 2;
    public static final int TASK_ARRIVE = 3;
    public static final int TASK_FINISH = 4;
    public static final int TASK_DOVE = 5;  // 被放鸽子:)
    public static final int TASK_RUNNING = 23;  // 被放鸽子:)

    public static final String EXT_TASK_ID = "task_id";
    public static final String EXT_TASK_ROW_ID = "task_row_id";

    public static final String TABLE_NAME = "task";

    public static final String COL_ID = "_id";
    public static final String COL_TASKID = "taskId";
    public static final String COL_TASKZT = "taskZt";
    public static final String COL_SJJSSJ = "SJJSSJ";
    public static final String COL_KSSJ = "kssj";
    public static final String COL_DXCSJ = "dxcsj";
    public static final String COL_JSSJ = "jssj";
    public static final String COL_EVENTID = "eventId";
    public static final String COL_SJMS = "sjms";
    public static final String COL_SJLX = "sjlx";
    public static final String COL_JJSJ = "jjsj";
    public static final String COL_DSRDH = "dsrdh";
    public static final String COL_SJCPH = "sjcph";
    public static final String COL_SJFX = "sjfx";
    public static final String COL_SJZH = "sjzh";
    public static final String COL_POINTX = "pointx";
    public static final String COL_POINTY = "pointy";
    public static final String COL_CQCL = "cqcl";
    public static final String COL_CQRY = "cqry";
    public static final String COL_CQRYDH = "cqrydh";
    public static final String COL_BZ = "bz";

    public static final String[] COLUMNS = {
            COL_TASKID,
            COL_TASKZT,
            COL_SJJSSJ,
            COL_KSSJ,
            COL_DXCSJ,
            COL_JSSJ,
            COL_EVENTID,
            COL_SJMS,
            COL_SJLX,
            COL_JJSJ,
            COL_DSRDH,
            COL_SJCPH,
            COL_SJFX,
            COL_SJZH,
            COL_POINTX,
            COL_POINTY,
            COL_CQCL,
            COL_CQRY,
            COL_CQRYDH,
            COL_BZ,
            COL_ID
    };

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COL_ID + " integer primary key autoincrement," +
                    COL_TASKID + " text," +
                    COL_SJJSSJ + " text," +
                    COL_KSSJ + " text," +
                    COL_DXCSJ + " text," +
                    COL_JSSJ + " text," +
                    COL_EVENTID + " text," +
                    COL_SJMS + " text," +
                    COL_SJLX + " text," +
                    COL_JJSJ + " text," +
                    COL_DSRDH + " text," +
                    COL_SJCPH + " text," +
                    COL_SJFX + " text," +
                    COL_SJZH + " text," +
                    COL_POINTX + " text," +
                    COL_POINTY + " text," +
                    COL_CQCL + " text," +
                    COL_CQRY + " text," +
                    COL_CQRYDH + " text," +
                    COL_BZ + " text," +
                    COL_TASKZT + " smallint);";

    private static final String SQL_DELETE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(SQL_CREATE_TABLE);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(TaskTable.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        database.execSQL(SQL_DELETE_TABLE);
        onCreate(database);
    }

}
