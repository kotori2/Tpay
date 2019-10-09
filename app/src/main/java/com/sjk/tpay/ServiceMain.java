package com.sjk.tpay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.sjk.tpay.bll.ApiBll;
import com.sjk.tpay.po.Configure;
import com.sjk.tpay.utils.LogUtils;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static java.text.DateFormat.LONG;
import static java.text.DateFormat.getDateTimeInstance;
import static java.text.DateFormat.getTimeInstance;


/**
 * @ Created by Dlg
 * @ <p>TiTle:  ServiceMain</p>
 * @ <p>Description: 这个类就一直轮循去请求是否需要二维码</p>
 * @ date:  2018/09/22
 * @ QQ群：524901982
 */
public class ServiceMain extends Service {
    private static class ServiceHandler extends Handler {
        private final WeakReference<ServiceMain> mService;
        private long lastTime = 0;

        public ServiceHandler(ServiceMain that) {
            mService = new WeakReference<ServiceMain>(that);
        }

        @Override
        public void handleMessage(Message msg) {
            ServiceMain that = mService.get();
            super.handleMessage(msg);

            //防止多次鉴权
            long curTime = System.currentTimeMillis();
            if(curTime - lastTime < 1000){
                LogUtils.show("ServiceMain->handleMessage:handleMessage 两次调用间隔过短");
                return;
            }
            lastTime = curTime;

            //未鉴权
            if(!ApiBll.getInstance().isAuthorized()){
                LogUtils.show("ServiceMain->handleMessage:handleMessage 尝试鉴权");
                if(that.mAuthorizeCnt > 2){
                    LogUtils.show("ServiceMain->handleMessage:handleMessage 鉴权失败三次，停止服务");
                    Params.getInstance().changeStatus();

                    //推送通知
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(that.getApplicationContext(), "tpayPush")
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("鉴权失败")
                            .setContentText("后台服务已停止");

                    //设置点击通知之后的响应，启动SettingActivity类
                    Intent resultIntent = new Intent(that.getApplicationContext(), ActMain.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(that.getApplicationContext(),0,resultIntent,PendingIntent.FLAG_UPDATE_CURRENT);
                    builder.setContentIntent(pendingIntent);
                    Notification notification = builder.build();
                    notification.flags = Notification.FLAG_AUTO_CANCEL;
                    NotificationManager notificationManager = (NotificationManager) that.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.notify(1, notification);

                    that.stopSelf();
                    return;
                }
                ApiBll.getInstance().authorize();
                that.mAuthorizeCnt++;
            }

            if (Params.getInstance().getStatus()) {//停止任务的时候，不会去轮循
                if(ApiBll.getInstance().isAuthorized()){
                    LogUtils.show("ServiceMain->handleMessage:handleMessage 已鉴权，开始轮询");
                    ApiBll.getInstance().checkQR();
                }
            }else{
                that.stopSelf();
                return;
            }
            /*if (that.handler.hasMessages(0)) {
                return;
            }*/
            mLastQueryTime = System.currentTimeMillis();
            //0-7点的时候就慢速轮循
            that.handler.sendEmptyMessageDelayed(0,Calendar.getInstance().get(Calendar.HOUR_OF_DAY) > 7 ? Configure.getInstance().getDelay_nor()
                    : Configure.getInstance().getDelay_slow());
        }
    }

    //是否启动了检测二维码需求的功能
    //public static Boolean mIsRunning = false;

    //上次询问服务器是否需要二维码的时间
    public static long mLastQueryTime = 0;

    //防止被休眠，你们根据情况可以开关，我是一直打开的，有点费电是必然的，哈哈
    private PowerManager.WakeLock mWakeLock;

    private int mAuthorizeCnt = 0;

    Notification noti;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mLastQueryTime = System.currentTimeMillis();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.sjk.tpay:waketag");
        mWakeLock.acquire();

        LogUtils.show("ServiceMain启动");
        addStatusBar();
        if (!handler.hasMessages(0)) {
            handler.sendEmptyMessage(0);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.sendEmptyMessage(0);
        return START_STICKY;
    }

    private Handler handler = new ServiceHandler(this);

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (mWakeLock != null)
                mWakeLock.release();
            mWakeLock = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        stopForeground(STOP_FOREGROUND_REMOVE);
        ApiBll.getInstance().unAuthorize();

        LogUtils.show("ServiceMain服务被杀死");
        if (Params.getInstance().getStatus()) {
            Intent intent = new Intent(this.getApplicationContext(), ServiceMain.class);
            this.startService(intent);
        }else{
            Message msg = new Message();
            msg.what = 1;
            ActMain.handler.sendMessage(msg);
        }
    }


    /**
     * 在状态栏添加图标
     */
    private void addStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel("tpayChannel", "服务状态",
                    NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
            channel = new NotificationChannel("tpayPush", "推送消息",
                    NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);

            Intent intent = new Intent(this, ActMain.class);
            PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
            noti = new Notification.Builder(this, "tpayChannel")
                    .setTicker("程序启动成功")
                    .setContentTitle("后台服务运行中")
                    .setContentText("始于：" + DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT).format(new Date()))
                    .setSmallIcon(R.mipmap.ic_launcher)//TODO:设置图标
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setContentIntent(pi)//点击之后的页面
                    .build();
            startForeground(17952, noti);
        } else {
            Intent intent = new Intent(this, ActMain.class);
            PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
            noti = new Notification.Builder(this)
                    .setTicker("程序启动成功")
                    .setContentTitle("后台服务运行中")
                    .setContentText("始于：" + DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT).format(new Date()))
                    .setSmallIcon(R.mipmap.ic_launcher)//设置图标
                    .setDefaults(Notification.DEFAULT_SOUND)//设置声音
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setContentIntent(pi)//点击之后的页面
                    .build();
            startForeground(17952, noti);
        }
    }
}
