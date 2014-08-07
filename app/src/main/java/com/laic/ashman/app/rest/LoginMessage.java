package com.laic.ashman.app.rest;

/**
 * Created by duduba on 14-7-20.
 */

public class LoginMessage extends Message {

    private String token;

    public LoginMessage(int status, String message) {
        super(status, message);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
