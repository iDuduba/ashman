package com.laic.ashman.app;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import com.baidu.frontia.FrontiaApplication;
import com.laic.ashman.app.rest.HttpUtils;
import com.laic.ashman.app.rest.Message;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

/**
 * Created by duduba on 14-7-21.
 */
public class MainApplication extends FrontiaApplication {

    private static Context sContext;

    private RestTemplate restTemplate;
    private String token = null;
    private String account = null;
    private String userName = null;

    @Override
    public void onCreate() {
        super.onCreate();

        sContext = getApplicationContext();

        // set up the database
//        this.repositoryHelper = new SQLiteConnectionRepositoryHelper(this);

        this.restTemplate = new RestTemplate();

        GsonHttpMessageConverter converter = new GsonHttpMessageConverter();
        // 如果server返回的html的Content Type不是application/json，添加下面一句，切记切记！！！
//        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.TEXT_PLAIN));
        restTemplate.getMessageConverters().add(converter);

        // The HttpComponentsClientHttpRequestFactory uses the
        // org.apache.http package to make network requests
        restTemplate
                .setRequestFactory(new HttpComponentsClientHttpRequestFactory(
                        HttpUtils.getNewHttpClient()));
    }

    // ***************************************
    // Private methods
    // ***************************************
    private String getBaseUrl() {
        return getString(R.string.base_url);
    }

    private String getBasePath() {
        return getString(R.string.base_path);
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public URI createGetUrl(String method, Map<String, Object> paras) {

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(getBaseUrl())
                .path(getBasePath())
                .queryParam(Message.ATT_METHOD, method);

        for(String key : paras.keySet()) {
            builder.queryParam(key, paras.get(key));
        }

        if(method.compareToIgnoreCase(Message.ACT_LOGIN) != 0) {
            builder.queryParam(Message.ATT_ACCOUNT, account);
            builder.queryParam(Message.ATT_TOKEN, token);
        }

        URI targetUrl= builder.build().toUri();

        return targetUrl;
    }

    public void setToken(String token) {
        this.token = token;
    }
    public void setAccount(String account) {
        this.account = account;
    }
    public String getAccount() {
        return account;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public static Context getContext() {
        return sContext;
    }

}
