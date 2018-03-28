package com.infrastructure.utils;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by cyc20 on 2018/3/4.
 */

public class EncryptUtils {
    /**
     * -----------------Base64-----------------
     */
    /**
     * BASE64 加密 android platform
     */
    public static String encryptBASE64(String originalText)throws Exception{
        byte[] bytes=originalText.getBytes();
        return Base64.encodeToString(bytes,Base64.DEFAULT);
    }
    /**
     * BASE64 解密 android platform
     */
    public static String decryptBASE64(String key)throws Exception{
        byte[] bytes=Base64.decode(key,Base64.DEFAULT);
        return new String(bytes);
    }
    public static final String KEY_MD5 = "MD5";
    public static final String KEY_SHA1 = "SHA-1";
    public static final String KEY_SHA3 = "SHA-3";// null
    public static final String KEY_SHA256 = "SHA-256";
    public static final String KEY_SHA384 = "SHA-384";
    public static final String KEY_SHA512 = "SHA-512";
    public static final String KEY_SHA224 = "SHA-224";

    /**
     * MD5 加密 小32
     */
    public static String encryptMD5L32(String originalText) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return encryptHash(originalText, KEY_MD5);
    }

    /**
     * MD5 加密 大32
     */
    public static String encryptMD5U32(String originalText) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String encryptText = encryptMD5L32(originalText);
        return encryptText.toUpperCase();
    }

    /**
     * MD5 加密 大16
     */
    public static String encryptMD5U16(String originalText) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String encryptText = encryptMD5L32(originalText);
        return encryptText.toUpperCase().substring(8, 24);
    }

    /**
     * MD5 加密 小16
     */
    public static String encryptMD5L16(String originalText) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String encryptText = encryptMD5L32(originalText);
        return encryptText.substring(8, 24);
    }




    /**
     * -------------------SHA-------------------
     */


    /**
     * SHA1 加密 不安全
     *
     * @param originalText 待加密原文
     * @return 加密后的十六进制字符串
     * @throws NoSuchAlgorithmException
     */
    public static String encryptSHA1(String originalText) throws NoSuchAlgorithmException {
        return encryptHash(originalText, KEY_SHA1);
    }

    /**
     * SHA256 加密 安全
     *
     * @param originalText 待加密原文
     * @return 加密后的十六进制字符串
     * @throws NoSuchAlgorithmException
     */
    public static String encryptSHA256(String originalText) throws NoSuchAlgorithmException {
        return encryptHash(originalText, KEY_SHA256);
    }

    /**
     * SHA384 加密 安全
     *
     * @param originalText 待加密原文
     * @return 加密后的十六进制字符串
     * @throws NoSuchAlgorithmException
     */
    public static String encryptSHA384(String originalText) throws NoSuchAlgorithmException {
        return encryptHash(originalText, KEY_SHA384);
    }

    /**
     * SHA512 加密 安全
     *
     * @param originalText 待加密原文
     * @return 加密后的十六进制字符串
     * @throws NoSuchAlgorithmException
     */
    public static String encryptSHA512(String originalText) throws NoSuchAlgorithmException {
        return encryptHash(originalText, KEY_SHA512);
    }

    /**
     * @param originalText 待加密原文
     * @param key          eg: MD5 SHA(SHA-1) SHA-256 ...
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static String encryptHash(String originalText, String key) throws NoSuchAlgorithmException {
        MessageDigest digester = MessageDigest.getInstance(key);
        digester.update(originalText.trim().getBytes());
        byte[] bytes = digester.digest();

        return parseBytesToHexString(bytes);
    }




    /**
     * -------------------HMAC-------------------
     */

    public static final String KEY_HMACSHA256 = "HmacSHA256";
    public static final String KEY_HMACMD5 = "HmacMD5";

    /**
     * HmacSHA256 加密
     */
    public static byte[] encryptHMACSHA256(byte[] data, byte[] key) throws Exception {
        return encryptHMAC(data, key, KEY_HMACSHA256);
    }

    /**
     * HmacSHA256 加密
     */
    public static byte[] initHMACSHA256Key() throws Exception {
        return initMacKey(KEY_HMACSHA256);
    }

    /**
     * 初始化HMAC密钥
     * @param key_mac hmac算法名
     * @return hmac秘钥数组
     * @throws Exception
     */
    public static byte[] initMacKey(String key_mac) throws Exception {
        //初始化KeyGenerator
        KeyGenerator keyGenerator = KeyGenerator.getInstance(key_mac);
        //产生密钥
        SecretKey secretKey = keyGenerator.generateKey();
        //获取密钥
        return secretKey.getEncoded();
    }

    /**
     * HMAC 加密
     * @param data 加密原文
     * @param key 秘钥的字节数组
     * @param key_mac 算法名
     * @return 加密的HMAC
     * @throws Exception
     */
    public static byte[] encryptHMAC(byte[] data, byte[] key, String key_mac) throws Exception {
        //还原密钥
        SecretKey restoreSecretKey = new SecretKeySpec(key, key_mac);
        //实例化MAC
        Mac mac = Mac.getInstance(restoreSecretKey.getAlgorithm());
        //初始化MAC
        mac.init(restoreSecretKey);
        //执行摘要
        return mac.doFinal(data);
    }





    /**
     * -------------------AES-------------------
     */

    /**
     * 密钥算法
     */
    private static final String KEY_ALGORITHM = "AES";
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/CBC/NoPadding";

    /**
     * 加密
     */
    public static byte[] encryptAES(byte[] data, byte[] keyBytes) throws Exception {
        //还原密钥
        SecretKey key = new SecretKeySpec(keyBytes, KEY_ALGORITHM);

        //加密
        Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
        byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        IvParameterSpec ivspec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivspec);
        return cipher.doFinal(data);
    }

    /**
     * 解密
     */
    public static byte[] decryptAES(byte[] data, byte[] keyBytes) throws Exception {
        //还原密钥
        SecretKey key = new SecretKeySpec(keyBytes, KEY_ALGORITHM);

        //解密
        Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
        byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        IvParameterSpec ivspec = new IvParameterSpec(iv);// 防止参数错误....
        cipher.init(Cipher.DECRYPT_MODE, key, ivspec);
        return cipher.doFinal(data);
    }

    /**
     * 生成密钥
     */
    public static byte[] initAES256Key(String seed) throws Exception {
        //生成KEY
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
        if(seed == null) {
            keyGenerator.init(256);
        } else {
            keyGenerator.init(256, new SecureRandom(seed.getBytes()));
        }
        //产生密钥
        SecretKey secretKey = keyGenerator.generateKey();
        //获取密钥
        return secretKey.getEncoded();
    }

    /**
     * @param bytes 非空字节数组
     * @return 十六进制字符串
     */
    public static String parseBytesToHexString(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : bytes) {
            int bt = b & 0xff;// int & int 自动转换成int,然后计算
            if (bt < 16) {
                stringBuilder.append(0);
            }
            stringBuilder.append(Integer.toHexString(bt));
        }

        return stringBuilder.toString();
    }

    public static void printBytes(String tag, byte[] bytes) {
        System.out.print(tag + ":");
        for(byte b : bytes) {
            System.out.print(b + ",");
        }
        System.out.println();
    }

    public static void printChars(String tag, char[] chars) {
        System.out.print(tag + ":");
        for(char b : chars) {
            System.out.print(b + ",");
        }
        System.out.println();
    }
}
