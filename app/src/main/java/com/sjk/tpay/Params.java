package com.sjk.tpay;

import com.sjk.tpay.utils.LogUtils;

public class Params {
    private static Params mParams;
    private Boolean mIsRunning = false;

    public synchronized static Params getInstance(){
        if(mParams == null){
            LogUtils.show("新建mParams");
            mParams = new Params();
        }
        return mParams;
    }

    public Boolean getStatus(){
        return mIsRunning;
    }

    public void changeStatus(){
        String before;
        String after;
        if(mIsRunning){
            before = "true";
            after = "false";
        }else{
            before = "false";
            after = "true";
        }
        LogUtils.show("changeStatus " + before + " to " + after);
        mIsRunning = !mIsRunning;
    }

    public void setStatus(Boolean status){
        mIsRunning = status;
    }
}
