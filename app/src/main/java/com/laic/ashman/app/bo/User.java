package com.laic.ashman.app.bo;


/**
 * Created by duduba on 14-4-17.
 */
public class User {
    private String zh;
    private String xm;

    public User(String zh, String xm) {
        this.zh = zh;
        this.xm = xm;
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
