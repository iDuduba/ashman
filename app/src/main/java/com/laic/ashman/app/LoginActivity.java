package com.laic.ashman.app;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;
import com.laic.ashman.app.rest.LoginMessage;
import com.laic.ashman.app.rest.Message;
import de.greenrobot.event.EventBus;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by duduba on 14-5-13.
 */
public class LoginActivity extends AbstractAsyncActivity {

    private EditText user;
    private EditText password;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // setting default screen to login.xml
        setContentView(R.layout.login);

        user = (EditText)findViewById(R.id.user);
        password = (EditText)findViewById(R.id.password);

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
        new FetchResourceTask().execute();
    }

    private void displayResponse(LoginMessage response) {
        if(!response.isOk()) {
            Toast.makeText(this, response.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            getApplicationContext().setToken(response.getToken());
            getApplicationContext().setAccount(user.getText().toString());
            getApplicationContext().setUserName(response.getXm());

            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(i);
            finish();
        }
    }

    private class FetchResourceTask extends AsyncTask<Void, Void, LoginMessage> {

        @Override
        protected void onPreExecute() {
            showLoadingProgressDialog("登录中，请等待......");
        }

        @Override
        protected LoginMessage doInBackground(Void... params) {
            Map paras = new HashMap<String, Object>();
            paras.put("account", user.getText().toString());
            paras.put("password", Util.encodeByMD5(password.getText().toString()));

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
            displayResponse(result);
        }

    }

}