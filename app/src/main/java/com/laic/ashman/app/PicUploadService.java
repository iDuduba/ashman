package com.laic.ashman.app;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import com.laic.ashman.app.provider.*;
import com.laic.ashman.app.rest.Message;
import de.greenrobot.event.EventBus;
import it.sauronsoftware.ftp4j.*;
import org.springframework.web.client.RestClientException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * An {@link android.app.IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class PicUploadService extends IntentService {
    final static String DEBUG_TAG = "PicUploadService";


    public static final String UPLOAD_ACTION_START = "upload.start";
    public static final String UPLOAD_ACTION_PERCENT = "upload.percent";
    public static final String UPLOAD_ACTION_FINISH = "upload.finish";
    public static final String UPLOAD_ACTION_ERROR = "upload.error";


    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_PHOTO_UPLOAD = "com.laic.ashman.app.action.UPLOAD";

    // TODO: Rename parameters
    private static final String EXTRA_FILE = "com.laic.ashman.app.extra.FILE";
    private static final String EXTRA_TASKID = "com.laic.ashman.app.extra.TASKID";

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see android.app.IntentService
     */
    // TODO: Customize helper method
    public static void startActionUpload(Context context, String file, String taskId) {
        Intent intent = new Intent(context, PicUploadService.class);
        intent.setAction(ACTION_PHOTO_UPLOAD);
        intent.putExtra(EXTRA_FILE, file);
        intent.putExtra(EXTRA_TASKID, taskId);
        context.startService(intent);
    }

    public PicUploadService() {
        super("PhotoUploadService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        Log.d(DEBUG_TAG, "onStartCommand:" + Thread.currentThread().getId() + " -> " + intent.getStringExtra(EXTRA_FILE));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
//        Log.d(DEBUG_TAG, "onHandleIntent:" + Thread.currentThread().getId() + " -> " + intent.getStringExtra(EXTRA_FILE));
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PHOTO_UPLOAD.equals(action)) {
                final String photoFile = intent.getStringExtra(EXTRA_FILE);
                final String taskId = intent.getStringExtra(EXTRA_TASKID);
                handleActionUpload(photoFile, taskId);
//                doit();
            }
        }
    }

    private void doit() {
        notifyUploadProgress(UPLOAD_ACTION_START, "abc.jpg");

        long fileSize = 1000;
        for(int i = 1; i <= 100; i++) {

            double percent = i * 10.0 / fileSize * 100.0;
            Log.d(DEBUG_TAG, "==> " + percent);

            double lastUpdate = 0.0;
            if(percent - lastUpdate > 5) {
                Log.d(DEBUG_TAG, "Finished " + percent + "%");
                lastUpdate = percent;
                notifyUploadProgress(UPLOAD_ACTION_PERCENT, String.valueOf((int)percent));
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        notifyUploadProgress(UPLOAD_ACTION_FINISH, "abc.jpg");
    }
    /**
     * Handle action upload in the provided background thread with the provided
     * parameters.
     */
    private void handleActionUpload(String photoFile, String taskId) {
        // TODO: Handle action Baz
//        throw new UnsupportedOperationException("Not yet implemented");
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isPassiveMode = settings.getBoolean(getString(R.string.setting_ftp_upload_mode), true);

        FTPClient client = new FTPClient();

        try {
            client.connect(getString(R.string.ftp_host));
            client.login(getString(R.string.ftp_user), getString(R.string.ftp_password));

            if(!isPassiveMode) {
//                Log.d(DEBUG_TAG, "Using active mode.");
                client.setPassive(false);
            }

            client.setType(FTPClient.TYPE_BINARY);
            client.setAutoNoopTimeout(30000);

            final File uFile = new File(photoFile);
            final long fileSize = uFile.length();

            client.upload(uFile, new FTPDataTransferListener() {
                private double lastUpdate = 0.0;
                private int xxx = 0;

                @Override
                public void started() {
                    notifyUploadProgress(UPLOAD_ACTION_START, uFile.getName());
                }

                @Override
                public void transferred(int count) {
                    xxx += count;
                    double curPercent = xxx * 100.0 / fileSize;
//                    Log.d(DEBUG_TAG, xxx + " ==> " + curPercent);
                    if(curPercent - lastUpdate > 5) {
//                        Log.d(DEBUG_TAG, "Finished " + curPercent + "%");
                        lastUpdate = curPercent;
                        notifyUploadProgress(UPLOAD_ACTION_PERCENT, String.valueOf((int)curPercent));
                    }
                }

                @Override
                public void completed() {

                }

                @Override
                public void aborted() {
                }

                @Override
                public void failed() {
                    notifyUploadProgress(UPLOAD_ACTION_ERROR, "上传失败");
                }
            });

            Map paras = new HashMap<String, Object>();
            paras.put(TaskTable.COL_TASKID, taskId);
            paras.put("imageName", uFile.getName());

            URI targetUrl = ((MainApplication) MainApplication.getContext()).createGetUrl(Message.ACT_PHOTO, paras);
            Message response = ((MainApplication) MainApplication.getContext()).getRestTemplate().getForObject(targetUrl, Message.class);

            if (response.isOk()) {
                notifyUploadProgress(UPLOAD_ACTION_FINISH, uFile.getName());

                ContentValues values = new ContentValues();
                values.put(PhotoTable.COL_UPFLAG, 1);
                getContentResolver().update(
                        PhotoContentProvider.CONTENT_URI,
                        values,
                        PhotoTable.COL_NAME + "='" + photoFile + "'",
                        null);

            }
        } catch (FTPIllegalReplyException e) {
            Log.e(DEBUG_TAG, e.getLocalizedMessage(), e);
            notifyUploadProgress(UPLOAD_ACTION_ERROR, e.getLocalizedMessage());
        } catch (FTPException e) {
            Log.e(DEBUG_TAG, e.getLocalizedMessage(), e);
            notifyUploadProgress(UPLOAD_ACTION_ERROR, e.getLocalizedMessage());
        } catch (FTPDataTransferException e) {
            Log.e(DEBUG_TAG, e.getLocalizedMessage(), e);
            notifyUploadProgress(UPLOAD_ACTION_ERROR, e.getLocalizedMessage());
        } catch (FTPAbortedException e) {
            Log.e(DEBUG_TAG, e.getLocalizedMessage(), e);
            notifyUploadProgress(UPLOAD_ACTION_ERROR, e.getLocalizedMessage());
        } catch(RestClientException e) {
            Log.e(DEBUG_TAG, e.getLocalizedMessage(), e);
            notifyUploadProgress(UPLOAD_ACTION_ERROR, e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(DEBUG_TAG, e.getLocalizedMessage(), e);
            notifyUploadProgress(UPLOAD_ACTION_ERROR, e.getLocalizedMessage());
        } finally {
            try {
                if (client.isConnected()) {
                    client.disconnect(true);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    private void notifyUploadProgress(String action, String value) {
        Map event = new HashMap<String, String>();
        event.put("type", "upload");
        event.put("action", action);
        event.put("value", value);

        EventBus.getDefault().post(event);
    }

    // Check Internet Connection!!!
    public boolean isNetworkAvailable() {
        ConnectivityManager connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
