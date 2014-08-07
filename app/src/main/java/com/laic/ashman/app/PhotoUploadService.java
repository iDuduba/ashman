package com.laic.ashman.app;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import com.laic.ashman.app.provider.TaskTable;
import com.laic.ashman.app.rest.Message;
import de.greenrobot.event.EventBus;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.web.client.RestClientException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class PhotoUploadService extends IntentService {
    final static String DEBUG_TAG = "PhotoUploadService";


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
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionUpload(Context context, String file, String taskId) {
        Intent intent = new Intent(context, PhotoUploadService.class);
        intent.setAction(ACTION_PHOTO_UPLOAD);
        intent.putExtra(EXTRA_FILE, file);
        intent.putExtra(EXTRA_TASKID, taskId);
        context.startService(intent);
    }

    public PhotoUploadService() {
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
                final String file = intent.getStringExtra(EXTRA_FILE);
                final String taskId = intent.getStringExtra(EXTRA_TASKID);
                handleActionUpload(file, taskId);
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
    private void handleActionUpload(String file, String taskId) {
        // TODO: Handle action Baz
//        throw new UnsupportedOperationException("Not yet implemented");
        FTPClient mFTPClient = new FTPClient();
        mFTPClient.setConnectTimeout(30 * 1000);

        try {
            boolean status;

            mFTPClient.connect(getString(R.string.ftp_host));

            status = mFTPClient.login(getString(R.string.ftp_user), getString(R.string.ftp_password));
            if(status == false) {
                notifyUploadProgress(UPLOAD_ACTION_ERROR, "登录FTP服务器失败");
                return;
            }

            /*
            During file transfers, the data connection is busy, but the control connection is idle.
            FTP servers know that the control connection is in use, so won't close it through lack of activity,
            but it's a lot harder for network routers to know that the control and data connections are associated
            with each other. Some routers may treat the control connection as idle, and disconnect it if the
            transfer over the data connection takes longer than the allowable idle time for the router.
            One solution to this is to send a safe command (i.e. NOOP) over the control connection to reset
            the router's idle timer. This is enabled as follows:
            */
            mFTPClient.setControlKeepAliveTimeout(300); // set timeout to 5 minutes
            // This will cause the file upload/download methods to send a NOOP approximately every 5 minutes.

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            boolean isPassiveMode = settings.getBoolean(getString(R.string.setting_ftp_upload_mode), false);

            // 为什么加了这句不行啊？？？
            if(isPassiveMode) {
                mFTPClient.enterLocalPassiveMode();
            }

            // now check the reply code, if positive mean connection success
//                if (FTPReply.isPositiveCompletion(mFTPClient.getReplyCode())) {

            mFTPClient.setFileType(FTP.BINARY_FILE_TYPE);

            File uFile = new File(file);
            final long fileSize = uFile.length();

//            Log.d(DEBUG_TAG, "1 -> " + status + " : " + fileSize);
            FileInputStream fis = new FileInputStream(uFile);
            CountingInputStream cis = new CountingInputStream(fis) {
                private double lastUpdate = 0.0;

                @Override
                protected synchronized void afterRead(int n) {
                    super.afterRead(n);

                    double curPercent = getCount() * 100.0 / fileSize;
//                    Log.d(DEBUG_TAG, "==> " + percent);
                    if(curPercent - lastUpdate > 5) {
                        Log.d(DEBUG_TAG, "Finished " + curPercent + "%");
                        lastUpdate = curPercent;
                        notifyUploadProgress(UPLOAD_ACTION_PERCENT, String.valueOf((int)curPercent));
                    }
                }
            };

            notifyUploadProgress(UPLOAD_ACTION_START, uFile.getName());

//            Log.d(DEBUG_TAG, "2 -> " + uFile.getName());
            status = mFTPClient.storeFile(uFile.getName(), cis);
            cis.close();
//            Log.d(DEBUG_TAG, "3 -> " + status);

//            status = mFTPClient.completePendingCommand();
//                }


            if(status == false) {
                notifyUploadProgress(UPLOAD_ACTION_ERROR, "上传失败");
            } else {
                Map paras = new HashMap<String, Object>();
                paras.put(TaskTable.COL_TASKID, taskId);
                paras.put("imageName", file);

                URI targetUrl = ((MainApplication) MainApplication.getContext()).createGetUrl(Message.ACT_PHOTO, paras);
                Message response = ((MainApplication) MainApplication.getContext()).getRestTemplate().getForObject(targetUrl, Message.class);

                if (response.isOk()) {
                    notifyUploadProgress(UPLOAD_ACTION_FINISH, uFile.getName());
                }
            }
        } catch(RestClientException e) {
            Log.e(DEBUG_TAG, e.getLocalizedMessage(), e);
            notifyUploadProgress(UPLOAD_ACTION_ERROR, e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(DEBUG_TAG, e.getLocalizedMessage(), e);
            notifyUploadProgress(UPLOAD_ACTION_ERROR, e.getLocalizedMessage());
        } finally {
            try {
                if (mFTPClient.isConnected()) {
                    mFTPClient.logout();
                    mFTPClient.disconnect();
                }
            } catch (IOException ex) {
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
