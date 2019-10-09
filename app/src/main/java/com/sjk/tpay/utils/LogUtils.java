package com.sjk.tpay.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import de.robv.android.xposed.XposedBridge;

/**
 * @ Created by Dlg
 * @ <p>TiTle:  LogUtils</p>
 * @ <p>Description: 懒得去判断这个日志是哪个进程发送的了，统一下日志接口吧。</p>
 * @ date:  2018/9/22
 * @ QQ群：524901982
 */
public class LogUtils {

    public static void show(String data) {
        try {
            XposedBridge.log(getFunctionName() + data);
        } catch (NoClassDefFoundError ignore) {

        }
        Log.d("LogUtils", getFunctionName() + data);
    }

    private static String getFunctionName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace != null) {
            for (StackTraceElement stackTraceElement : stackTrace) {
                if (!stackTraceElement.isNativeMethod() && !stackTraceElement.getClassName().equals(Thread.class.getName())
                        && !stackTraceElement.getFileName().contentEquals("LogUtils.java")) {
                    return "[ " + Thread.currentThread().getName() + ": " + stackTraceElement.getFileName()
                            + ":" + stackTraceElement.getLineNumber() + " " + stackTraceElement.getMethodName() + " ]";
                }
            }
        }
        return null;
    }
}
