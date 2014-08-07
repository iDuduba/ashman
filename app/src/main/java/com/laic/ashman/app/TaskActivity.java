package com.laic.ashman.app;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.laic.ashman.app.bo.Task;
import com.laic.ashman.app.provider.*;
import com.laic.ashman.app.rest.Message;
import com.laic.ashman.app.rest.TaskMessage;
import com.laic.slideexpandable.library.SlideExpandableListAdapter;

import de.greenrobot.event.EventBus;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by duduba on 14-5-6.
 */
public class TaskActivity extends AbstractAsyncListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    final static String DEBUG_TAG = "TaskActivity";

    // private Cursor cursor;
    private SimpleCursorAdapter mAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task);

        getListView().setTextFilterEnabled(true);

        getListView().setDividerHeight(2);
        fillData();
        registerForContextMenu(getListView());
    }

    // creates a new loader after the initLoader () call
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.

//        String ORDER = TaskTable.COL_TASKZT + "," + TaskTable.COL_JJSJ;

        CursorLoader cursorLoader = new CursorLoader(
                this,
                TaskContentProvider.CONTENT_URI,
                TaskTable.COLUMNS,
                TaskTable.COL_TASKZT + "=" + TaskTable.TASK_NEW,
                null,
                TaskTable.COL_JJSJ + " desc");

        return cursorLoader;
    }

    // Called when a previously created loader has finished loading
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
    }

    // Called when a previously created loader is reset, making the data unavailable
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.task_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_task_download:
//                createTask();
                new FetchResourceTask().execute();
                return true;
            case R.id.action_task_remove:
                clearTaskList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        /*
        设置row item上的button的下面属性：
            android:focusable="false"
            android:focusableInTouchMode="false"
        否则onListItemClick不会被调用
        切记切记！！！
         */
        Intent i = new Intent(this, TaskDetailActivity.class);
        Uri todoUri = Uri.parse(TaskContentProvider.CONTENT_URI + "/" + id);
        i.putExtra(TaskContentProvider.CONTENT_ITEM_TYPE, todoUri);

        startActivity(i);
    }

    private boolean bNew = true;
    public void onNewList(View v) {
        if(!bNew) {
            mAdapter.getFilter().filter(TaskTable.COL_TASKZT + "=" + TaskTable.TASK_NEW);
            bNew = !bNew;
        }
    }
    public void onOldList(View v) {
        if (bNew) {
            mAdapter.getFilter().filter(TaskTable.COL_TASKZT + "<>" + TaskTable.TASK_NEW);
            bNew = !bNew;
        }
    }

    private void clearTaskList() {
        new AlertDialog.Builder(this)
                .setTitle("警告")
                .setMessage("删除所有本地任务数据？")
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String account = getApplicationContext().getAccount();

//                        getContentResolver().delete(PositionContentProvider.CONTENT_URI, null, null);
//                        getContentResolver().delete(ReportContentProvider.CONTENT_URI, null, null);

                        Cursor cursor = getContentResolver().query(TaskContentProvider.CONTENT_URI,
                                new String[]{TaskTable.COL_TASKID},
                                TaskTable.COL_CQRY + "=?",
                                new String[]{account},
                                null);

                        while (cursor.moveToNext()) {
                            String taskId = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_TASKID));
                            String[] values = new String[] {taskId};

                            getContentResolver().delete(
                                    PositionContentProvider.CONTENT_URI,
                                    PositionTable.COL_TASKID + "=?",
                                    values);

                            getContentResolver().delete(
                                    ReportContentProvider.CONTENT_URI,
                                    ReportTable.COL_TASKID + "=?",
                                    values);
                        }
                        cursor.close();


                        getContentResolver().delete(
                                TaskContentProvider.CONTENT_URI,
                                TaskTable.COL_CQRY + "=?",
                                new String[]{account});

                        getLoaderManager().restartLoader(0, null, TaskActivity.this);
                    }
                })
                .create()
                .show();
    }

    private void fillData() {
        // Fields from the database (projection)
        // Must include the _id column for the mAdapter to work
        String[] from = {
                TaskTable.COL_TASKID,
                TaskTable.COL_JJSJ,
                TaskTable.COL_TASKZT,
                TaskTable.COL_SJZH
        };

        // Fields on the UI to which we map
        int[] to = new int[]{
                R.id.accident,
                R.id.occur,
                R.id.status,
                R.id.sjzh
        };

        mAdapter = new TaskCursorAdapter(
                this,
                R.layout.taskitem,
                null,
                from,
                to,
                0);

        // We set the view binder for the mAdapter to our own CustomViewBinder.
        // The code for the custom view binder is below.
        mAdapter.setViewBinder(new CustomViewBinder());

//        setListAdapter(mAdapter);
        setListAdapter(new SlideExpandableListAdapter(
                mAdapter,
                R.id.expandable_toggle_button,
                R.id.expandable
        ));

        //To initialize a background query we have to use LoaderManager.initLoader() method.
        // This will initiate the background tasks.
        // Once we initialize the query, onCreateLoader() method will be invoked by Android.
        getLoaderManager().initLoader(0, null, this);
    }

    //extend the SimpleCursorAdapter to create a custom class where we
    //can override the getView to change the row colors
    private class TaskCursorAdapter extends SimpleCursorAdapter{

        public TaskCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            //get reference to the row
            View view = super.getView(position, convertView, parent);

            Cursor cursor = (Cursor)getItem(position);

//            View status = view.findViewById(R.id.status);
//            boolean newTaskFlag = (boolean)status.getTag(R.id.tag_task_status);
//            final int rowId = (int)status.getTag(R.id.tag_task_row_id);

            int idColumnIndex = cursor.getColumnIndex(TaskTable.COL_ID);
            final String rowId = cursor.getString(idColumnIndex);
            int taskIdColumnIndex = cursor.getColumnIndex(TaskTable.COL_TASKID);
            final String taskId = cursor.getString(taskIdColumnIndex);

            Button btnRun = (Button) view.findViewById(R.id.t_run);
            btnRun.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Map event = new HashMap<String, String>();
                    event.put("type", "task");
                    event.put("id", rowId);
                    EventBus.getDefault().post(event);

                    finish();
                }
            });
            Button btnPhoto = (Button) view.findViewById(R.id.t_photo);
            btnPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(TaskActivity.this, GalleryActivity.class);
                    i.putExtra(TaskTable.EXT_TASK_ID, taskId);
                    startActivity(i);
                }
            });
            Button btnReport = (Button) view.findViewById(R.id.t_report);
            btnReport.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(TaskActivity.this, ReportActivity.class);
                    i.putExtra(TaskTable.EXT_TASK_ID, taskId);
                    i.putExtra("CREATE", false);
                    startActivity(i);
                }
            });
            Button btnPath = (Button) view.findViewById(R.id.t_path);
            btnPath.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(TaskActivity.this, MapActivity.class);
                    i.putExtra(TaskTable.EXT_TASK_ROW_ID, rowId);
                    startActivity(i);
                }
            });

            int statusColumnIndex = cursor.getColumnIndex(TaskTable.COL_TASKZT);
            int taskStatus = cursor.getInt(statusColumnIndex);

            if(taskStatus == TaskTable.TASK_NEW) {
                btnRun.setVisibility(View.VISIBLE);
                btnPhoto.setVisibility(view.GONE);
                btnReport.setVisibility(view.GONE);
                btnPath.setVisibility(view.GONE);
            } else {
                btnRun.setVisibility(View.GONE);
                btnPhoto.setVisibility(view.VISIBLE);
                btnReport.setVisibility(view.VISIBLE);
                btnPath.setVisibility(view.VISIBLE);
            }


            //check for odd or even to set alternate colors to the row background
            if(position % 2 == 0){
                view.setBackgroundColor(Color.rgb(238, 233, 233));
            }
            else {
                view.setBackgroundColor(Color.rgb(255, 255, 255));
            }
            return view;
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            FilterQueryProvider filter = getFilterQueryProvider();
            if (filter != null) {
                return filter.runQuery(constraint);
            }

            return getContentResolver().query(
                    TaskContentProvider.CONTENT_URI,
                    TaskTable.COLUMNS,
                    constraint.toString(),
                    null,
                    TaskTable.COL_JJSJ + " desc");

//            Uri uri = Uri.withAppendedPath(
//                    TaskContentProvider.CONTENT_URI,
//                    Uri.encode(constraint.toString()));
//
//            return mContent.query(uri, CONTACT_PROJECTION, null, null, null);
        }
    }

    /**
     * Custom ViewBinder to handle custom view showing in SimpleCursorAdapter.
     *
     */
    private class CustomViewBinder implements SimpleCursorAdapter.ViewBinder {

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            if (columnIndex == cursor.getColumnIndex(TaskTable.COL_TASKZT)) {
                int taskZt = cursor.getInt(columnIndex);
                if (taskZt == TaskTable.TASK_NEW) {
                    ((ImageView)view).setImageResource(R.drawable.task_new);
                } else {
                    ((ImageView)view).setImageResource(R.drawable.task_finish);
                }
                return true;
            }
            // For others, we simply return false so that the default binding
            // happens.
            return false;
        }

    }


    private void displayResponse(TaskMessage response) {
        if(!response.isOk()) {
            Toast.makeText(this, response.getMessage(), Toast.LENGTH_LONG).show();
        } else {

            String[] projection = new String[] {TaskTable.COL_ID};

            for(Task task : response.getData()) {
                ContentValues values = new ContentValues();

                values.put(TaskTable.COL_TASKID, task.getTaskId());
                values.put(TaskTable.COL_TASKZT, task.getTaskZt());
                values.put(TaskTable.COL_SJJSSJ, task.getSjjssj());
                values.put(TaskTable.COL_KSSJ, task.getKssj());
                values.put(TaskTable.COL_DXCSJ, task.getDxcsj());
                values.put(TaskTable.COL_JSSJ, task.getJssj());
                values.put(TaskTable.COL_EVENTID, task.getEventId());
                values.put(TaskTable.COL_SJMS, task.getSjms());
                values.put(TaskTable.COL_SJLX, task.getSjlx());
                values.put(TaskTable.COL_JJSJ, task.getJjsj());
                values.put(TaskTable.COL_DSRDH, task.getDsrdh());
                values.put(TaskTable.COL_SJCPH, task.getSjcph());
                values.put(TaskTable.COL_SJFX, task.getSjfx());
                values.put(TaskTable.COL_SJZH, task.getSjzh());
                values.put(TaskTable.COL_POINTX, task.getPointx());
                values.put(TaskTable.COL_POINTY, task.getPointy());
                values.put(TaskTable.COL_CQCL, task.getCqcl());
                values.put(TaskTable.COL_CQRY, task.getCqry());
                values.put(TaskTable.COL_CQRYDH, task.getCqrydh());
                values.put(TaskTable.COL_BZ, task.getBz());

                Cursor cursor = getContentResolver().query(
                        TaskContentProvider.CONTENT_URI,
                        projection,
                        TaskTable.COL_TASKID + "=?",
                        new String[] {task.getTaskId()},
                        null);

                boolean addFlag = true;
                if (cursor.getCount() > 0) {
                    addFlag = false;
                }
                cursor.close();

                if(addFlag) {
//                    Log.d(DEBUG_TAG, "A -> " + task.getTaskId());
                    getContentResolver().insert(TaskContentProvider.CONTENT_URI, values);
                } else {
//                    Log.d(DEBUG_TAG, "U -> " + task.getTaskId());
                    getContentResolver().update(
                            TaskContentProvider.CONTENT_URI,
                            values,
                            TaskTable.COL_TASKID + "=?",
                            new String[] {task.getTaskId()});
                }
            }

            getLoaderManager().restartLoader(0, null, this);
            onNewList(null);

            String msg = "共下载 " + response.getTotal() + " 项任务.";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    private class FetchResourceTask extends AsyncTask<Void, Void, TaskMessage> {

        @Override
        protected void onPreExecute() {
            showLoadingProgressDialog("下载任务列表，请等待......");
        }

        @Override
        protected TaskMessage doInBackground(Void... params) {
            Map paras = new HashMap<String, Object>();

            URI targetUrl = getApplicationContext().createGetUrl(Message.ACT_GETTASK, paras);

            try {
                // Make the network request
                // Make the HTTP GET request, marshaling the response from JSON to Message
//                ResponseEntity<LoginMessage> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, LoginMessage.class);
//                LoginMessage response = responseEntity.getBody();

                TaskMessage response = getApplicationContext().getRestTemplate().getForObject(targetUrl, TaskMessage.class);
                return response;
            } catch (RestClientException e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
                return new TaskMessage(Message.NETERR, getString(R.string.com_network_err));
            }
        }

        @Override
        protected void onPostExecute(TaskMessage result) {
            dismissProgressDialog();
            displayResponse(result);
        }

    }
}