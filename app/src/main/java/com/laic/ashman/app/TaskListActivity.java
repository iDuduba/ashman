package com.laic.ashman.app;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.laic.ashman.app.bo.Task;
import com.laic.ashman.app.provider.*;
import com.laic.ashman.app.rest.Message;
import com.laic.ashman.app.rest.TaskMessage;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by duduba on 14-8-19.
 */
public class TaskListActivity extends AbstractAsyncFragmentActivity implements ActionBar.TabListener {

    TaskPagerAdapter mPagerAdapter;
    ViewPager mViewPager;

    private ActionBar actionBar;

    // Tab titles
    private String[] tabs = { "新任务", "未完成任务", "已完成任务" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tasklist);

        mPagerAdapter = new TaskPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        // When swiping between pages, select the
                        // corresponding tab.
                        getActionBar().setSelectedNavigationItem(position);
                    }
                });
        mViewPager.setAdapter(mPagerAdapter);


        // Specify that tabs should be displayed in the action bar.
        actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        // Adding Tabs
        for (String tab_name : tabs) {
            actionBar.addTab(actionBar.newTab().setText(tab_name)
                    .setTabListener(this));
        }
    }


    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        // When the tab is selected, switch to the
        // corresponding page in the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

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

    class TaskPagerAdapter extends FragmentPagerAdapter {
        TaskPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = new TaskListFragment();

            Bundle args = new Bundle();
            int status = 1;
            switch(position) {
                case 0:
                    status = TaskTable.TASK_NEW;
                    break;
                case 1:
                    status = TaskTable.TASK_RUNNING;
                    break;
                case 2:
                    status = TaskTable.TASK_FINISH;
                    break;
            }
            args.putInt(TaskListFragment.ARG_TASK_STATUS, status);
            fragment.setArguments(args);

            return fragment;
        }


        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return super.getPageTitle(position);
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

//                        getLoaderManager().restartLoader(0, null, TaskListActivity.this);
                    }
                })
                .create()
                .show();
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

//            getLoaderManager().restartLoader(0, null, this);

            String msg = "共下载 " + response.getTotal() + " 项任务.";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    class FetchResourceTask extends AsyncTask<Void, Void, TaskMessage> {

        @Override
        protected void onPreExecute() {
            showLoadingProgressDialog("下载任务列表，请等待......");
        }

        @Override
        protected TaskMessage doInBackground(Void... params) {
            Map paras = new HashMap<String, Object>();

            URI targetUrl = getApplicationContext().createGetUrl(Message.ACT_TASKLIST, paras);

            try {
                // Make the network request
                // Make the HTTP GET request, marshaling the response from JSON to Message
//                ResponseEntity<LoginMessage> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, LoginMessage.class);
//                LoginMessage response = responseEntity.getBody();

                TaskMessage response = getApplicationContext().getRestTemplate().getForObject(targetUrl, TaskMessage.class);
                return response;
            } catch (RestClientException e) {
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
