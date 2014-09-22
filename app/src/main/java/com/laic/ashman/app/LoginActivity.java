package com.laic.ashman.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;
import com.laic.ashman.app.bo.Station;
import com.laic.ashman.app.bo.User;
import com.laic.ashman.app.rest.LoginMessage;
import com.laic.ashman.app.rest.Message;
import com.laic.ashman.app.rest.UserMessage;
import de.greenrobot.event.EventBus;
import org.springframework.web.client.RestClientException;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by duduba on 14-5-13.
 */
public class LoginActivity extends AbstractAsyncActivity {
    final static String DEBUG_TAG = "LoginActivity";

    private Spinner stations;
    private Spinner users;
    private ImageView head;
    private Button btnLogin;

    private UserAdapter usersAdapter;
    private ArrayAdapter<String> stationsAdapter;

    private ArrayList<User> userList = new ArrayList();
    private List<String> stationList = new ArrayList();;

    private Station[] ss = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // setting default screen to login.xml
        setContentView(R.layout.login);

        stations = (Spinner)findViewById(R.id.stations);
        users = (Spinner)findViewById(R.id.users);
        head = (ImageView)findViewById(R.id.avator);
        btnLogin = (Button) findViewById(R.id.btnLogin);

        userList.add(new User("0000", " - 选择用户 - "));
        usersAdapter = new UserAdapter(this, R.layout.spinner_item, userList);
        usersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        users.setAdapter(usersAdapter);

        stationList.add(" - 选择救援站 - ");
        stationsAdapter = new ArrayAdapter(this,R.layout.spinner_item,stationList);
        stationsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stations.setAdapter(stationsAdapter);

        stations.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                String ss = parent.getItemAtPosition(position).toString();
                if(!userIsInteracting)
                    return;

                User[] _users = ss[position].getUsers();

                userList.clear();
                for(User u : _users) {
                    userList.add(u);
                }
                usersAdapter.notifyDataSetChanged();

                int pu = users.getSelectedItemPosition();
                if(pu < userList.size()) {
                    User u = userList.get(pu);
                    new LoginTask(u.getZh()).execute();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        users.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(!userIsInteracting)
                    return;

                User u = userList.get(position);
                new LoginTask(u.getZh()).execute();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        EventBus.getDefault().register(this);

        // Push: 以apikey的方式登录，一般放在主Activity的onCreate中。
        // 这里把apikey存放于manifest文件中，只是一种存放方式，
        // 您可以用自定义常量等其它方式实现，来替换参数中的Utils.getMetaValue(PushDemoActivity.this,
        // "api_key")
        // 通过share preference实现的绑定标志开关，如果已经成功绑定，就取消这次绑定
        if (!BaiduUtils.hasBind(getApplicationContext())) {
            PushManager.startWork(getApplicationContext(),
                    PushConstants.LOGIN_TYPE_API_KEY,
                    BaiduUtils.getMetaValue(LoginActivity.this, "api_key"));
            // Push: 如果想基于地理位置推送，可以打开支持地理位置的推送的开关
            // PushManager.enableLbs(getApplicationContext());

            List<String> tags = BaiduUtils.getTagsList("cicada,panda");
            PushManager.setTags(getApplicationContext(), tags);
        }

    }

    private boolean userIsInteracting = false;
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        userIsInteracting = true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        new UserListTask().execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(Map<String, String> event) {
        String type = event.get("type");
        if(type.compareToIgnoreCase("server") == 0) {
            Toast.makeText(this, event.get("content"), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String result = intent.getStringExtra("result");
        if (result != null) {
            Toast.makeText(this, result, Toast.LENGTH_LONG).show();
        }
//            super.onNewIntent(intent);
    }

    public void onLogin(View view) {
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(i);
        finish();
    }

    private void saveImage(byte[] avator, String fileName) {
        File photo=new File(getFilesDir(), fileName + ".png");

        if (photo.exists()) {
            photo.delete();
        }

        FileOutputStream fos = null;
        try {
            fos=new FileOutputStream(photo.getPath());

            fos.write(avator);
            fos.close();
        } catch (java.io.IOException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        } finally {
            if(fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {}
            }
        }
    }

    private void handleLogin(LoginMessage response) {
        if(!response.isOk()) {
            Toast.makeText(this, response.getMessage(), Toast.LENGTH_LONG).show();
            head.setImageResource(R.drawable.avator);
            btnLogin.setEnabled(false);
        } else {
            btnLogin.setEnabled(true);

            getApplicationContext().setToken(response.getToken());
            getApplicationContext().setAccount(response.getZh());
            getApplicationContext().setUserName(response.getXm());

            if(response.getImg() != null && response.getImg().length() > 0) {
                byte[] avator = android.util.Base64.decode(response.getImg(), Base64.DEFAULT);
                head.setImageBitmap(BitmapFactory.decodeByteArray(avator, 0, avator.length));
                saveImage(avator, response.getZh());
            } else {
                head.setImageResource(R.drawable.avator);
            }
        }
    }

    private void handleUserList(UserMessage response) {
        if(!response.isOk()) {
            Toast.makeText(this, response.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            stationList.clear();
            ss = response.getData();
            for(Station s : ss) {
                stationList.add(s.getQzzd());
            }
            stationsAdapter.notifyDataSetChanged();

            if(ss != null && ss.length > 0) {
                User[] _users = ss[0].getUsers();
                userList.clear();
                for (User u : _users) {
                    userList.add(u);
                }
                usersAdapter.notifyDataSetChanged();
                new LoginTask(_users[0].getZh()).execute();
            }
        }
    }

    private class UserListTask extends AsyncTask<Void, Void, UserMessage> {
        @Override
        protected void onPreExecute() {
            showLoadingProgressDialog("获取用户列表，请等待......");
        }

        @Override
        protected UserMessage doInBackground(Void... params) {
            Map paras = new HashMap<String, Object>();

            URI targetUrl = getApplicationContext().createGetUrl(Message.ACT_USERLIST, paras);

            try {
                UserMessage response = getApplicationContext().getRestTemplate().getForObject(targetUrl, UserMessage.class);
                return response;
            } catch (RestClientException e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
                return new UserMessage(Message.NETERR, getString(R.string.com_network_err));
            }
        }

        @Override
        protected void onPostExecute(UserMessage result) {
            dismissProgressDialog();
            handleUserList(result);
        }
    }

    private class LoginTask extends AsyncTask<Void, Void, LoginMessage> {

        private String account;
        public LoginTask(String account) {
            this.account = account;
        }

        @Override
        protected void onPreExecute() {
            showLoadingProgressDialog("登录中，请等待......");
        }

        @Override
        protected LoginMessage doInBackground(Void... params) {
            Map paras = new HashMap<String, Object>();
            paras.put("account", account);
            paras.put("password", "");

            URI targetUrl = getApplicationContext().createGetUrl(Message.ACT_LOGIN, paras);

            try {
                // Make the HTTP GET request, marshaling the response from JSON to Message
//                ResponseEntity<LoginMessage> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, LoginMessage.class);
//                LoginMessage response = responseEntity.getBody();

                LoginMessage response = getApplicationContext().getRestTemplate().getForObject(targetUrl, LoginMessage.class);
                return response;
            } catch (RestClientException e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
                return new LoginMessage(Message.NETERR, getString(R.string.com_network_err));
            }
        }

        @Override
        protected void onPostExecute(LoginMessage result) {
            dismissProgressDialog();
            handleLogin(result);
        }

    }



    class UserAdapter extends ArrayAdapter<User> {

        ArrayList<User> user;

        public UserAdapter(Activity context, int resource, ArrayList<User> user) {
            super(context, resource, user);
            this.user = user;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView v = (TextView)super.getView(position, convertView, parent);
            User current = user.get(position);
            v.setText(current.getXm());
            return v;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView v = (TextView)super.getView(position, convertView, parent);
            User current = user.get(position);
            v.setText(current.getXm());
            return v;
        }


    }
}