package com.infrastructure.cache;

import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import com.infrastructure.utils.BaseUtils;
import com.infrastructure.utils.EncryptUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by cyc20 on 2018/3/4.
 */

public class CacheManager {
    public static final String APP_CACHE_PATH = Environment.getExternalStorageDirectory().getPath() + "/cyc/appdata/";
    /*
    * 内部存储最小限制空间，小于此空间不再接收缓存
    * */
    private static final long STORAGE_MIN_SPACE = 1024 * 1024 * 10;

    private static CacheManager instance;

    private CacheManager() {

    }

    public static CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }

    //    初始化缓存目录
    public void initCacheDir() {

    }

    //    存储缓存数据到文件
    public boolean putFileCache(String key, String data, long expiredTime) {
        String md5Key;
        try {
            md5Key = EncryptUtils.encryptMD5L32(key);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e2) {
            e2.printStackTrace();
            return false;
        }
        return putIntoCache(new CacheItem(md5Key, data, expiredTime));
    }

    //    获取缓存数据
    public String getFileCache(String key) {
        String md5Key;
        try {
            md5Key = EncryptUtils.encryptMD5L32(key);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        CacheItem item = getFromCache(md5Key);
        return item == null ? null : item.getData();
    }

    //    是否存在缓存数据
    public boolean contains(String key) {
        File file = new File(APP_CACHE_PATH + key);
        return file.exists();
    }

    //    清除缓存文件
    public void clearAllData() {
        File rootFile = new File(APP_CACHE_PATH);
        File[] files = rootFile.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        for (File file : files) {
            if (file != null) {
                file.delete();
            }
        }

    }

    //    直接存储实体
    private synchronized boolean putIntoCache(@Nullable CacheItem item) {
        Log.d("put path", APP_CACHE_PATH + item.getKey());
        return BaseUtils.getAvailDataStorageSize() > STORAGE_MIN_SPACE && BaseUtils.saveObject(APP_CACHE_PATH + item.getKey(), item);
    }

    private synchronized CacheItem getFromCache(String key) {
        Log.d("get path", APP_CACHE_PATH + key);
        Object object = BaseUtils.restoreObject(APP_CACHE_PATH + key);
        if (object == null || !(object instanceof CacheItem)) {
            return null;
        }

        CacheItem cacheItem = (CacheItem) object;
        if (System.currentTimeMillis() > cacheItem.getTimeStamp()) {
            return null;
        }

        return cacheItem;
    }
}
