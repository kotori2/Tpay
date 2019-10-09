package com.sjk.tpay.po;

import com.alibaba.fastjson.annotation.JSONField;

public class AuthorizeBean {
    @JSONField(name = "sessionId")
    private String sessionID;

    public void setSessionID(String i){
        sessionID = i;
    }
    public String getSessionID(){
        return sessionID;
    }
}
