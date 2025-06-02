package com.fongmi.android.tv.app;

import android.app.Application;
import com.yinli.sspad.sdk.YLAdSdk;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        /**
         * 初始化SDK
         * 注意：
         * 1、必须在Application的onCreate方法中调用YLAdSdk.init方法
         * 2、必须先调用YLAdSdk.init方法后，才可调用其他方法
         *
         * @param context 上下文
         * @param mediaId 媒体id
         * @param showLog 是否显示日志，建议正式上线时，设置为false
         */
        YLAdSdk.init(this, "1001", false);
    }
}