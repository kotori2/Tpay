package com.sjk.tpay;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.text.TextUtils;
import android.view.WindowManager;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sjk.tpay.imp.CallBackDo;
import com.sjk.tpay.po.PaySuccessBean;
import com.sjk.tpay.po.QrBean;
import com.sjk.tpay.po.QrResultBean;
import com.sjk.tpay.utils.ReflecUtils;
import com.sjk.tpay.utils.LogUtils;
import com.sjk.tpay.utils.XmlToJson;

import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;


public class HookWechat extends HookBase {

    private static HookWechat mHookWechat;
    private int taskID;

    public static synchronized HookWechat getInstance() {
        if (mHookWechat == null) {
            mHookWechat = new HookWechat();
        }
        return mHookWechat;
    }


    @Override
    public void hookFirst() throws Error, Exception {
        //关屏也能打码，和打码的实现
        hookQRWindows();
    }

    @Override
    public void hookCreatQr() {
        LogUtils.show("HookWechat:hookCreatQr");
        Class<?> clazz = XposedHelpers.findClass("com.tencent.mm.plugin.collect.model.s", mAppClassLoader);
        XposedHelpers.findAndHookMethod(clazz, "onGYNetEnd",
                int.class, String.class, org.json.JSONObject.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param)
                            throws Throwable {
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param)
                            throws Throwable {
                        try {
                            LogUtils.show("com.tencent.mm.plugin.collect.model.s:s {afterHookedMethod}");
                            //QrBean qrBean = new QrBean();
                            //qrBean.setChannel(QrBean.WECHAT);
                            Double amount = ReflecUtils.findField(param.thisObject.getClass(), double.class, 0, false)
                                    .getDouble(param.thisObject);
                            String comment = (String) ReflecUtils.findField(param.thisObject.getClass(), String.class, 1, false)
                                    .get(param.thisObject);
                            String payurl = (String) ReflecUtils.findField(param.thisObject.getClass(), String.class, 2, false)
                                    .get(param.thisObject);

                            LogUtils.show(String.format(Locale.CHINA, "微信成功生成二维码: amount=%s, comment=%s, URL=%s, taskID=%d", amount.toString(), comment, payurl, taskID));

                            QrResultBean qrResultBean = new QrResultBean();
                            qrResultBean.setTaskID(taskID);
                            qrResultBean.setURL(payurl);

                            Intent broadCastIntent = new Intent(RECV_ACTION);
                            broadCastIntent.putExtra(RECV_ACTION_DATA, qrResultBean.toString());
                            broadCastIntent.putExtra(RECV_ACTION_TYPE, getLocalQrActionType());
                            mContext.sendBroadcast(broadCastIntent);
                        } catch (Error | Exception ignore) {
                            ignore.printStackTrace();
                        }
                    }
                });
    }

    @Override
    public void hookBill() throws Error, Exception {
        XposedHelpers.findAndHookMethod("com.tencent.wcdb.database.SQLiteDatabase",
                mAppClassLoader, "insert", String.class, String.class, ContentValues.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param)
                            throws Throwable {
                        try {
                            ContentValues contentValues = (ContentValues) param.args[2];
                            String tableName = (String) param.args[0];
                            if (TextUtils.isEmpty(tableName) || !tableName.equals("message")) {
                                return;
                            }
                            Integer type = contentValues.getAsInteger("type");
                            if (type != null && type == 318767153) {
                                JSONObject msg = XmlToJson.documentToJSONObject(contentValues.getAsString("content"))
                                        .getJSONObject("appmsg");
                                LogUtils.show(msg.toString());
                                if (!msg.getString("type").equals("5")) {
                                    //收款类型type为5
                                    return;
                                }
                                PaySuccessBean paySuccessBean = new PaySuccessBean();
                                paySuccessBean.setChannel(QrBean.WECHAT);
                                paySuccessBean.setAmount((int) (Float.valueOf(msg.getJSONObject("mmreader")
                                        .getJSONObject("template_detail")
                                        .getJSONObject("line_content")
                                        .getJSONObject("topline")
                                        .getJSONObject("value")
                                        .getString("word")
                                        .replace("￥", "")) * 100));

                                paySuccessBean.setMerchantOrderId(msg.getString("template_id"));
                                JSONArray lines = msg.getJSONObject("mmreader")
                                        .getJSONObject("template_detail")
                                        .getJSONObject("line_content")
                                        .getJSONObject("lines")
                                        .getJSONArray("line");

                                for (int i = 0; i < 2; i++) {
                                    if (lines.size() < i + 1 && lines.getJSONObject(i) == null) {
                                        break;
                                    }
                                    if (lines.getJSONObject(i)
                                            .getJSONObject("key")
                                            .getString("word").contains("付款方")) {
                                        paySuccessBean.setMessageBuy(lines.getJSONObject(i)
                                                .getJSONObject("value")
                                                .getString("word"));
                                    } else if (lines.getJSONObject(i)
                                            .getJSONObject("key")
                                            .getString("word").contains("收款方")) {
                                        paySuccessBean.setMessageSell(lines.getJSONObject(i)
                                                .getJSONObject("value")
                                                .getString("word"));
                                    }
                                }
                                if (TextUtils.isEmpty(paySuccessBean.getMessageSell())) {
                                    return;
                                }

                                LogUtils.show("微信收到支付订单：" + paySuccessBean.getAmount() + "|" + paySuccessBean.getMessageSell() + "|" + paySuccessBean.getMessageBuy());

                                Intent broadCastIntent = new Intent(RECV_ACTION);
                                broadCastIntent.putExtra(RECV_ACTION_DATA, paySuccessBean.toString());
                                broadCastIntent.putExtra(RECV_ACTION_TYPE, getLocalBillActionType());
                                mContext.sendBroadcast(broadCastIntent);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    @Override
    public void addRemoteTaskI() {
        addRemoteTask(getRemoteQrActionType(), new CallBackDo() {
            @Override
            public void callBack(Intent intent) throws Error, Exception {
                LogUtils.show("获取微信二维码");
                Intent intent2 = new Intent(mContext, XposedHelpers.findClass(
                        "com.tencent.mm.plugin.collect.ui.CollectCreateQRCodeUI", mContext.getClassLoader()));
                intent2.putExtra("comment", intent.getStringExtra("comment"));
                intent2.putExtra("amount", intent.getStringExtra("amount"));
                intent2.putExtra("taskID", intent.getIntExtra("taskID", 0));
                intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent2); //此处调起CollectCreateQRCodeUI.initView，下方的afterHookedMethod
            }
        });
    }

    @Override
    public void addLocalTaskI() {
        super.addLocalTaskI();
    }

    @Override
    public String getPackPageName() {
        return "com.tencent.mm";
    }

    @Override
    public String getAppName() {
        return "微信";
    }


    private void hookQRWindows() {
        Class<?> clazz = XposedHelpers.findClass("com.tencent.mm.plugin.collect.ui.CollectCreateQRCodeUI", mAppClassLoader);
        XposedBridge.hookAllMethods(clazz, "onCreate", new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    ((Activity) param.thisObject).getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                } catch (Error | Exception ignore) {

                }
            }
        });

        XposedHelpers.findAndHookMethod("com.tencent.mm.plugin.collect.ui.CollectCreateQRCodeUI",
                mAppClassLoader, "initView", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param)
                            throws Throwable {
                        try {
                            Intent intent = ((Activity) param.thisObject).getIntent();
                            String comment = intent.getStringExtra("comment");
                            String amount = intent.getStringExtra("amount");
                            taskID = intent.getIntExtra("taskID", 0);
                            if (TextUtils.isEmpty(comment)) {
                                return;
                            }
                            Class<?> bs = XposedHelpers.findClass("com.tencent.mm.plugin.collect.model.s", mAppClassLoader);
                            Object obj = XposedHelpers.newInstance(bs, Double.valueOf(amount), "1", comment);
                            //XposedBridge.log("com.tencent.mm.plugin.collect.model.s called.");

                            XposedHelpers.callMethod(param.thisObject, "doSceneProgress", obj, true, true, 1);
                        } catch (Error | Exception ignore) {
                            ignore.printStackTrace();
                        }
                    }
                });
    }
}