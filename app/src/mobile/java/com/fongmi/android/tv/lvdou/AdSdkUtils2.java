package com.fongmi.android.tv.lvdou;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.WallConfig;
import com.orhanobut.hawk.Hawk;

import java.io.File;

public class AdSdkUtils2 {

    public static boolean hasInit() {
        return false;
    }

    public void init(Context context) {
        Hawk.put(HawkConfig.APP_H, false);
    }

    //开屏
    public void splashAD(Context context, FrameLayout mFrameLayout, Callback callback) {
        callback.onEvent(0);
    }

    //激励
    public void rewardVideoAD(Context context, Callback callback) {
        callback.onEvent(0);
    }

    //全屏
    public void fullUnifiedInterstitialAD(Context mContext, Callback callback) {
        callback.onEvent(0);
    }

    //插屏半屏
    public void halfUnifiedInterstitialAD(Context mContext, String event, Callback callback) {
        callback.onEvent(0);
    }

    // 横幅
    public void bannerAD(Context mContext, FrameLayout banner_container, Callback callback) {
        callback.onEvent(0);
    }

    //信息流
    public void nativeExpressAD(Activity mActivity, FrameLayout container, Callback callback) {
        callback.onEvent(0);
    }

    //播放器贴片
    public void nativeUnifiedAD(Activity activity, FrameLayout mContainer, Callback callback) {
        callback.onEvent(0);
    }

    /**
     * 广告回调
     *
     * @describe 0 未开启广告位，1 错误，2 广告被关闭， 200 加载成功
     **/
    public interface Callback {
        void onEvent(int code);
    }
}
