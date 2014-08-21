package com.laic.ashman.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.support.v4.app.FragmentActivity;

/**
 * Created by duduba on 14-6-11.
 */
public abstract class AbstractAsyncFragmentActivity extends FragmentActivity implements AsyncActivity {

    protected static final String TAG = AbstractAsyncFragmentActivity.class.getSimpleName();

    private ProgressDialog progressDialog;

    private boolean destroyed = false;

    // ***************************************
    // Activity methods
    // ***************************************
    @Override
    public MainApplication getApplicationContext() {
        return (MainApplication) super.getApplicationContext();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.destroyed = true;
    }

    // ***************************************
    // Public methods
    // ***************************************
    public void showLoadingProgressDialog(String message) {
        this.showProgressDialog(message);
    }

    public void showProgressDialog(CharSequence message) {
        if (this.progressDialog == null) {
            this.progressDialog = new ProgressDialog(this);
            this.progressDialog.setIndeterminate(true);
        }

        this.progressDialog.setMessage(message);
        this.progressDialog.show();
    }

    public void dismissProgressDialog() {
        if (this.progressDialog != null && !this.destroyed) {
            this.progressDialog.dismiss();
        }
    }

}
