package com.infrastructure.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by cyc20 on 2018/3/4.
 */

public abstract class BaseActivity extends AppCompatActivity{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initVariables();
        initView(savedInstanceState);
        loadData();
    }
    protected abstract void initVariables();//intent数据/成员变量
    protected abstract void initView(Bundle saveInstanceState);//view
    protected abstract void loadData();//MobileAPI获取数据
}
