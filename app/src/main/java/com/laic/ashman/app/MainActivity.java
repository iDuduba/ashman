package com.laic.ashman.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.*;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.esri.android.map.*;
import com.esri.android.map.ags.ArcGISLocalTiledLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.android.map.event.OnZoomListener;
import com.esri.android.runtime.ArcGISRuntime;
import com.esri.core.geometry.*;
import com.esri.core.io.UserCredentials;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.laic.ashman.app.bo.Task;
import com.laic.ashman.app.provider.*;
import com.laic.ashman.app.rest.Message;
import de.greenrobot.event.EventBus;
import org.springframework.web.client.RestClientException;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AbstractAsyncActivity {
    final static String DEBUG_TAG = "MainActivity";

    private ViewGroup taskSection;
    private TextView taskField;
//    private TextView latitudeField;
//    private TextView longitudeField;
    private TextView speedField;

    private TextView distanceField;
    private Chronometer elapseField;

    private Button btnTaskStart;
    private Button btnTaskArrive;
    private Button btnTaskFinish;

    private Button btnCamera;
    private Button btnReport;

    MapView mMapView;
    private LocationDisplayManager mLocService = null;

    private MenuItem mSwitchFollowMode;        // The button used to switch location mode

    private Task currentTask = null;
    private boolean isFollowMode = false;          // Indicates if we are in GPS following mode, or not
//    private int taskStatus = TaskTable.TASK_NEW;

    private GraphicsLayer gLayer;
    // create a line symbol (green, 3 thick and a dash style)
    private SimpleLineSymbol lineSymbol = new SimpleLineSymbol(Color.BLUE, 3, SimpleLineSymbol.STYLE.SOLID);

    // ----- setting ------
    boolean isTrack = true;
    int updateInterval = 1000000;

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(DEBUG_TAG, "onStart");
        checkGpsSettings();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(DEBUG_TAG, "onCreate");

        //设置窗口特征：启用不显示进度的进度条
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        isTrack = settings.getBoolean(getString(R.string.setting_record_track), true);
        if(isTrack) {
            updateInterval = Integer.valueOf(
                    settings.getString(getString(R.string.setting_gps_update_interval), "1000000")) * 1000;
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // ActionBar半透明效果
//        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
//        ActionBar actionBar = getActionBar();
//        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#330000ff")));

        setContentView(R.layout.main);

        ArcGISRuntime.setClientId("MwOHwL7ofpZsYhB5");

        // Retrieve the map and initial extent from XML layout
        mMapView = (MapView)findViewById(R.id.map);
        // enable map to wrap around date line
        mMapView.enableWrapAround(true);

//        initMap();

        taskSection = (ViewGroup) findViewById(R.id.runtime);
        taskSection.setVisibility(View.GONE);

        taskField = (TextView) findViewById(R.id.taskdesc);
//        latitudeField = (TextView) findViewById(R.id.latitude);
//        longitudeField = (TextView) findViewById(R.id.longitude);
        speedField = (TextView) findViewById(R.id.speed);

        distanceField = (TextView) findViewById(R.id.distance);
        elapseField = (Chronometer) findViewById(R.id.elapse);

        btnTaskStart = (Button) findViewById(R.id.btnTaskStart);
        btnTaskArrive = (Button) findViewById(R.id.btnTaskArrive);
        btnTaskFinish = (Button) findViewById(R.id.btnTaskFinish);

        btnCamera = (Button) findViewById(R.id.take_photo);
        btnCamera.setVisibility(View.INVISIBLE);
        btnReport = (Button) findViewById(R.id.take_report);
        btnReport.setVisibility(View.INVISIBLE);

        // 如果include指定了id的话，就不能直接把它里面的控件当成主xml中的控件来直接获得了，
        // 必须先获得这个xml布局文件，再通过布局文件findViewById来获得其子控件。
//        View taskLayout = getLayoutInflater().inflate(R.layout.info, null);

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(DEBUG_TAG, "onResume");

        mMapView.unpause();

        if (mLocService != null && mLocService.isStarted()) {
            mLocService.resume();
        }

        if(currentTask == null) {
            taskSection.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(DEBUG_TAG, "onPause");

        mMapView.pause();

        if (mLocService != null && mLocService.isStarted()) {
            mLocService.pause();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(DEBUG_TAG, "onRestart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(DEBUG_TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(DEBUG_TAG, "onDestroy");

        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        if(currentTask != null && currentTask.isRunning()) {
            editor.putInt("id", currentTask.getId());
            editor.putInt("status", currentTask.getTaskZt());
        } else {
            editor.putInt("id", 0);
        }
        editor.commit();

        mMapView.recycle();
        mMapView = null;

        EventBus.getDefault().unregister(this);
    }

    private void launchTask(int taskRowId, boolean isContinue) {
        currentTask = queryTaskById(taskRowId);
        if(currentTask != null) {
//            taskStatus = currentTask.getTaskZt();
            taskSection.setVisibility(View.VISIBLE);

            switch(currentTask.getTaskZt()) {
                case TaskTable.TASK_NEW:
                    btnTaskStart.setVisibility(View.VISIBLE);
                    btnTaskArrive.setVisibility(View.GONE);
                    btnTaskFinish.setVisibility(View.GONE);
                    break;
                case TaskTable.TASK_START:
                    btnTaskStart.setVisibility(View.GONE);
                    btnTaskArrive.setVisibility(View.VISIBLE);
                    btnTaskFinish.setVisibility(View.GONE);
                    break;
                case TaskTable.TASK_ARRIVE:
                    btnTaskStart.setVisibility(View.GONE);
                    btnTaskArrive.setVisibility(View.GONE);
                    btnTaskFinish.setVisibility(View.VISIBLE);

                    btnCamera.setVisibility(View.VISIBLE);
                    btnReport.setVisibility(View.VISIBLE);
                    break;
            }

            distanceField.setText("0.0");
            speedField.setText("0.0");
            taskField.setText(currentTask.getTaskId());

            gLayer.removeAll();
            markAccidentLocation(currentTask);

            prevLocation = null;
            distance = 0.0f;

            if(isContinue && currentTask.isStarted()) {
                switchOnFollowMode();
                elapseField.start();
            }
        }
    }

    public void onEventMainThread(Map<String, String> event) {
        String type = event.get("type");
        if(type.compareToIgnoreCase("task") == 0) {
            if(currentTask != null && currentTask.isRunning()) {
                Toast.makeText(this, "任务执行中...", Toast.LENGTH_SHORT)
                        .show();
            } else {
                int taskRowId = Integer.parseInt(event.get("id"));
                launchTask(taskRowId, false);
            }
        } else  if(type.compareToIgnoreCase("upload") == 0) {
            String action = event.get("action");
            String value = event.get("value");

            if(action.compareToIgnoreCase(PicUploadService.UPLOAD_ACTION_START) == 0) {
                setProgressBarIndeterminateVisibility(true);
            } else if(action.compareToIgnoreCase(PicUploadService.UPLOAD_ACTION_FINISH) == 0) {
                setProgressBarIndeterminateVisibility(false);
                Toast.makeText(getApplicationContext(),
                        value + " uploaded.",
                        Toast.LENGTH_LONG).show();
            } else if(action.compareToIgnoreCase(PicUploadService.UPLOAD_ACTION_ERROR) == 0) {
                setProgressBarIndeterminateVisibility(false);
                Toast.makeText(getApplicationContext(),
                        value,
                        Toast.LENGTH_SHORT).show();
            } else {
//                int percent = Integer.parseInt(value) * 100;
//                setProgress(percent);
            }
        }

    }

//    public void onEventBackgroundThread(String event) {
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(DEBUG_TAG, "onCreateOptionsMenu");
        // Inflate the menu items for use in the action bar
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);

        mSwitchFollowMode = menu.findItem(R.id.action_follow_switch);
        // Since we will need to set location button only when map is ready,
        // so we need to set the listener only when menuitem is ready
        initMap();

        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        final int taskId = pref.getInt("id", 0);
        if(taskId != 0) {
            new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("继续中断的任务？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            launchTask(taskId, true);
                        }
                    })
                    .create()
                    .show();
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_follow_switch:
                if(isFollowMode)
                    switchOffFollowMode();
                else
                    switchOnFollowMode();

                return true;
            case R.id.action_task_list:
                openTaskList();
                return true;
            case R.id.action_settings:
                openSettings();
                return true;
            case R.id.action_cancel_task:
                cancelTask();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openTaskList() {
        Intent i = new Intent(this, TaskActivity.class);
        startActivity(i);
    }

    private void cancelTask() {
        currentTask = null;

        btnCamera.setVisibility(View.INVISIBLE);
        btnReport.setVisibility(View.INVISIBLE);

        taskSection.setVisibility(View.GONE);
        gLayer.removeAll();
    }


    //////////////////////// Take Photo   ////////////////////////////////

    // Required for camera operations in order to save the image file on resume.
    private String mCurrentPhotoPath = null;
    private Uri mCapturedImageURI = null;

    // Activity result key for camera
    static final int REQUEST_TAKE_PHOTO = 11111;
    /**
     * Getters and setters.
     */

    public String getCurrentPhotoPath() {
        return mCurrentPhotoPath;
    }

    public void setCurrentPhotoPath(String mCurrentPhotoPath) {
        this.mCurrentPhotoPath = mCurrentPhotoPath;
    }

    public Uri getCapturedImageURI() {
        return mCapturedImageURI;
    }

    public void setCapturedImageURI(Uri mCapturedImageURI) {
        this.mCapturedImageURI = mCapturedImageURI;
    }


    /**
     * Start the camera by dispatching a camera intent.
     */
    protected void dispatchTakePictureIntent() {

        // Check if there is a camera.
        PackageManager packageManager = getPackageManager();
        if(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA) == false){
            Toast.makeText(this, "This device does not have a camera.", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        // Camera exists? Then proceed...
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go.
            // If you don't do this, you may get a crash in some devices.
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast toast = Toast.makeText(this, "There was a problem saving the photo...", Toast.LENGTH_SHORT);
                toast.show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri fileUri = Uri.fromFile(photoFile);
                setCapturedImageURI(fileUri);
                setCurrentPhotoPath(fileUri.getPath());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, getCapturedImageURI());
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }


    /**
     * Creates the image file to which the image must be saved.
     * @return
     * @throws java.io.IOException
     */
    protected File createImageFile() throws IOException {


        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String imageFileName = new StringBuffer("D_")
                .append(currentTask.getTaskId())
                .append("_")
                .append(timeStamp)
                .append("_")
                .toString();

        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File appStorageDir = new File(storageDir, "imhere");

        // Make sure the Pictures directory exists.
        appStorageDir.mkdirs();

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                appStorageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        setCurrentPhotoPath("file:" + image.getAbsolutePath());
        return image;
    }

    /**
     * Add the picture to the photo gallery.
     * Must be called on all camera images or they will
     * disappear once taken.
     */
    protected void addPhotoToGallery() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(getCurrentPhotoPath());
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }


    /*
    invoke the system's media scanner to add your photo to the Media Provider's database,
    making it available in the Android Gallery application and to other apps.
     */
    private void galleryAddPic() {
        Log.d(DEBUG_TAG, "add to gallery : " + mCurrentPhotoPath);
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//        File f = new File("file:" + mCurrentPhotoPath);
        Uri contentUri = Uri.parse("file://" + mCurrentPhotoPath);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }


    /**
     * Scale the photo down and fit it to our image views.
     *
     * "Drastically increases performance" to set images using this technique.
     * Read more:http://developer.android.com/training/camera/photobasics.html
     */
    private void setFullImageFromFilePath(String imagePath, ImageView imageView) {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, bmOptions);
        imageView.setImageBitmap(bitmap);
    }

    ///////////////////////////////////////////////////////////////////////
    private void openSettings() {
        Intent i = new Intent(this, SettingActivity.class);
        startActivity(i);
    }

    private void markAccidentLocation(Task task) {
        // create a point marker symbol (red, size 10, of type circle)
//        SimpleMarkerSymbol simpleMarker = new SimpleMarkerSymbol(Color.RED, 10, SimpleMarkerSymbol.STYLE.CROSS);

        Drawable d = getResources().getDrawable(R.drawable.accident);
        PictureMarkerSymbol marker = new PictureMarkerSymbol(d);

        // create a point at x=-302557, y=7570663 (for a map using meters as units; this depends
        // on the spatial reference)

//        Point p = new Point(task.getPointx(), task.getPointy());
//        p = (Point) GeometryEngine.project(p,
//                SpatialReference.create(4326),
//                mMapView.getSpatialReference());
        Point p = GeometryEngine.project(task.getPointx(), task.getPointy(), mMapView.getSpatialReference());

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("Title", task.getTaskId());

        // create a graphic with the geometry and marker symbol
        Graphic pointGraphic = new Graphic(p, marker, attrs);

        // add the graphic to the graphics layer
        gLayer.addGraphic(pointGraphic);

        if (mMapView.getMaxExtent().contains(p)) {
            mMapView.centerAt(p, true);

//            mMapView.zoomToResolution(p, 10.0);
//            mMapView.zoomToScale(p, 53166.0);

            // Zooms to the current location
//            Unit mapUnit = mMapView.getSpatialReference().getUnit();
//            double zoomWidth = Unit.convertUnits(5, Unit.create(LinearUnit.Code.METER), mapUnit);
//            Envelope zoomExtent = new Envelope(p, zoomWidth, zoomWidth);
//            mMapView.setExtent(zoomExtent);
        }

    }

    private void switchOnFollowMode() {
        if(!isFollowMode) {
            if (checkGpsSettings()) {
                if (!mLocService.isStarted()) {
                    mLocService.setLocationListener(locationListener);
                    mLocService.start();
                }  else {
                    mLocService.resume();
                }
            }

            mSwitchFollowMode.setIcon(R.drawable.device_access_location_on);

            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            isFollowMode = true;
        }
    }

    private void switchOffFollowMode() {
        if(currentTask != null && currentTask.isStarted()) {
            Toast.makeText(getApplicationContext(), R.string.msg_ban_gps_turned_off, Toast.LENGTH_SHORT).show();
        } else {
            if (isFollowMode) {
                if (mLocService.isStarted()) {
                    mLocService.stop();
                }

                mSwitchFollowMode.setIcon(R.drawable.device_access_location_off);

                Toast.makeText(getApplicationContext(), R.string.msg_gps_turned_off, Toast.LENGTH_SHORT).show();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                isFollowMode = false;
            }
        }
    }

    private void updateTaskStatus(int status) {

        currentTask.setTaskZt(status);

        ContentValues values = new ContentValues();
        String current = sdf.format(new Date());

        String method = null;
        switch(status) {
            case TaskTable.TASK_START:
                values.put(TaskTable.COL_KSSJ, current);
                method = Message.ACT_START;
                break;
            case TaskTable.TASK_ARRIVE:
                values.put(TaskTable.COL_DXCSJ, current);
                method = Message.ACT_ARRIVE;
                break;
            case TaskTable.TASK_FINISH:
                values.put(TaskTable.COL_JSSJ, current);
                method = Message.ACT_FINISH;
                break;
        }

        values.put(TaskTable.COL_TASKZT, currentTask.getTaskZt());

        getContentResolver().update(
                ContentUris.withAppendedId(TaskContentProvider.CONTENT_URI, currentTask.getId()),
                values,
                null,
                null);

        Map paras = new HashMap<String, Object>();
        paras.put("taskId", currentTask.getTaskId());
        new FetchResourceTask(method, paras).execute();
    }

    public void onStartClick(View view) {
        if(currentTask != null && currentTask.isNewTask()) {
            new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("确定出发？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switchOnFollowMode();

                            elapseField.setBase(SystemClock.elapsedRealtime());
                            elapseField.start();

                            updateTaskStatus(TaskTable.TASK_START);

                            btnTaskStart.setVisibility(View.GONE);
                            btnTaskArrive.setVisibility(View.VISIBLE);

                            Toast.makeText(getApplicationContext(),
                                    "开始出发",
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    })
                    .create()
                    .show();
        }
    }

    public void onArriveClick(View view) {
        if(currentTask != null && currentTask.isStarted()) {

            new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("确定到达现场？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            updateTaskStatus(TaskTable.TASK_ARRIVE);

                            switchOffFollowMode();
                            elapseField.stop();

                            btnTaskArrive.setVisibility(View.GONE);
                            btnTaskFinish.setVisibility(View.VISIBLE);

                            btnCamera.setVisibility(View.VISIBLE);
                            btnReport.setVisibility(View.VISIBLE);

                            Toast.makeText(getApplicationContext(),
                                    "到达目的地",
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    })
                    .create()
                    .show();
        }
    }

    public void onFinishClick(View view) {
        if(currentTask != null && currentTask.isArrived()) {
            new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("确定任务已经完成？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            updateTaskStatus(TaskTable.TASK_FINISH);

                            btnTaskFinish.setVisibility(View.GONE);
                            taskSection.setVisibility(View.GONE);

                            btnCamera.setVisibility(View.INVISIBLE);
                            btnReport.setVisibility(View.INVISIBLE);
                        }
                    })
                    .create()
                    .show();
        }
    }

    public void onTaskClick(View view) {
        Intent i = new Intent(this, TaskDetailActivity.class);
        Uri todoUri = Uri.parse(TaskContentProvider.CONTENT_URI + "/" + currentTask.getId());
        i.putExtra(TaskContentProvider.CONTENT_ITEM_TYPE, todoUri);
        startActivity(i);
    }

    public void onReportClick(View view) {
        Intent i = new Intent(this, ReportActivity.class);
        i.putExtra(TaskTable.EXT_TASK_ID, currentTask.getTaskId());
        i.putExtra("CREATE", true);
        startActivity(i);
    }

    public void onCameraClick(View view) {
//        Intent intent = new Intent();
//
//        intent.setAction(Intent.ACTION_GET_CONTENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("image/*");
//
//        startActivity(intent);

        dispatchTakePictureIntent();

//        Intent i = new Intent(this, GalleryActivity.class);
//        startActivity(i);

//        Intent i = new Intent(this, CameraActivity.class);
//        startActivity(i);

    }

    private Task queryTaskById(int rowid) {

        Task task = null;

        Cursor cursor = getContentResolver().query(
                ContentUris.withAppendedId(TaskContentProvider.CONTENT_URI, rowid),
                TaskTable.COLUMNS,
                null,
                null,
                null);

        if (cursor != null) {
            task = new Task();

            cursor.moveToFirst();

            task.setId(rowid);
            task.setTaskId(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_TASKID)));
            task.setPointx(cursor.getDouble(cursor.getColumnIndexOrThrow(TaskTable.COL_POINTX)));
            task.setPointy(cursor.getDouble(cursor.getColumnIndexOrThrow(TaskTable.COL_POINTY)));
            task.setKssj(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_KSSJ)));
            task.setDxcsj(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_DXCSJ)));
            task.setTaskZt(cursor.getInt(cursor.getColumnIndexOrThrow(TaskTable.COL_TASKZT)));

            cursor.close();
        }
        return task;
    }

    class Ha extends ArcGISTiledMapServiceLayer {

        public Ha(String url) {
            super(url);
        }

        public Ha(String url, UserCredentials credentials) {
            super(url, credentials);
        }

        public Ha(String url, UserCredentials credentials, boolean initLayer) {
            super(url, credentials, initLayer);
        }

        @Override
        protected void initLayer() {
//            DisplayMetrics dm = new DisplayMetrics();
//            dm = getResources().getDisplayMetrics();
//            dm.densityDpi,

            TileInfo ti = getTileInfo();
            TileInfo tin = new TileInfo(
                    ti.getOrigin(),
                    ti.getScales(),
                    ti.getResolutions(),
                    ti.getLevels(),
//                    ti.getDPI(),
                    320,
                    ti.getTileWidth(),
                    ti.getTileHeight());

            setTileInfo(tin);

            super.initLayer();
        }
    }

    private char panMode = 'L';
    public void onSwitchMode(View v) {
        mMapView.setRotationAngle(0);

//        if(!isFollowMode)
//            return;

        switch(panMode) {
            case 'L':
                panMode = 'C';
                ((Button)v).setText("C");
                mLocService.setAutoPanMode(LocationDisplayManager.AutoPanMode.COMPASS);
                break;
            case 'C':
                panMode = 'N';
                ((Button)v).setText("N");
                mLocService.setAutoPanMode(LocationDisplayManager.AutoPanMode.NAVIGATION);
                break;
            case 'N':
                panMode = 'L';
                ((Button)v).setText("L");
                mLocService.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
                break;
        }
    }

    private void initMap() {

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isLocalMap = settings.getBoolean(getString(R.string.setting_local_map), false);

        TiledLayer baseLayer;
        if(isLocalMap) {
            baseLayer = new ArcGISLocalTiledLayer(getString(R.string.local_map));
        } else {
            baseLayer = new ArcGISTiledMapServiceLayer(getString(R.string.online_map));
        }

        mMapView.addLayer(baseLayer);

        // Create GraphicsLayer
        gLayer = new GraphicsLayer();
        // Add empty GraphicsLayer
        mMapView.addLayer(gLayer);
        Log.d(DEBUG_TAG, "add gLayer");


        // debug
        mMapView.setOnZoomListener(new OnZoomListener() {
            @Override
            public void preAction(float v, float v2, double v3) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void postAction(float v, float v2, double v3) {
//                String _m = mMapView.getScale() + " | " + mMapView.getResolution() + " | " + v3;
//                String _m = mMapView.getScale() + " | " + mMapView.getResolution();
//                Toast.makeText(MainActivity.this, _m, Toast.LENGTH_SHORT).show();
            }
        });

        mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {
            @Override
            public void onStatusChanged(Object source, STATUS status) {
                if (source == mMapView && status == STATUS.INITIALIZED) {
                    mLocService = mMapView.getLocationDisplayManager();
                    mLocService.setAllowNetworkLocation(true);
                    mLocService.setShowLocation(true);
                    mLocService.setShowPings(true);
                    mLocService.setUseCourseSymbolOnMovement(false);
                    mLocService.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
                }
            }
        });

        mMapView.setOnSingleTapListener(new OnSingleTapListener() {

            @Override
            public void onSingleTap(float x, float y) {
                if (!mMapView.isLoaded())
                    return;
            }
        });

//        mMapView.setAllowRotationByPinch(true);
    }

    private Location prevLocation = null;
    private float distance = 0.0f;
    private long preTime;
    private void handleLocationAtTaskMode(Location location) {

        if(isBetterLocation(location, prevLocation) == true) {

            if(prevLocation != null) {
                double gap = location.distanceTo(prevLocation);
                distance += gap;

                if(gap >= 1) {
                    long curTime = System.currentTimeMillis();
                    if(curTime - preTime >= updateInterval) {
                        addPosition(location);
                        preTime = curTime;
                    }

                    Point bPoint = GeometryEngine.project(prevLocation.getLongitude(), prevLocation.getLatitude(), mMapView.getSpatialReference());
                    Point ePoint = GeometryEngine.project(location.getLongitude(), location.getLatitude(), mMapView.getSpatialReference());
//                    Point bPoint = new Point(prevLocation.getLongitude(), prevLocation.getLatitude());
//                    Point ePoint = new Point(location.getLongitude(), location.getLatitude());

                    // create the line geometry
                    Polyline lineGeometry = new Polyline();
                    lineGeometry.startPath(bPoint);
                    lineGeometry.lineTo(ePoint);

                    // create the graphic using the geometry and the symbol
                    Graphic lineGraphic = new Graphic(lineGeometry, lineSymbol);
                    gLayer.addGraphic(lineGraphic);
                }
            } else {
                addPosition(location);
                distance = 0.0f;
                preTime = System.currentTimeMillis();
            }

            prevLocation = location;

            distanceField.setText(String.valueOf(Util.round(distance/1000, 1, BigDecimal.ROUND_HALF_UP)));
            if(location.hasSpeed()) {
                double currentSpeed = Util.round(location.getSpeed(),3,BigDecimal.ROUND_HALF_UP);
                double kmphSpeed =Util.round((currentSpeed*3.6),2,BigDecimal.ROUND_HALF_UP);

                speedField.setText(String.valueOf(kmphSpeed));
            }
//            longitudeField.setText(String.valueOf(Util.round(location.getLongitude(), 6, BigDecimal.ROUND_HALF_UP)));
//            latitudeField.setText(String.valueOf(Util.round(location.getLatitude(), 6, BigDecimal.ROUND_HALF_UP)));
        }

    }
    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if(!isTrack)
                return;

            if(location == null)
                return;

//            Point p = new Point(location.getLongitude(), location.getLatitude());

//            Point p = GeometryEngine.project(location.getLongitude(), location.getLatitude(), mMapView.getSpatialReference());
//            if (isFollowMode && panMode == 'N') {
//                // Check if the point is inside out map or not
//                if (mMapView.getMaxExtent().contains(p)) {
//                    mMapView.centerAt(p, true);
//                } else {
//                   // Toast.makeText(MainActivity.this.getApplicationContext(), R.string.msg_gps_out_of_range, Toast.LENGTH_SHORT).show();
//                }
//            }

            if(currentTask != null && currentTask.isStarted()) {
                handleLocationAtTaskMode(location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
//                    Toast.makeText(getApplicationContext(), R.string.msg_gps_not_available, Toast.LENGTH_SHORT).show();
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    Toast.makeText(getApplicationContext(), R.string.msg_gps_out_of_service, Toast.LENGTH_SHORT).show();
                    break;
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            Toast.makeText(getApplicationContext(), provider + " enabled.",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText(getApplicationContext(), provider + " disabled.",
                    Toast.LENGTH_SHORT).show();
        }
    };

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private void addPosition(Location loc) {
        ContentValues values =  new ContentValues();
        values.put(PositionTable.COL_TASKID, currentTask.getTaskId());


        // location.getTime()返回 UTC时间，有问题！！！
//        values.put(PositionTable.COL_TIME, sdf.format(loc.getTime()));
        values.put(PositionTable.COL_TIME, sdf.format(System.currentTimeMillis()));
        values.put(PositionTable.COL_LONGITUDE, loc.getLongitude());
        values.put(PositionTable.COL_LATITUDE, loc.getLatitude());
        values.put(PositionTable.COL_UPFLAG, 0);

        getContentResolver().insert(PositionContentProvider.CONTENT_URI, values);

        Map paras = new HashMap<String, Object>();
        paras.put(TaskTable.COL_TASKID, currentTask.getTaskId());
        paras.put(TaskTable.COL_POINTX, loc.getLongitude());
        paras.put(TaskTable.COL_POINTY, loc.getLatitude());
        new FetchResourceTask(Message.ACT_POSITION, paras).execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {

            addPhotoToGallery();

            ContentValues values = new ContentValues();
            values.put(PhotoTable.COL_TASKID, currentTask.getTaskId());
            values.put(PhotoTable.COL_NAME, getCurrentPhotoPath());
            getContentResolver().insert(PhotoContentProvider.CONTENT_URI, values);

            // Show the full sized image.
//            setFullImageFromFilePath(getCurrentPhotoPath(), mThumbnailImageView);

            PicUploadService.startActionUpload(
                    getApplicationContext(),
                    getCurrentPhotoPath(),
                    currentTask.getTaskId());

//            Toast.makeText(getApplicationContext(),
//                    getCurrentPhotoPath(),
//                    Toast.LENGTH_SHORT)
//                    .show();
        }
    }

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }


    // Check GPS settings to see if it's on or not, if not, ask if user want to swtich it on now
    private boolean checkGpsSettings() {
        final Context context = getApplicationContext();
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false, network_enabled = false;
        if (lm == null)
            lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        if (!gps_enabled && !network_enabled) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.gps_not_enabled);  // GPS not found
            builder.setMessage(R.string.open_location_settings); // Want to enable?
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.this.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            builder.create().show();
            return false;
        } else {
            return true;
        }
    }

    //两次BackPressed退出应用程序
    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, getString(R.string.prompt_exit_application), Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;

            }
        }, 2000);
    }

    // -------------  与后台通讯部分   ----------------

    private boolean mToggleIndeterminate = false;
    private class FetchResourceTask extends AsyncTask<Void, Void, Message> {

        private String method;
        private Map paras;

        public FetchResourceTask(String method, Map paras) {
            this.method = method;
            this.paras = paras;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(mToggleIndeterminate == false) {
                mToggleIndeterminate = true;
                setProgressBarIndeterminateVisibility(mToggleIndeterminate);
            }
        }

        @Override
        protected Message doInBackground(Void... params) {
            URI targetUrl = getApplicationContext().createGetUrl(method, paras);
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
            super.onPostExecute(result);

            if(mToggleIndeterminate == true) {
                mToggleIndeterminate = false;
                setProgressBarIndeterminateVisibility(mToggleIndeterminate);
            }
            if (!result.isOk()) {
                Toast.makeText(MainActivity.this, result.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

}
