package com.laic.ashman.app.rest;

import java.io.Serializable;

/**
 * Created by duduba on 14-7-20.
 */

public class Message implements Serializable {
    public final static int OK = 0;
    public final static int NETERR = -1000;

    public final static String ACT_LOGIN = "login";
    public final static String ACT_GETTASK = "tasklist";
    public final static String ACT_START = "depart";
    public final static String ACT_ARRIVE = "arrived";
    public final static String ACT_FINISH = "finished";
    public final static String ACT_POSITION = "position";
    public final static String ACT_REPORT = "report";
    public final static String ACT_PHOTO = "uploadImage";

    public final static String ATT_METHOD = "method";
    public final static String ATT_STATUS = "status";
    public final static String ATT_ACCOUNT = "account";
    public final static String ATT_TOKEN = "token";

    private int status;
    private String message;

    public Message(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isOk() {
        return status == OK;
    }
}
