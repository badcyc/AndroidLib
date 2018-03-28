package com.infrastructure.okHttp;

import android.content.Context;
import android.os.Handler;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by cyc20 on 2018/3/28.
 */

public class OkHttpEngine {

    private volatile static OkHttpEngine mInstance;
    private OkHttpClient mOkHttpClient;
    private Handler mHandler;

    public static OkHttpEngine getInstance(Context context) {
        if (mInstance == null) {
            synchronized (OkHttpEngine.class) {
                if (mInstance == null) {
                    mInstance = new OkHttpEngine(context);
                }
            }
        }
        return mInstance;
    }

    public OkHttpEngine(Context context) {
        File file = context.getExternalCacheDir();
        int cacheSize = 10 * 1024 * 1024;
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .cache(new Cache(file.getAbsoluteFile(), cacheSize));
        mOkHttpClient = builder.build();
        mHandler = new Handler();
    }

    public void getAsynHttp(String url, ResultCallback callBack) {
        final Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = mOkHttpClient.newCall(request);
        dealResult(call, callBack);
    }

    private void dealResult(Call call, final ResultCallback callback) {
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                sendFailedCallback(call.request(), e, callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                sendSuccessCallback(response, callback);
            }
        });
    }

    private void sendFailedCallback(final Request request, final IOException e, final ResultCallback callback) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onError(request, e);
                }
            }
        });
    }

    private void sendSuccessCallback(final Response response, final ResultCallback callback) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    try {
                        callback.onResponse(response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
    }
}
