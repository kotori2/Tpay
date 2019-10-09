package com.sjk.tpay;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.WindowManager;

import com.sjk.tpay.imp.CallBackDo;
import com.sjk.tpay.utils.LogUtils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class HookAlipay extends HookBase {

    private static HookAlipay mHookAlipay;
    private String setMoneyActivity = "com.alipay.mobile.payee.ui.PayeeQRSetMoneyActivity";

    public static synchronized HookAlipay getInstance() {
        if (mHookAlipay == null) {
            mHookAlipay = new HookAlipay();
        }
        return mHookAlipay;
    }

    @Override
    public void hookFirst() throws Error, Exception {
    }

    @Override
    public void hookCreatQr() throws Error, Exception {

    }

    @Override
    public void hookBill() throws Error, Exception {

    }

    @Override
    public void addRemoteTaskI() {
        addRemoteTask(getRemoteQrActionType(), new CallBackDo() {
            @Override
            public void callBack(Intent intent) throws Error, Exception {
                LogUtils.show("获取支付宝二维码");
                Intent intent2 = new Intent(mContext, XposedHelpers.findClass(
                        setMoneyActivity, mContext.getClassLoader()));
                intent2.putExtra("mark", intent.getStringExtra("mark"));
                intent2.putExtra("money", intent.getStringExtra("money"));
                intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent2);
            }
        });
    }

    @Override
    public String getPackPageName() {
        return "com.eg.android.AlipayGphone";
    }

    @Override
    public String getAppName() {
        return "支付宝";
    }
}
