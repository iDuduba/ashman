package com.laic.ashman.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;
import com.laic.ashman.app.provider.*;
import de.greenrobot.event.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by duduba on 14-5-12.
 */
public class GalleryActivity extends Activity {
    final static String DEBUG_TAG = "GalleryActivity";

    private int count;
    private Thumbnail[] thumbnails;
    private ImageAdapter mAdapter;
    private String taskId;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);

        setContentView(R.layout.gallery);

        Intent intent = getIntent();
        taskId = intent.getStringExtra(TaskTable.EXT_TASK_ID);

        final String[] columns = { MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID };
        final String orderBy = MediaStore.Images.Media._ID;

        Cursor imagecursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                columns,
                MediaStore.Images.Media.DATA + " like ? ",
                new String[] {"%/imhere/D_" + taskId + "_%"},
                orderBy);

        int image_column_index = imagecursor.getColumnIndex(MediaStore.Images.Media._ID);

        this.count = imagecursor.getCount();
        this.thumbnails = new Thumbnail[this.count];

        for (int i = 0; i < this.count; i++) {
            thumbnails[i] = new Thumbnail();

            imagecursor.moveToPosition(i);
            int id = imagecursor.getInt(image_column_index);

            thumbnails[i].image = MediaStore.Images.Thumbnails.getThumbnail(
                    getApplicationContext().getContentResolver(),
                    id,
                    MediaStore.Images.Thumbnails.MINI_KIND,
                    null);

            int dataColumnIndex = imagecursor.getColumnIndex(MediaStore.Images.Media.DATA);
            thumbnails[i].path = imagecursor.getString(dataColumnIndex);

            Cursor cursor = getContentResolver().query(
                    PhotoContentProvider.CONTENT_URI,
                    PhotoTable.COLUMNS,
                    PhotoTable.COL_NAME + "='" + thumbnails[i].path + "'",
                    null,
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                short upFlag = cursor.getShort(cursor.getColumnIndexOrThrow(PhotoTable.COL_UPFLAG));
                thumbnails[i].uploaded = (upFlag == 0 ? false : true);
                cursor.close();
            }
        }

        GridView gallery = (GridView) findViewById(R.id.PhoneImageGrid);
        mAdapter = new ImageAdapter();
        gallery.setAdapter(mAdapter);
        imagecursor.close();

        final Button selectBtn = (Button) findViewById(R.id.selectBtn);
        selectBtn.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // TODO Auto-generated method stub
                final int len = thumbnails.length;
                int cnt = 0;
                List selectImages = new ArrayList<String>();
                for (int i =0; i<len; i++)
                {
                    if (thumbnails[i].selected){
                        cnt++;
                        selectImages.add(thumbnails[i].path);
                    }
                }
                if (cnt == 0){
                    Toast.makeText(getApplicationContext(),
                            "Please select at least one image",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "You've selected Total " + cnt + " image(s).",
                            Toast.LENGTH_LONG).show();

                    for(Object p : selectImages) {
                        PicUploadService.startActionUpload(getApplicationContext(), (String)p, taskId);
                    }
//                    Log.d("SelectedImages", selectImages);
                }
            }
        });

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(Map<String, String> event) {
        String type = event.get("type");
        if(type.compareToIgnoreCase("upload") == 0) {
            String action = event.get("action");
            String value = event.get("value");

            if(action.compareToIgnoreCase(PicUploadService.UPLOAD_ACTION_START) == 0) {
                setProgress(0);
                Toast.makeText(getApplicationContext(),
                        "Start upload: " + value,
                        Toast.LENGTH_SHORT).show();
            } else if(action.compareToIgnoreCase(PicUploadService.UPLOAD_ACTION_FINISH) == 0) {
                setProgress(10000);
                Toast.makeText(getApplicationContext(),
                        "Finish upload: " + value,
                        Toast.LENGTH_SHORT).show();
            } else if(action.compareToIgnoreCase(PicUploadService.UPLOAD_ACTION_ERROR) == 0) {
                setProgress(10000);
                Toast.makeText(getApplicationContext(),
                        value,
                        Toast.LENGTH_SHORT).show();
            } else {
                int percent = Integer.parseInt(value) * 100;
                setProgress(percent);
            }
        }
    }

    class Thumbnail {
        Bitmap image;
        String path;
        boolean uploaded = false;
        boolean selected = false;
    }


    private int selected = 0;
    public class ImageAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public ImageAdapter() {
            mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return count;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.galleryitem, null);
                holder.imageview = (ImageView) convertView.findViewById(R.id.thumbImage);
                holder.uploaded = (ImageView) convertView.findViewById(R.id.uploadFlag);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.imageview.setId(position);
            holder.imageview.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int id = v.getId();
                    if(!thumbnails[id].uploaded) {
                        if(!thumbnails[id].selected) {
                            thumbnails[id].selected = true;
                            v.setSelected(true);
                            selected++;
                        } else {
                            thumbnails[id].selected = false;
                            v.setSelected(false);
                            selected--;
                        }
                    }
                    return true;
                }
            });
            holder.imageview.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    int id = v.getId();
                    if(selected == 0) {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse("file://" + thumbnails[id].path), "image/*");
                        startActivity(intent);
                    } else {
                        if(!thumbnails[id].uploaded) {
                            if(!thumbnails[id].selected) {
                                thumbnails[id].selected = true;
                                v.setSelected(true);
                                selected++;
                            } else {
                                thumbnails[id].selected = false;
                                v.setSelected(false);
                                selected--;
                            }
                        }
                    }
                }
            });
            holder.imageview.setImageBitmap(thumbnails[position].image);

            if(thumbnails[position].uploaded) {
                holder.uploaded.setVisibility(View.VISIBLE);
            } else {
                holder.uploaded.setVisibility(View.INVISIBLE);
            }

            holder.id = position;
            return convertView;
        }
    }
    class ViewHolder {
        ImageView imageview;
        ImageView uploaded;
        int id;
    }

}