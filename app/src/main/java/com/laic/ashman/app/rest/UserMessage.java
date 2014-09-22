package com.laic.ashman.app.rest;

import com.laic.ashman.app.bo.Station;
import com.laic.ashman.app.bo.Task;
import com.laic.ashman.app.bo.User;

/**
 * Created by duduba on 14-7-20.
 */

public class UserMessage extends Message {
    private Station[] data;

    public UserMessage(int status, String message) {
        super(status, message);
    }

    public Station[] getData() {
        return data;
    }

    public void setData(Station[] data) {
        this.data = data;
    }
}
