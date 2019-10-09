package com.sjk.tpay.po;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;

public class QrResultBean {
    @JSONField(name = "task_id")
    private int taskID;
    @JSONField(name = "url")
    private String URL;

    public void setTaskID(int taskID){ this.taskID = taskID;}
    public int getTaskID(){ return taskID;}
    public void setURL(String URL){ this.URL = URL;}
    public String getURL(){ return URL;}

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
