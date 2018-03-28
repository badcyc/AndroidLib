package com.infrastructure.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import java.io.File;

/**
 * Created by cyc20 on 2018/3/4.
 */

public class Utils {

    /*
    安全转化字符串
     */
    public final static int covertToInt(Object value,int defaultValue){
        if (value==null||"".equals(value.toString().trim())){
            return defaultValue;
        }
        try{
            return Integer.valueOf(value.toString());
        }catch (Exception e){
            try {
                return Double.valueOf(value.toString()).intValue();
            }catch (Exception e1){
                return defaultValue;
            }
        }
    }

    /*
    subString 检查越界
     */

    //缓存地址，通常都会存放在 /sdcard/Android/data/<application package>/cache 这个路径下面，

    public static File getDiskCacheDir(Context context, String uniqueName){
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())||!Environment.isExternalStorageRemovable()){
            cachePath=context.getExternalCacheDir().getPath();
        }else {
            cachePath=context.getCacheDir().getPath();
        }
        return new File(cachePath+File.separator+uniqueName);
    }

    //应用程序版本号
    public static int getAppVersion(Context context){
        try {
            PackageInfo info=context.getPackageManager().getPackageInfo(context.getPackageName(),0);
            return info.versionCode;
        }catch (PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
        return 1;
    }
}
