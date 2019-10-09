package com.sjk.tpay.po;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;

public class PaySuccessBean {
    @JSONField(name = "channel")
    private int channel;
    @JSONField(name = "amount")
    private int amount;
    @JSONField(name = "merchant_order_id")
    private String merchantOrderId;
    @JSONField(name = "message_sell")
    private String messageSell;
    @JSONField(name = "message_buy")
    private String messageBuy;

    public String getMessageSell() {
        return messageSell;
    }

    public void setMessageSell(String messageSell) {
        this.messageSell = messageSell;
    }

    public String getMessageBuy() {
        return messageBuy;
    }

    public void setMessageBuy(String messageBuy) {
        this.messageBuy = messageBuy;
    }

    public String getMerchantOrderId() {
        return merchantOrderId;
    }

    public void setMerchantOrderId(String merchantOrderId) {
        this.merchantOrderId = merchantOrderId;
    }



    public void setChannel(int channel){ this.channel = channel;}
    public int getChannel(){ return channel;}
    public void setAmount(int amount){ this.amount = amount;}
    public int getAmount(){ return amount;}

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
