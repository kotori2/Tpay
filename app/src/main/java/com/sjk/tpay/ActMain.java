package com.sjk.tpay;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.textfield.TextInputLayout;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sjk.tpay.bll.ApiBll;
import com.sjk.tpay.po.Configure;
import com.sjk.tpay.request.FastJsonRequest;
import com.sjk.tpay.utils.LogUtils;
import com.sjk.tpay.utils.SaveUtils;
import com.sjk.tpay.utils.StrEncode;

//import static com.sjk.tpay.ServiceMain.mIsRunning;

/**
 * @ Created by Dlg
 * @ <p>TiTle:  ActMain</p>
 * @ <p>Description: 启动首页，直接在xml绑定的监听
 * @ 其实我是不推荐这种绑定方式的，哈哈哈，为了项目简洁点还是就这样吧</p>
 * @ date:  2018/09/11
 * @ QQ群：524901982
 */
public class ActMain extends AppCompatActivity {
    private static class ActivityHandler extends Handler {
        ActMain mContext;
        ActivityHandler(ActMain context){
            mContext = context;
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                mContext.mBtnSubmit.setText("确认配置并启动");break;
                case 2:
                mContext.mBtnSubmit.setText("停止服务");
            }
        }
    }

    public static Handler handler;
    private EditText mEdtUrl;
    private EditText mEdtToken;
    //private EditText mEdtPage;
    private EditText mEdtTimeNor;
    private EditText mEdtTimeSlow;
    private Button mBtnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        System.loadLibrary("security");

        handler = new ActivityHandler(this);
        setContentView(R.layout.act_main);
        mEdtUrl = ((TextInputLayout) findViewById(R.id.edt_act_main_url)).getEditText();
        mEdtToken = ((TextInputLayout) findViewById(R.id.edt_act_main_token)).getEditText();
        //mEdtPage = ((TextInputLayout) findViewById(R.id.edt_act_main_page)).getEditText();
        mEdtTimeNor = ((TextInputLayout) findViewById(R.id.edt_act_main_time_nor)).getEditText();
        mEdtTimeSlow = ((TextInputLayout) findViewById(R.id.edt_act_main_time_slow)).getEditText();
        mBtnSubmit = findViewById(R.id.btn_submit);
        int nativeVersion = Security.getVersion();
        ((TextView) findViewById(R.id.txt_version)).setText(String.format("Version: %s\nNative version: %d on %s", BuildConfig.VERSION_CODE, nativeVersion, Security.getABI()));


        mBtnSubmit.setText(Params.getInstance().getStatus() ? "停止服务" : "确认配置并启动");
    }

    /**
     * 切换APP服务的运行状态
     *
     * @return
     */
    private boolean changeStatus() {
        Params.getInstance().changeStatus();
        mBtnSubmit.setText(Params.getInstance().getStatus() ? "停止服务" : "确认配置并启动");
        FastJsonRequest.setSessionID(null);
        ApiBll.getInstance().unAuthorize();
        return Params.getInstance().getStatus();
    }

    /**
     * 点确认配置的操作
     *
     * @param view
     */
    public void clsSubmit(View view) {
        changeStatus();
        //在运行时，直接改变状态并返回
        if (!Params.getInstance().getStatus()) {
            return;
        }

        mEdtUrl.setText(mEdtUrl.getText().toString().trim());
        mEdtToken.setText(mEdtToken.getText().toString().trim());
        if (mEdtUrl.length() < 2 || mEdtToken.length() < 1
                || mEdtTimeNor.length() < 2 || mEdtTimeSlow.length() < 2) {
            Toast.makeText(ActMain.this, "请先输入正确配置！", Toast.LENGTH_SHORT).show();
            changeStatus();
            return;
        }
        if (mEdtToken.length() != 8) {
            Toast.makeText(ActMain.this, "密钥只能为八位数字或字符！", Toast.LENGTH_SHORT).show();
            changeStatus();
            return;
        }
        if (!mEdtUrl.getText().toString().endsWith("/")) {
            mEdtUrl.setText(mEdtUrl.getText().toString() + "/");//保持以/结尾的网址
        }


        //下面开始获取最新配置并启动服务。
        Configure.getInstance()
                .setUrl(mEdtUrl.getText().toString());
        Configure.getInstance()
                .setToken(mEdtToken.getText().toString());
        Configure.getInstance()
                .setDelay_nor(Integer.valueOf(mEdtTimeNor.getText().toString()));
        Configure.getInstance()
                .setDelay_slow(Integer.valueOf(mEdtTimeSlow.getText().toString()));
        //保存配置
        new SaveUtils().putJson(SaveUtils.BASE, Configure.getInstance()).commit();


        //有的手机就算已经静态注册服务还是不行启动，我再手动启动一下吧。
        startService(new Intent(this, ServiceMain.class));
        startService(new Intent(this, ServiceProtect.class));
    }

    /**
     * 测试微信获取二维码的功能
     *
     * @param view
     */
    public void clsWechatPay(View view) {
        String time = System.currentTimeMillis() / 1000 + "";
        Toast.makeText(this, "只要能打开收款页面即表示成功，并不会输入和生成二维码", Toast.LENGTH_SHORT).show();
        HookWechat.getInstance().creatQrTask(12, "test" + time, 1);
    }


    /**
     * 测试支付宝获取二维码的功能
     *
     * @param view
     */
    public void clsAlipayPay(View view) {
        String time = System.currentTimeMillis() / 1000 + "";
        Toast.makeText(this, "此功能可以加群获取支付宝版哦", Toast.LENGTH_SHORT).show();
        //HookAlipay.getInstance().creatQrTask(12, "test" + time);
    }

    public void debug(View view) {
        /*byte[] req = {};
        for(int i = 0; i < 100000; i++){
            req = Security.initRequest();
        }*/
        byte[] req = Security.initRequest();
        LogUtils.show("RSA Request: " + StrEncode.byteArr2HexStr(req));
        byte[] nml = {};
        String r = "";
        for(int i = 0; i < 100000; i++){
            nml = Security.getRequest("{TESTSTRING}");
            r = Security.getResponse(nml);
        }
        LogUtils.show("AES Resp: " + r);
    }
}
