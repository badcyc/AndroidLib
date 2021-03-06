package com.infrastructure.net;

/**
 * Created by cyc20 on 2018/3/4.
 */

public class Response {
    private boolean error;
    private int errorType;    //1为Cookie失效
    private String errorMessage;
    private String result;

    public boolean hasError() {
        return error;
    }

    public void setError(boolean hasError) {
        this.error = hasError;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getErrorType() {
        return errorType;
    }

    public void setErrorType(int errorType) {
        this.errorType = errorType;
    }
}
