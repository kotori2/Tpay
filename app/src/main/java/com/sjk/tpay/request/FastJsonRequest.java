package com.sjk.tpay.request;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.sjk.tpay.po.BaseMsg;
import com.sjk.tpay.po.Configure;
import com.sjk.tpay.utils.LogUtils;
import com.sjk.tpay.utils.StrEncode;

import java.io.UnsupportedEncodingException;
import com.sjk.tpay.Security;

import java.util.HashMap;
import java.util.Map;

/**
 * @ Created by Dlg
 * @ <p>TiTle:  FastJsonRequest</p>
 * @ <p>Description: 和服务器HTTP请求的返回统一构造类
 * @ 统一设置好超时和重试次数，自动添加token和返回序列化好的BaseMsg</p>
 * @ date:  2018/9/30
 */
public class FastJsonRequest extends Request<BaseMsg> {
    private byte[] mRequestBody;
    private final Response.Listener<BaseMsg> mListener;
    private Response.ErrorListener mErrorListener;
    private String uri;
    private static String mSessionID;

    public FastJsonRequest(String uri_, JSONObject body, Response.Listener<BaseMsg> listener, @Nullable Response.ErrorListener errorListener) {
        this(uri_, addCommonBody(body), listener, errorListener);
    }

    public FastJsonRequest(String uri_, String body, Response.Listener<BaseMsg> listener, @Nullable Response.ErrorListener errorListener) {
        this(uri_, Security.getRequest(body), listener, errorListener);
    }

    public FastJsonRequest(String uri_, byte[] body, Response.Listener<BaseMsg> listener, @Nullable Response.ErrorListener errorListener) {
        super(Method.POST, Configure.getInstance().getFullURL(uri_), errorListener);
        mRequestBody = body;
        mListener = listener;
        mErrorListener = errorListener;
        uri = uri_;

        setRetryPolicy(new DefaultRetryPolicy(5000, 0, 0));
    }

    private static String addCommonBody(JSONObject body){
        if(mSessionID != null){
            body.put("sessionId", mSessionID);
        }
        body.put("timeStamp", System.currentTimeMillis());
        LogUtils.show("Request: " + body.toString());
        return body.toString();
    }

    @Override
    public byte[] getBody() {
        return mRequestBody;
    }

    @Override
    protected void deliverResponse(BaseMsg response) {
        // 用监听器的方法来传递下响应的结果
        mListener.onResponse(response);
    }

    @Override
    public void deliverError(VolleyError error) {
        if(mErrorListener != null){
            mErrorListener.onErrorResponse(error);
        }
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/octet-stream");
        if(mSessionID != null){
            headers.put("X-Session-ID", mSessionID);
        }
        headers.putAll(super.getHeaders());
        return headers;
    }

    @Override
    protected Response<BaseMsg> parseNetworkResponse(NetworkResponse response) {
        String result = Security.getResponse(response.data);
        LogUtils.show(result);
        try {
            return Response.success(
                    JSON.parseObject(result, BaseMsg.class), HttpHeaderParser.parseCacheHeaders(response));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }

    public static void setSessionID(String ssid){
        mSessionID = ssid;
    }
}