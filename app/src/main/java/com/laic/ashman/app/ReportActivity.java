package com.laic.ashman.app;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.laic.ashman.app.bo.Report;
import com.laic.ashman.app.provider.ReportContentProvider;
import com.laic.ashman.app.provider.ReportTable;
import com.laic.ashman.app.provider.TaskTable;
import com.laic.ashman.app.rest.Message;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by duduba on 14-5-14.
 */
public class ReportActivity extends AbstractAsyncActivity {

    private EditText mLzTxt;
    private EditText mHlbTxt;
    private EditText mGlsTxt;
    private EditText mHlzcjTxt;
    private EditText mFxbTxt;
    private EditText mFzslkbTxt;
    private EditText mSmTxt;
    private EditText mLhTxt;
    private EditText mCpTxt;
    private EditText mDlTxt;
    private EditText mGqTxt;
    private EditText mCbshlTxt;
    private EditText mQzsfTxt;

    private Button mSaveBtn;
    private Button mUploadBtn;

    private Report report = new Report();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report);

        mLzTxt = (EditText) findViewById(R.id.lz);
        mHlbTxt = (EditText) findViewById(R.id.hlb);
        mGlsTxt = (EditText) findViewById(R.id.gls);
        mHlzcjTxt = (EditText) findViewById(R.id.hlzcj);
        mFxbTxt = (EditText) findViewById(R.id.fxb);
        mFzslkbTxt = (EditText) findViewById(R.id.fzslkb);
        mSmTxt = (EditText) findViewById(R.id.sm);
        mLhTxt = (EditText) findViewById(R.id.lh);
        mCpTxt = (EditText) findViewById(R.id.cp);
        mDlTxt = (EditText) findViewById(R.id.dl);
        mGqTxt = (EditText) findViewById(R.id.gq);
        mCbshlTxt = (EditText) findViewById(R.id.cbshl);
        mQzsfTxt = (EditText) findViewById(R.id.qzsf);

        mSaveBtn = (Button) findViewById(R.id.report_save);
        mUploadBtn = (Button) findViewById(R.id.report_upload);

        Intent intent = getIntent();
        report.setTaskId(intent.getStringExtra(TaskTable.EXT_TASK_ID));
        boolean blnCreate = intent.getBooleanExtra("CREATE", false);
        if(!blnCreate) {
            fillData();
        } else {
            mUploadBtn.setEnabled(false);
        }

    }

    public void onSaveClick(View view) {

        report.setLz(Integer.valueOf(mLzTxt.getText().toString()));
        report.setHlb(Integer.valueOf(mHlbTxt.getText().toString()));
        report.setGls(Integer.valueOf(mGlsTxt.getText().toString()));
        report.setHlzcj(Integer.valueOf(mHlzcjTxt.getText().toString()));
        report.setFxb(Integer.valueOf(mFxbTxt.getText().toString()));
        report.setFzslkb(Integer.valueOf(mFzslkbTxt.getText().toString()));
        report.setSm(Integer.valueOf(mSmTxt.getText().toString()));
        report.setLh(Integer.valueOf(mLhTxt.getText().toString()));
        report.setCp(Integer.valueOf(mCpTxt.getText().toString()));
        report.setDl(Integer.valueOf(mDlTxt.getText().toString()));
        report.setGq(Integer.valueOf(mGqTxt.getText().toString()));
        report.setCbshl(Integer.valueOf(mCbshlTxt.getText().toString()));
        report.setQzsf(Integer.valueOf(mQzsfTxt.getText().toString()));

        ContentValues values = new ContentValues();

        values.put(ReportTable.COL_LZ, report.getLz());
        values.put(ReportTable.COL_HLB, report.getHlb());
        values.put(ReportTable.COL_GLS, report.getGls());
        values.put(ReportTable.COL_HLZCJ, report.getHlzcj());
        values.put(ReportTable.COL_FXB, report.getFxb());
        values.put(ReportTable.COL_FZSLKB, report.getFzslkb());
        values.put(ReportTable.COL_SM, report.getSm());
        values.put(ReportTable.COL_LH, report.getLh());
        values.put(ReportTable.COL_CP, report.getCp());
        values.put(ReportTable.COL_DL, report.getDl());
        values.put(ReportTable.COL_GQ, report.getGq());
        values.put(ReportTable.COL_CBSHL, report.getCbshl());
        values.put(ReportTable.COL_QZSF, report.getQzsf());
        values.put(ReportTable.COL_TASKID, report.getTaskId());

        getContentResolver().insert(ReportContentProvider.CONTENT_URI, values);

        Toast.makeText(getApplicationContext(), "报告已经保存", Toast.LENGTH_SHORT).show();

        view.setEnabled(false);
        mUploadBtn.setEnabled(true);

    }

    public void onUploadClick(View view) {
        new FetchResourceTask().execute();
    }

    private void fillData() {

        final Cursor cursor = getContentResolver().query(
                ReportContentProvider.CONTENT_URI,
                ReportTable.COLUMNS,
                ReportTable.COL_TASKID + "='" + report.getTaskId() + "'",
                null,
                null);

        if (cursor != null) {
            if(cursor.moveToFirst()) {
                report.setLz(cursor.getShort(cursor.getColumnIndexOrThrow(ReportTable.COL_LZ)));
                report.setHlb(cursor.getShort(cursor.getColumnIndexOrThrow(ReportTable.COL_HLB)));
                report.setGls(cursor.getShort(cursor.getColumnIndexOrThrow(ReportTable.COL_GLS)));
                report.setHlzcj(cursor.getShort(cursor.getColumnIndexOrThrow(ReportTable.COL_HLZCJ)));
                report.setFxb(cursor.getShort(cursor.getColumnIndexOrThrow(ReportTable.COL_FXB)));
                report.setFzslkb(cursor.getShort(cursor.getColumnIndexOrThrow(ReportTable.COL_FZSLKB)));
                report.setSm(cursor.getShort(cursor.getColumnIndexOrThrow(ReportTable.COL_SM)));
                report.setLh(cursor.getShort(cursor.getColumnIndexOrThrow(ReportTable.COL_LH)));
                report.setCp(cursor.getShort(cursor.getColumnIndexOrThrow(ReportTable.COL_CP)));
                report.setDl(cursor.getShort(cursor.getColumnIndexOrThrow(ReportTable.COL_DL)));
                report.setGq(cursor.getShort(cursor.getColumnIndexOrThrow(ReportTable.COL_GQ)));
                report.setCbshl(cursor.getShort(cursor.getColumnIndexOrThrow(ReportTable.COL_CBSHL)));
                report.setQzsf(cursor.getShort(cursor.getColumnIndexOrThrow(ReportTable.COL_QZSF)));
                report.setUpflag(cursor.getShort(cursor.getColumnIndexOrThrow(ReportTable.COL_UPFLAG)));

                mLzTxt.setText(String.valueOf(report.getLz()));
                mHlbTxt.setText(String.valueOf(report.getHlb()));
                mGlsTxt.setText(String.valueOf(report.getGls()));
                mHlzcjTxt.setText(String.valueOf(report.getHlzcj()));
                mFxbTxt.setText(String.valueOf(report.getFxb()));
                mFzslkbTxt.setText(String.valueOf(report.getFzslkb()));
                mSmTxt.setText(String.valueOf(report.getSm()));
                mLhTxt.setText(String.valueOf(report.getLh()));
                mCpTxt.setText(String.valueOf(report.getCp()));
                mDlTxt.setText(String.valueOf(report.getDl()));
                mGqTxt.setText(String.valueOf(report.getGq()));
                mCbshlTxt.setText(String.valueOf(report.getCbshl()));
                mQzsfTxt.setText(String.valueOf(report.getQzsf()));

                mSaveBtn.setEnabled(false);

                if(report.getUpflag() != 0) {  // 已经上传
                    mUploadBtn.setEnabled(false);
                }
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("提示")
                        .setMessage("该救援任务无处理报告")
                        .setNegativeButton("返回", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                cursor.close();
                                finish();
                            }
                        })
                        .setPositiveButton("新建", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mUploadBtn.setEnabled(false);
                            }
                        })
                        .create()
                        .show();
            }
            // always close the cursor
            cursor.close();
        }

    }

    private class FetchResourceTask extends AsyncTask<Void, Void, Message> {

        @Override
        protected void onPreExecute() {
            showLoadingProgressDialog("上传报告, 请稍等...");
        }

        @Override
        protected Message doInBackground(Void... params) {
            Map paras = new HashMap<String, Object>();
            paras.put("taskId", report.getTaskId());
            paras.put(ReportTable.COL_CBSHL, report.getCbshl());
            paras.put(ReportTable.COL_FZSLKB, report.getFzslkb());
            paras.put(ReportTable.COL_FXB, report.getFxb());
            paras.put(ReportTable.COL_CP, report.getCp());
            paras.put(ReportTable.COL_DL, report.getDl());
            paras.put(ReportTable.COL_GLS, report.getGls());
            paras.put(ReportTable.COL_GQ, report.getGq());
            paras.put(ReportTable.COL_HLB, report.getHlb());
            paras.put(ReportTable.COL_HLZCJ, report.getHlzcj());
            paras.put(ReportTable.COL_LH, report.getLh());
            paras.put(ReportTable.COL_LZ, report.getLz());
            paras.put(ReportTable.COL_SM, report.getSm());
            paras.put(ReportTable.COL_QZSF, report.getQzsf());

            URI targetUrl = getApplicationContext().createGetUrl(Message.ACT_REPORT, paras);

            try {
                Message response = getApplicationContext().getRestTemplate().getForObject(targetUrl, Message.class);
                return response;
            } catch (RestClientException e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
                return new Message(Message.NETERR, getString(R.string.com_network_err));
            }
        }

        @Override
        protected void onPostExecute(Message result) {
            dismissProgressDialog();
            displayResponse(result);
        }
    }
    private void displayResponse(Message response) {
        if (!response.isOk()) {
            Toast.makeText(this, response.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "成功上传", Toast.LENGTH_SHORT).show();

            mUploadBtn.setEnabled(false);

            report.setUpflag(1);

            ContentValues values = new ContentValues();
            values.put(ReportTable.COL_UPFLAG, report.getUpflag());
            getContentResolver().update(
                    ReportContentProvider.CONTENT_URI,
                    values,
                    ReportTable.COL_TASKID + "='" + report.getTaskId() + "'",
                    null);
        }
    }
}