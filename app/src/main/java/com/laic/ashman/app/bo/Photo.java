package com.laic.ashman.app.bo;

/**
 * Created by duduba on 14-5-7.
 */
public class Photo {

    private int id;

    private String taskId;
    private String name;
    private int upflag = 0;

    public int getUpflag() {
        return upflag;
    }

    public void setUpflag(int upflag) {
        this.upflag = upflag;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
