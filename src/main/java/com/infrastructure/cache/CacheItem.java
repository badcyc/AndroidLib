package com.infrastructure.cache;

import java.io.Serializable;

/**
 * Created by cyc20 on 2018/3/4.
 */

public class CacheItem implements Serializable{
    /**存储的的key**/
    private String key;
    /**json字符串**/
    private String data;

    /**过期时间的时间戳 =存入时间+url过期时间长度; 当前时间与此时间戳比较以判断是否获取缓存*/
     private long timeStamp=0;

     public CacheItem(String key,String data,long expiredTime){
         this.key=key;
         this.data=data;
         this.timeStamp=System.currentTimeMillis()+expiredTime*1000;
     }

    public String getKey() {
        return key;
    }

    public String getData() {
        return data;
    }

    public long getTimeStamp() {
        return timeStamp;
    }
}
