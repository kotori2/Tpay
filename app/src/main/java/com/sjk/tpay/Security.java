package com.sjk.tpay;

public class Security {
    public native static int getVersion();
    public native static String getABI();
    public native static byte[] initRequest();
    public native static byte[] getRequest(String data);
    public native static String getResponse(byte[] data);
}
