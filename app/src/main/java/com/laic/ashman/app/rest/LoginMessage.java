package com.laic.ashman.app.rest;

/**
 * Created by duduba on 14-7-20.
 */

public class LoginMessage extends Message {

    private String token;
    private String zh;
    private String xm;

    public LoginMessage(int status, String message) {
        super(status, message);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getZh() {
        return zh;
    }

    public void setZh(String zh) {
        this.zh = zh;
    }

    public String getXm() {
        return xm;
    }

    public void setXm(String xm) {
        this.xm = xm;
    }
}
