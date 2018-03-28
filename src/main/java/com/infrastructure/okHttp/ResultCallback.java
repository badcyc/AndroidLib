package com.infrastructure.okHttp;

import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by cyc20 on 2018/3/28.
 */

public abstract class ResultCallback {
    public abstract void onError(Request request,Exception e);

    public abstract void onResponse(Response response)throws IOException;
}
