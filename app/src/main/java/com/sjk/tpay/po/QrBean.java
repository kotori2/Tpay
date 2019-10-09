package com.sjk.tpay.po;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;


/**
 * @ Created by Dlg
 * @ <p>TiTle:  QrBean</p>
 * @ <p>Description: 这个最基本的二维码信息Bean类了，处理不返回null</p>
 * @ date:  2018/9/21
 * @ QQ群：524901982
 */
public class QrBean {

    //以后自己可以添加更多支付方式，没必要用枚举
    public static final int WECHAT = 1;
    public static final int ALIPAY = 2;


    /**
     * 任务ID
     */
    @JSONField(name = "task_id")
    private int taskID;

    /**
     * 渠道类型
     */
    @JSONField(name = "channel")
    private int channel;//WECHAT,ALIPAY

    /**
     * 二维码的金额,单位为分
     */
    @JSONField(name = "amount")
    private int amount;


    /**
     * 订单id，用于备注
     */
    @JSONField(name = "order_id")
    private String orderID;


    public int getTaskID() { return taskID; }

    public void setTaskID(int taskID) { this.taskID = taskID; }

    public int getChannel() {
        return channel;
    }

    public String getChannelString() {
        switch (channel) {
            case WECHAT:
                return "WECHAT";
            case ALIPAY:
                return "ALIPAY";
            default:
                return "UNKNOWN";
        }
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getOrderID() {
        return orderID == null ? "" : orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
