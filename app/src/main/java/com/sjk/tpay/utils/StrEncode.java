package com.sjk.tpay.utils;


import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

/**
 * @ Created by Dlg
 * @ <p>TiTle:  StrEncode</p>
 * @ <p>Description: 字符串的DES对称加密类，和php端是相同方案的des</p>
 * @ date:  2018/09/17
 * @ QQ群：524901982
 */
public class StrEncode {
    /**
     * DES加密
     *
     * @param plainText 明文
     * @param pass      密码
     * @return
     */
    @Deprecated
    public static String encoderByDES(String plainText, String pass) {
        try {
            byte[] result = coderByDES(plainText.getBytes("UTF-8"), pass,
                    Cipher.ENCRYPT_MODE);
            return byteArr2HexStr(result);
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    /**
     * DES解密
     *
     * @param secretText 密文
     * @param pass       密码
     * @return @NotNull 失败返回空的文本，非Null
     */
    @Deprecated
    public static String decoderByDES(String secretText, String pass) {
        try {
            byte[] result = coderByDES(hexStr2ByteArr(secretText), pass,
                    Cipher.DECRYPT_MODE);
            return new String(result, "UTF-8");
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    /** */
    /**
     * DES加解密
     *
     * @param plainText 要处理的byte[]
     * @param key       密钥
     * @param mode      模式
     * @return
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws UnsupportedEncodingException
     */
    @Deprecated
    private static byte[] coderByDES(byte[] plainText, String key, int mode)
            throws InvalidKeyException, InvalidKeySpecException,
            NoSuchAlgorithmException, NoSuchPaddingException,
            BadPaddingException, IllegalBlockSizeException,
            UnsupportedEncodingException {
        SecureRandom sr = new SecureRandom();
        byte[] resultKey = makeKey(key);
        DESKeySpec desSpec = new DESKeySpec(resultKey);
        SecretKey secretKey = SecretKeyFactory.getInstance("DES")
                .generateSecret(desSpec);
        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(mode, secretKey, sr);
        return cipher.doFinal(plainText);
    }

    /** */
    /**
     * 生产8位的key
     *
     * @param key 字符串形式
     * @return
     * @throws UnsupportedEncodingException
     */
    private static byte[] makeKey(String key)
            throws UnsupportedEncodingException {
        byte[] keyByte = new byte[8];
        byte[] keyResult = key.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < keyResult.length && i < keyByte.length; i++) {
            keyByte[i] = keyResult[i];
        }
        return keyByte;
    }

    /** */
    /**
     * 将byte数组转换为表示16进制值的字符串， 如：byte[]{8,18}转换为：0813， 和public static byte[]
     * hexStr2ByteArr(String strIn) 互为可逆的转换过程
     *
     * @param arrB 需要转换的byte数组
     * @return 转换后的字符串
     */
    public static String byteArr2HexStr(byte[] arrB) {
        int iLen = arrB.length;
        // 每个byte用两个字符才能表示，所以字符串的长度是数组长度的两倍
        StringBuilder sb = new StringBuilder(iLen * 2);
        for (int i = 0; i < iLen; i++) {
            int intTmp = arrB[i];
            // 把负数转换为正数
            while (intTmp < 0) {
                intTmp = intTmp + 256;
            }
            // 小于0F的数需要在前面补0
            if (intTmp < 16) {
                sb.append("0");
            }
            sb.append(Integer.toString(intTmp, 16));
        }
        return sb.toString();
    }

    /** */
    /**
     * 将表示16进制值的字符串转换为byte数组， 和public static String byteArr2HexStr(byte[] arrB)
     * 互为可逆的转换过程
     *
     * @param strIn 需要转换的字符串
     * @return 转换后的byte数组
     * @throws NumberFormatException
     */
    private static byte[] hexStr2ByteArr(String strIn)
            throws NumberFormatException {
        byte[] arrB = strIn.getBytes();
        int iLen = arrB.length;
        // 两个字符表示一个字节，所以字节数组长度是字符串长度除以2
        byte[] arrOut = new byte[iLen / 2];
        for (int i = 0; i < iLen; i = i + 2) {
            String strTmp = new String(arrB, i, 2);
            arrOut[i / 2] = (byte) Integer.parseInt(strTmp, 16);
        }
        return arrOut;
    }


}



