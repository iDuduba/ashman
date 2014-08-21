package com.laic.ashman.app;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import com.laic.ashman.app.provider.TaskContentProvider;
import com.laic.ashman.app.provider.TaskTable;
import com.laic.slideexpandable.library.SlideExpandableListAdapter;
import de.greenrobot.event.EventBus;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by duduba on 14-8-19.
 */
public class TaskListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_TASK_STATUS = "task.status";

    // private Cursor cursor;
    private SimpleCursorAdapter mAdapter;

    private MainApplication app;
    private int taskStatus = TaskTable.TASK_NEW;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(
                R.layout.fragment_tasklist, container, false);

        Bundle args = getArguments();
        taskStatus = args.getInt(ARG_TASK_STATUS);

//        getListView().setTextFilterEnabled(true);
//        getListView().setDividerHeight(2);
//        registerForContextMenu(getListView());

        app = (MainApplication)getActivity().getApplicationContext();

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Fields from the database (projection)
        // Must include the _id column for the mAdapter to work
        String[] from = {TaskTable.COL_TASKID, TaskTable.COL_JJSJ, TaskTable.COL_TASKZT, TaskTable.COL_SJZH};
        // Fields on the UI to which we map
        int[] to = new int[]{R.id.accident, R.id.occur, R.id.status, R.id.sjzh};

        mAdapter = new TaskCursorAdapter(getActivity(), R.layout.taskitem, null, from, to, 0);

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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        StringBuffer select = new StringBuffer(TaskTable.COL_CQRY);
        select.append("=").append(app.getAccount()).append(" and (").append(TaskTable.COL_TASKZT);
        if(taskStatus == TaskTable.TASK_RUNNING) {
            select.append("=").append(TaskTable.TASK_START).append(" or ").append(TaskTable.COL_TASKZT).append("=").append(TaskTable.TASK_ARRIVE);
        } else {
            select.append("=").append(taskStatus);
        }
        select.append(")");

        CursorLoader cursorLoader = new CursorLoader(
                getActivity(),
                TaskContentProvider.CONTENT_URI,
                TaskTable.COLUMNS,
                select.toString(),
                null,
                TaskTable.COL_JJSJ + " desc");

        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        /*
        设置row item上的button的下面属性：
            android:focusable="false"
            android:focusableInTouchMode="false"
        否则onListItemClick不会被调用
        切记切记！！！
         */
        Intent i = new Intent(getActivity(), TaskDetailActivity.class);
        Uri todoUri = Uri.parse(TaskContentProvider.CONTENT_URI + "/" + id);
        i.putExtra(TaskContentProvider.CONTENT_ITEM_TYPE, todoUri);

        startActivity(i);
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
            int statusColumnIndex = cursor.getColumnIndex(TaskTable.COL_TASKZT);
            final int taskStatus = cursor.getInt(statusColumnIndex);

            View.OnClickListener lsn = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Map event = new HashMap<String, String>();
                    event.put("type", "task");
                    event.put("id", rowId);
                    event.put("status", String.valueOf(taskStatus));
                    EventBus.getDefault().post(event);

                    getActivity().finish();
                }
            };

            Button btnRun = (Button) view.findViewById(R.id.t_run);
            btnRun.setOnClickListener(lsn);
            Button btnContinue = (Button) view.findViewById(R.id.t_continue);
            btnContinue.setOnClickListener(lsn);

            Button btnPhoto = (Button) view.findViewById(R.id.t_photo);
            btnPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(getActivity(), GalleryActivity.class);
                    i.putExtra(TaskTable.EXT_TASK_ID, taskId);
                    startActivity(i);
                }
            });
            Button btnReport = (Button) view.findViewById(R.id.t_report);
            btnReport.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(getActivity(), ReportActivity.class);
                    i.putExtra(TaskTable.EXT_TASK_ID, taskId);
                    i.putExtra("CREATE", false);
                    startActivity(i);
                }
            });
            Button btnPath = (Button) view.findViewById(R.id.t_path);
            btnPath.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(getActivity(), MapActivity.class);
                    i.putExtra(TaskTable.EXT_TASK_ROW_ID, rowId);
                    startActivity(i);
                }
            });


            if(taskStatus == TaskTable.TASK_NEW) {
                btnRun.setVisibility(View.VISIBLE);
                btnContinue.setVisibility(View.GONE);
                btnPhoto.setVisibility(view.GONE);
                btnReport.setVisibility(view.GONE);
                btnPath.setVisibility(view.GONE);
            } else if(taskStatus == TaskTable.TASK_FINISH) {
                btnRun.setVisibility(View.GONE);
                btnContinue.setVisibility(View.GONE);
                btnPhoto.setVisibility(view.VISIBLE);
                btnReport.setVisibility(view.VISIBLE);
                btnPath.setVisibility(view.VISIBLE);
            } else {
                btnRun.setVisibility(View.GONE);
                btnContinue.setVisibility(View.VISIBLE);
                btnPhoto.setVisibility(view.GONE);
                btnReport.setVisibility(view.GONE);
                btnPath.setVisibility(view.GONE);
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
                } else if (taskZt == TaskTable.TASK_FINISH) {
                    ((ImageView)view).setImageResource(R.drawable.task_finish);
                } else  {
                    ((ImageView)view).setImageResource(R.drawable.task_cancel);
                }
                return true;
            }
            // For others, we simply return false so that the default binding
            // happens.
            return false;
        }

    }
}
