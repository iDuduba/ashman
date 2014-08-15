package com.laic.ashman.app;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.esri.android.map.*;
import com.esri.android.map.ags.ArcGISLocalTiledLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.laic.ashman.app.bo.Task;
import com.laic.ashman.app.provider.PositionContentProvider;
import com.laic.ashman.app.provider.PositionTable;
import com.laic.ashman.app.provider.TaskContentProvider;
import com.laic.ashman.app.provider.TaskTable;

import java.util.*;

/**
 * Created by duduba on 14-5-16.
 */
public class MapActivity extends Activity {

    final static String DEBUG_TAG = "MapActivity";

    private MapView mMap;
    private GraphicsLayer gLayer;

    View content;
    Callout callout;

    private String mTaskRowId;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        Intent intent = getIntent();
        mTaskRowId = intent.getStringExtra(TaskTable.EXT_TASK_ROW_ID);

        mMap = (MapView) findViewById(R.id.map);
        mMap.enableWrapAround(true);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isLocalMap = settings.getBoolean(getString(R.string.setting_local_map), false);
        TiledLayer baseLayer;
        if(isLocalMap) {
            baseLayer = new ArcGISLocalTiledLayer(getString(R.string.local_map));
        } else {
            baseLayer = new ArcGISTiledMapServiceLayer(getString(R.string.online_map));
        }

        mMap.addLayer(baseLayer);

        mMap.setOnStatusChangedListener(new OnStatusChangedListener() {
            @Override
            public void onStatusChanged(Object source, STATUS status) {
                if (source == mMap && status == STATUS.INITIALIZED) {
                    gLayer = new GraphicsLayer();
                    mMap.addLayer(gLayer);

                    Task task = queryTaskById(Integer.parseInt(mTaskRowId));
                    if(task != null) {
                        showPathEx(task);
                    }

                }
            }
        });

        content = createContent();

        mMap.setOnSingleTapListener(new OnSingleTapListener() {

            @Override
            public void onSingleTap(float x, float y) {

                if (!mMap.isLoaded())
                    return;

                // Handles the tapping on Graphic

                int[] graphicIDs = gLayer.getGraphicIDs(x, y, 25);
                if (graphicIDs != null && graphicIDs.length > 0) {
                    callout = mMap.getCallout();
                    callout.setStyle(R.xml.countypop);
                    callout.setOffset(0, 10);

                    // Graphic gr = graphics[0];
                    Graphic gr = gLayer.getGraphic(graphicIDs[0]);
                    String title = (String) gr.getAttributeValue("Title");
                    if(title != null && title.length() > 0) {
                        updateContent(title);
                        callout.show(mMap.toMapPoint(new Point(x, y)), content);
                    }
                } else {
                    if (callout != null && callout.isShowing()) {
                        callout.hide();
                    }
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        mMap.unpause();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMap.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMap.recycle();
        mMap = null;
    }

    final static int V_TITLE_ID = 1;
    @SuppressWarnings("ResourceType")
    public View createContent() {
        // create linear layout for the entire view
        LinearLayout layout = new LinearLayout(this);
        layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);

        // create TextView for the title
        TextView titleView = new TextView(this);
        titleView.setId(V_TITLE_ID);

        // titleView.setText(title);
        titleView.setTextColor(Color.GRAY);
        layout.addView(titleView);

        return layout;
    }

    @SuppressWarnings("ResourceType")
    public void updateContent(String title) {
        if (content == null)
            return;

        TextView txt = (TextView) content.findViewById(V_TITLE_ID);
        txt.setText(title);

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
            if(cursor.moveToFirst()) {
                task = new Task();
                task.setId(rowid);
                task.setTaskId(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_TASKID)));
                task.setPointx(cursor.getDouble(cursor.getColumnIndexOrThrow(TaskTable.COL_POINTX)));
                task.setPointy(cursor.getDouble(cursor.getColumnIndexOrThrow(TaskTable.COL_POINTY)));
                task.setKssj(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_KSSJ)));
                task.setDxcsj(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COL_DXCSJ)));
                task.setTaskZt(cursor.getInt(cursor.getColumnIndexOrThrow(TaskTable.COL_TASKZT)));
            }
            cursor.close();
        }
        return task;
    }

    private void showPathEx(Task task) {

        SimpleMarkerSymbol beginMarker = new SimpleMarkerSymbol(Color.RED, 10, SimpleMarkerSymbol.STYLE.CIRCLE);
        SimpleMarkerSymbol endMarker = new SimpleMarkerSymbol(Color.RED, 10, SimpleMarkerSymbol.STYLE.SQUARE);
        SimpleMarkerSymbol marker = new SimpleMarkerSymbol(Color.BLUE, 4, SimpleMarkerSymbol.STYLE.CIRCLE);

        Point point = null;

        String selection = PositionTable.COL_TASKID + "=? AND datetime("+ PositionTable.COL_TIME+") >= datetime(?)"+
                " AND datetime("+ PositionTable.COL_TIME+") <= datetime(?)";

        Cursor cursor = getContentResolver().query(
                PositionContentProvider.CONTENT_URI,
                PositionTable.COLUMNS,
                selection,
                new String[] {task.getTaskId(), task.getKssj(), task.getDxcsj()},
                PositionTable.COL_TIME);

        int i = 0;
        String t = null;
        while (cursor.moveToNext()) {
            t = cursor.getString(1);

            point = GeometryEngine.project(cursor.getFloat(2), cursor.getFloat(3), mMap.getSpatialReference());
            if (i == 0) {
                if(mMap.getMaxExtent().contains(point)) {
                    mMap.centerAt(point, true);
                }
                Map props = new HashMap<String, Object>();
                props.put("Title", t);
                gLayer.addGraphic(new Graphic(point, beginMarker, props));
            } else {
                gLayer.addGraphic(new Graphic(point, marker));
            }
            i++;
        }
        cursor.close();

        if(i > 0) {
            Map props = new HashMap<String, Object>();
            props.put("Title", t);
            gLayer.addGraphic(new Graphic(point, endMarker, props));
        }
    }

}