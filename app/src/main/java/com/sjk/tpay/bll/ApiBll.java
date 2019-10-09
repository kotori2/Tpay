package com.sjk.tpay.bll;


import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.sjk.tpay.HKApplication;
import com.sjk.tpay.HookAlipay;
import com.sjk.tpay.HookWechat;
import com.sjk.tpay.Security;
import com.sjk.tpay.po.AuthorizeBean;
import com.sjk.tpay.po.BaseMsg;
import com.sjk.tpay.po.Configure;
import com.sjk.tpay.po.PaySuccessBean;
import com.sjk.tpay.po.QrBean;
import com.sjk.tpay.po.QrResultBean;
import com.sjk.tpay.request.FastJsonRequest;
import com.sjk.tpay.utils.LogUtils;
import com.sjk.tpay.utils.SaveUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @ Created by Dlg
 * @ <p>TiTle:  ApiBll</p>
 * @ <p>Description: 和服务端交互的业务类</p>
 * @ date:  2018/9/21
 * @ QQ群：524901982
 */
public class ApiBll {
    private static ApiBll mApiBll;
    private static Boolean mAuthorized = false;

    private RequestQueue mQueue;

    public static ApiBll getInstance() {
        if (mApiBll == null) {
            mApiBll = new ApiBll();
            mApiBll.mQueue = Volley.newRequestQueue(HKApplication.app);
        }
        return mApiBll;
    }

    public Boolean isAuthorized(){
        return mAuthorized;
    }

    /**
     * 检查是否需要发送新二维码
     */
    public void checkQR() {
        if (!Configure.getInstance().getUrl().toLowerCase().startsWith("http")) {
            return;//防止首次启动还没有配置，就一直去轮循
        }

        mQueue.add(new FastJsonRequest("/checkTask", new JSONObject(), succ, fail));
        mQueue.start();
    }


    /**
     * 发送服务器所需要的二维码字符串给服务器
     * 服务器如果有新订单，就会立马返回新的订单，手机端就不用再等下次轮循了
     *
     * @param qrResultBean 要发送的二维码参数
     */
    public void sendQR(QrResultBean qrResultBean) {
        JSONObject body = new JSONObject();
        body.put("task_id", qrResultBean.getTaskID());
        body.put("url", qrResultBean.getURL());
        mQueue.add(new FastJsonRequest("/submitTask", body, succ, fail));
        mQueue.start();
        dealTaskList();
        LogUtils.show("发送二维码：" + qrResultBean.toString());
    }

    public void authorize(){
        mQueue.add(new FastJsonRequest("/authorize", Security.initRequest(), new Response.Listener<BaseMsg>(){
            @Override
            public void onResponse(BaseMsg response){
                AuthorizeBean b = response.getData(AuthorizeBean.class);
                if(b != null){
                    FastJsonRequest.setSessionID(b.getSessionID());
                    mAuthorized = true;
                }else{
                    LogUtils.show("Authorize json parse failed!");
                }

            }
        }, fail));

    }

    public void unAuthorize(){mAuthorized = false;}


    /**
     * 向服务器发送支付成功的消息
     * 如果因为一些原因，暂时没有通知成功，会保存任务，下次再尝试
     *
     * @param successBean 订单详情信息
     */
    public void payQR(final PaySuccessBean successBean) {
        JSONObject body = new JSONObject();
        body.put("channel", successBean.getChannel());
        body.put("amount", successBean.getAmount());
        body.put("merchant_order_id", successBean.getMerchantOrderId());
        body.put("message_sell", successBean.getMessageSell());
        body.put("message_buy", successBean.getMessageBuy());
        mQueue.add(new FastJsonRequest("/finishTask", body, succ, fail));
        mQueue.start();

        LogUtils.show("发送订单: " + successBean.getMessageSell());
    }


    /**
     * 处理以前没有完成的任务 （特指订单完成任务）
     */
    private void dealTaskList() {
        SaveUtils saveUtils = new SaveUtils();
        List<PaySuccessBean> list = saveUtils.getJsonArray(SaveUtils.TASK_LIST, PaySuccessBean.class);
        if (list != null) {
            //先清空任务，如果待会儿在payQR里又失败的话，会自动又添加的。
            saveUtils.putJson(SaveUtils.TASK_LIST, null).commit();
            for (PaySuccessBean successBean : list) {
                payQR(successBean);
            }
        }
    }


    /**
     * 添加未完成的任务列表
     * 一定要用static的synchronized方式，上面的dealTaskList在某情况下可能会有问题
     * 但个人方案就暂不考虑这么极端的情况了
     *
     * @param successBean
     */
    private synchronized static void addTaskList(PaySuccessBean successBean) {
        SaveUtils saveUtils = new SaveUtils();
        List<PaySuccessBean> list = saveUtils.getJsonArray(SaveUtils.TASK_LIST, PaySuccessBean.class);
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(successBean);
        saveUtils.putJson(SaveUtils.TASK_LIST, list).commit();
    }


    /**
     * 当询问是否需要生成二维码返回成功后的操作
     */
    private final Response.Listener<BaseMsg> succ = new Response.Listener<BaseMsg>() {
        @Override
        public void onResponse(BaseMsg response) {
            if (response == null) {
                return;
            }
            QrBean qrBean = response.getData(QrBean.class);
            if (qrBean != null && qrBean.getAmount() > 0) {
                LogUtils.show("服务器需要新二维码：" + qrBean.getAmount() + "|" + qrBean.getOrderID() + "|" + qrBean.getChannelString());
                String comment = String.format("订单 {%s}", qrBean.getOrderID());
                switch (qrBean.getChannel()) {
                    case QrBean.WECHAT:
                        HookWechat.getInstance().creatQrTask(qrBean.getAmount(), comment, qrBean.getTaskID());
                        break;
                    case QrBean.ALIPAY:
                        HookAlipay.getInstance().creatQrTask(qrBean.getAmount(), comment, qrBean.getTaskID());
                        break;
                    default:
                        LogUtils.show("Unknown channel! Request skipped.");
                }
            }
        }
    };


    private final Response.ErrorListener fail = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            if(error.networkResponse != null){
                int statusCode = error.networkResponse.statusCode;
                if(statusCode == 400 || statusCode == 403){
                    LogUtils.show("Revoke session key");
                    unAuthorize();
                    FastJsonRequest.setSessionID(null);
                }
                LogUtils.show("Network error " + statusCode + " at ApiBll");

                //LogUtils.show(new String(error.networkResponse.data));
            }else{
                LogUtils.show("Network error at ApiBll");
                //LogUtils.show(error.getMessage());
            }
            LogUtils.show(error.getMessage());
        }
    };

}
