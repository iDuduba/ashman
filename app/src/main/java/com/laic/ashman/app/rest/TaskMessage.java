package com.laic.ashman.app.rest;

import com.laic.ashman.app.bo.Task;

/**
 * Created by duduba on 14-7-20.
 */

public class TaskMessage extends Message {
    private int total;

    private Task[] data;

    public TaskMessage(int status, String message) {
        super(status, message);
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public Task[] getData() {
        return data;
    }

    public void setData(Task[] data) {
        this.data = data;
    }
}
