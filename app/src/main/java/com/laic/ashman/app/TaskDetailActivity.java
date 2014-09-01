package com.laic.ashman.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import com.laic.ashman.app.provider.TaskContentProvider;
import com.laic.ashman.app.provider.TaskTable;
import de.greenrobot.event.EventBus;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by duduba on 14-5-6.
 */
public class TaskDetailActivity extends Activity {

    final static String TAG = "TaskDetailActivity";

    private String id;
    private TextView mTaskidTxt;
    private EditText mTaskztTxt;
    private TextView mSjjssjTxt;
    private TextView mKssjTxt;
    private TextView mDxcsjTxt;
    private TextView mJssjTxt;
    private TextView mEventidTxt;
    private TextView mSjmsTxt;
    private TextView mSjlxTxt;
    private TextView mJjsjTxt;
    private TextView mDsrdhTxt;
    private TextView mSjcphTxt;
    private TextView mSjfxTxt;
    private TextView mSjzhTxt;
    private TextView mPointxTxt;
    private TextView mPointyTxt;
    private TextView mCqclTxt;
    private TextView mCqryTxt;
    private TextView mCqrydhTxt;
    private TextView mBzTxt;
    private Button mUpdateBtn;

    private Uri taskUri;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail);

        mTaskidTxt = (TextView) findViewById(R.id.taskid);
        mTaskztTxt = (EditText) findViewById(R.id.taskzt);
        mSjjssjTxt = (TextView) findViewById(R.id.sjjssj);
        mKssjTxt = (TextView) findViewById(R.id.kssj);
        mDxcsjTxt = (TextView) findViewById(R.id.dxcsj);
        mJssjTxt = (TextView) findViewById(R.id.jssj);
        mEventidTxt = (TextView) findViewById(R.id.eventid);
        mSjmsTxt = (TextView) findViewById(R.id.sjms);
        mSjlxTxt = (TextView) findViewById(R.id.sjlx);
        mJjsjTxt = (TextView) findViewById(R.id.jjsj);
        mDsrdhTxt = (TextView) findViewById(R.id.dsrdh);
        mSjcphTxt = (TextView) findViewById(R.id.sjcph);
        mSjfxTxt = (TextView) findViewById(R.id.sjfx);
        mSjzhTxt = (TextView) findViewById(R.id.sjzh);
        mPointxTxt = (TextView) findViewById(R.id.pointx);
        mPointyTxt = (TextView) findViewById(R.id.pointy);
        mCqclTxt = (TextView) findViewById(R.id.cqcl);
        mCqryTxt = (TextView) findViewById(R.id.cqry);
        mCqrydhTxt = (TextView) findViewById(R.id.cqrydh);
        mBzTxt = (TextView) findViewById(R.id.bz);
        mUpdateBtn = (Button) findViewById(R.id.task_update);

        Bundle extras = getIntent().getExtras();

        // check from the saved Instance
        taskUri = (savedInstanceState == null) ? null : (Uri) savedInstanceState
                .getParcelable(TaskContentProvider.CONTENT_ITEM_TYPE);

        // Or passed from the other activity
        if (extras != null) {
            taskUri = extras.getParcelable(TaskContentProvider.CONTENT_ITEM_TYPE);

            fillData(taskUri);
        }

        // add PhoneStateListener
        PhoneCallListener phoneListener = new PhoneCallListener();
        TelephonyManager telephonyManager = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneListener,PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDebugMode = settings.getBoolean(getString(R.string.setting_debug_mode), false);

        if(isDebugMode) {
            mTaskztTxt.setEnabled(true);
            mTaskztTxt.setFocusable(true);
            mUpdateBtn.setVisibility(View.VISIBLE);
        } else {
            mTaskztTxt.setEnabled(false);
            mTaskztTxt.setFocusable(false);
            mUpdateBtn.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
        outState.putParcelable(TaskContentProvider.CONTENT_ITEM_TYPE, taskUri);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }

    public void onSaveClick(View view) {
        if (TextUtils.isEmpty(mTaskidTxt.getText().toString())) {
            makeToast();
        } else {
            setResult(RESULT_OK);
            finish();
        }
    }

    public void onRunClick(View view) {
        Map event = new HashMap<String, String>();
        event.put("type", "task");
        event.put("id", id);
        EventBus.getDefault().post(event);

        goBackMainScreen();
    }

    private void goBackMainScreen() {
        Intent startMain = new Intent(getApplicationContext(), MainActivity.class);
        startMain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(startMain);
    }

    private void saveState() {
//        String longitude = mPointxTxt.getText().toString();
//        String latitude = mPointyTxt.getText().toString();
        String status = mTaskztTxt.getText().toString();

        ContentValues values = new ContentValues();

        values.put(TaskTable.COL_ID, id);
//        values.put(TaskTable.COL_POINTX, longitude);
//        values.put(TaskTable.COL_POINTY, latitude);
        values.put(TaskTable.COL_TASKZT, status);

        getContentResolver().update(taskUri, values, null, null);

//        if (taskUri == null) {
//            // New todo
//            taskUri = getContentResolver().insert(TaskContentProvider.CONTENT_URI, values);
//        } else {
//            // Update todo
//            getContentResolver().update(taskUri, values, null, null);
//        }
    }

    private void fillData(Uri uri) {
        String[] projection = TaskTable.COLUMNS;
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                id = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_ID));

                mTaskidTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_TASKID)));
                mTaskztTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_TASKZT)));
                mSjjssjTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_SJJSSJ)));
                mKssjTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_KSSJ)));
                mDxcsjTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_DXCSJ)));
                mJssjTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_JSSJ)));
                mEventidTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_EVENTID)));
                mSjmsTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_SJMS)));
                mSjlxTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_SJLX)));
                mJjsjTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_JJSJ)));
                mDsrdhTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_DSRDH)));
                mSjcphTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_SJCPH)));
                mSjfxTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_SJFX)));
                mSjzhTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_SJZH)));
                mPointxTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_POINTX)));
                mPointyTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_POINTY)));
                mCqclTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_CQCL)));
                mCqryTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_CQRY)));
                mCqrydhTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_CQRYDH)));
                mBzTxt.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_BZ)));
            }
            // always close the cursor
            cursor.close();
        }
    }

    private void makeToast() {
        Toast.makeText(TaskDetailActivity.this, "Please maintain name of task",Toast.LENGTH_LONG).show();
    }


    public void onPhoneNumberClick(View v) {
        String tel = ((TextView)v).getText().toString();
        if(tel.length() > 0) {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + tel));
            startActivity(callIntent);
        }

    }

    //monitor phone call activities
    private class PhoneCallListener extends PhoneStateListener {
        private boolean isPhoneCalling = false;
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (TelephonyManager.CALL_STATE_RINGING == state) {
                // phone ringing state
            }
            if (TelephonyManager.CALL_STATE_OFFHOOK == state) {
                // active
                isPhoneCalling = true;
            }
            if (TelephonyManager.CALL_STATE_IDLE == state) {
                // run when class initial and phone call ended,
                // need detect flag from CALL_STATE_OFFHOOK
                if (isPhoneCalling) {
                    // restart app
                    Intent i = getBaseContext().getPackageManager()
                            .getLaunchIntentForPackage(
                                    getBaseContext().getPackageName());
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    isPhoneCalling = false;
                }
            }
        }
    }

}