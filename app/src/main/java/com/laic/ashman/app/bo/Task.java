package com.laic.ashman.app.bo;

import com.laic.ashman.app.provider.TaskTable;

/**
 * Created by duduba on 14-5-7.
 */
public class Task {

    private int id;
    private int taskZt;
    private String taskId;
    private String sjjssj;
    private String kssj;
    private String dxcsj;
    private String jssj;
    private String eventId;
    private String sjms;
    private String sjlx;
    private String jjsj;
    private String dsrdh;
    private String sjcph;
    private String sjfx;
    private String sjzh;
    private double pointx;
    private double pointy;
    private String cqcl;
    private String cqry;
    private String cqrydh;
    private String bz;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTaskZt() {
        return taskZt;
    }

    public void setTaskZt(int taskZt) {
        this.taskZt = taskZt;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getSjjssj() {
        return sjjssj;
    }

    public void setSjjssj(String sjjssj) {
        this.sjjssj = sjjssj;
    }

    public String getKssj() {
        return kssj;
    }

    public void setKssj(String kssj) {
        this.kssj = kssj;
    }

    public String getDxcsj() {
        return dxcsj;
    }

    public void setDxcsj(String dxcsj) {
        this.dxcsj = dxcsj;
    }

    public String getJssj() {
        return jssj;
    }

    public void setJssj(String jssj) {
        this.jssj = jssj;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getSjms() {
        return sjms;
    }

    public void setSjms(String sjms) {
        this.sjms = sjms;
    }

    public String getSjlx() {
        return sjlx;
    }

    public void setSjlx(String sjlx) {
        this.sjlx = sjlx;
    }

    public String getJjsj() {
        return jjsj;
    }

    public void setJjsj(String jjsj) {
        this.jjsj = jjsj;
    }

    public String getDsrdh() {
        return dsrdh;
    }

    public void setDsrdh(String dsrdh) {
        this.dsrdh = dsrdh;
    }

    public String getSjcph() {
        return sjcph;
    }

    public void setSjcph(String sjcph) {
        this.sjcph = sjcph;
    }

    public String getSjfx() {
        return sjfx;
    }

    public void setSjfx(String sjfx) {
        this.sjfx = sjfx;
    }

    public String getSjzh() {
        return sjzh;
    }

    public void setSjzh(String sjzh) {
        this.sjzh = sjzh;
    }

    public double getPointx() {
        return pointx;
    }

    public void setPointx(double pointx) {
        this.pointx = pointx;
    }

    public double getPointy() {
        return pointy;
    }

    public void setPointy(double pointy) {
        this.pointy = pointy;
    }

    public String getCqcl() {
        return cqcl;
    }

    public void setCqcl(String cqcl) {
        this.cqcl = cqcl;
    }

    public String getCqry() {
        return cqry;
    }

    public void setCqry(String cqry) {
        this.cqry = cqry;
    }

    public String getCqrydh() {
        return cqrydh;
    }

    public void setCqrydh(String cqrydh) {
        this.cqrydh = cqrydh;
    }

    public String getBz() {
        return bz;
    }

    public void setBz(String bz) {
        this.bz = bz;
    }

    public boolean isNewTask() { return taskZt == TaskTable.TASK_NEW; }
    public boolean isStarted() { return taskZt == TaskTable.TASK_START; }
    public boolean isArrived() { return taskZt == TaskTable.TASK_ARRIVE; }
    public boolean isFinished() { return taskZt == TaskTable.TASK_FINISH; }
    public boolean isRunning() {
        return (isStarted() || isArrived());
    }
}
