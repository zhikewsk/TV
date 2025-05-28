package com.fongmi.android.tv.lvdou;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.fongmi.android.tv.utils.Notify;
import com.orhanobut.hawk.Hawk;
import com.superad.ad_lib.ADManage;
import com.superad.ad_lib.SuperBannerAD;
import com.superad.ad_lib.SuperFullUnifiedInterstitialAD;
import com.superad.ad_lib.SuperHalfUnifiedInterstitialAD;
import com.superad.ad_lib.SuperNativeExpressAD;
import com.superad.ad_lib.SuperNativeUnifiedAD;
import com.superad.ad_lib.SuperRewardVideoAD;
import com.superad.ad_lib.SuperSplashAD;
import com.superad.ad_lib.listener.ADInitListener;
import com.superad.ad_lib.listener.AdError;
import com.superad.ad_lib.listener.SuperFullUnifiedInterstitialADListener;
import com.superad.ad_lib.listener.SuperHalfUnifiedInterstitialADListener;
import com.superad.ad_lib.listener.SuperNativeADListener;
import com.superad.ad_lib.listener.SuperNativeUnifiedADListener;
import com.superad.ad_lib.listener.SuperRewardVideoADListener;
import com.superad.ad_lib.listener.SuperSplashADListener;
import com.superad.ad_lib.listener.SuperUnifiedBannerADListener;

import java.util.Map;

public class AdSdkUtils {

    private String TAG = "AdSdkUtils";
    private SuperNativeUnifiedAD ad;

    private boolean getUse() {
        String use = HawkCustom.get().getConfig("use", "img");
        if (use == null) return false;
        if (use.isEmpty()) return false;
        return use.contains("sdk");
    }

    private boolean hasEvent(String name) {
        String event = HawkCustom.get().getConfigValue("sdk", name, "event");
        if (event == null || event.isEmpty()) return false;
        return !event.equals("close");
    }

    private boolean hasEvent(String name, String event) {
        event = HawkCustom.get().getConfigValue("sdk", name, "event");
        if (event == null || event.isEmpty()) return false;
        if (event.equals("close")) return false;
        return event.contains("name");
    }

    private String getId(String name) {
        return HawkCustom.get().getConfigValue("sdk", name, "id");
    }

    public static boolean hasInit() {
        return Hawk.get(HawkConfig.APP_H, false);
    }

    public void init(Context context) {
        if (!hasEvent("应用id") || getId("应用id").isEmpty()) return;
        if (!getUse()) {
            Hawk.put(HawkConfig.APP_H, false);
        } else {
            try {
                new ADManage().initSDK(context, getId("应用id"), new ADInitListener() {

                    @Override
                    public void onSuccess() {
                        Log.e("ad_init", "成功");
                        Hawk.put(HawkConfig.APP_H, true);
                    }

                    @Override
                    public void onError(int code, String message) {
                        Log.e("ad_init", message);
//                        Notify.show("巨量SDK:" + message);
                        Hawk.put(HawkConfig.APP_H, false);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //开屏
    public void splashAD(Context context, FrameLayout mFrameLayout, Callback callback) {
        if (!hasEvent("开屏") || !hasInit()) {
            Log.d(TAG, "splashAD: false");
            callback.onEvent(0);
        } else {
            try {
                Log.d(TAG, "splashAD: true");
                new SuperSplashAD(context, mFrameLayout, Long.parseLong(getId("开屏")), new SuperSplashADListener() {
                    @Override
                    public void onError(AdError error) {
                        callback.onEvent(1);
                        Notify.show("巨量SDK:" + error.getMsg());
                        Log.e("SuperSplashAD", "onError");
                    }

                    @Override
                    public void onAdLoad() {
                        Log.e("SuperSplashAD", "onAdLoad");
                    }

                    @Override
                    public void onADShow() {
                        Log.e("SuperSplashAD", "onADShow");
                    }

                    @Override
                    public void onADClicked() {
                        Log.e("SuperSplashAD", "onADClicked");
                    }

                    @Override
                    public void onADDismissed() {
                        callback.onEvent(200);
                        mFrameLayout.removeAllViews();
                        mFrameLayout.setVisibility(View.GONE);
                        Log.d(TAG, "splashAD: 3333333333");
                    }

                    @Override
                    public void onAdTypeNotSupport() {
                        Log.e("SuperSplashAD", "onAdTypeNotSupport");
                    }
                });
            } catch (Exception e) {
                e.fillInStackTrace();
            }
        }
    }

    //激励
    public void rewardVideoAD(Context context, Callback callback) {
        try {
            if (!hasEvent("激励视频") || !hasInit()) {
                callback.onEvent(0);
                return;
            }
            new SuperRewardVideoAD((Activity) context, Long.parseLong(getId("激励视频")), new SuperRewardVideoADListener() {
                @Override
                public void onADLoad() {
                    Log.e("~~~~~", "onADLoad");
                }

                @Override
                public void onADShow() {
                    Log.e("~~~~~", "onADShow");
                }

                @Override
                public void onReward(Map<String, Object> var1) {
                    callback.onEvent(200);
                    Log.e("~~~~~", "onReward");
                }

                @Override
                public void onADClick() {
                    Log.e("~~~~~", "onADClick");
                }

                @Override
                public void onVideoComplete() {
                    Log.e("~~~~~", "onVideoComplete");
                }

                @Override
                public void onADClose() {
                    callback.onEvent(2);
                    Log.e("~~~~~", "onADClose");
                }

                @Override
                public void onError(AdError var1) {
                    callback.onEvent(1);
                    Log.e("~~~~~", "onError");
                    Notify.show("巨量SDK:" + var1.getMsg());
                }

                @Override
                public void onAdTypeNotSupport() {

                }
            });
        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }

    //全屏
    public void fullUnifiedInterstitialAD(Context mContext, Callback callback) {
        if (!hasEvent("全屏视频") || !hasInit()) {
            callback.onEvent(0);
            return;
        }
        try {
            new SuperFullUnifiedInterstitialAD((Activity) mContext, Long.parseLong(getId("全屏视频")), new SuperFullUnifiedInterstitialADListener() {

                @Override
                public void onError(AdError error) {
                    Log.e("~~~~", "onError");
                    Notify.show("巨量SDK:" + error.getMsg());
                    callback.onEvent(1);
                }

                @Override
                public void onAdLoad() {
                    Log.e("~~~~", "onAdLoad");
                }

                @Override
                public void onAdClicked() {
                    Log.e("~~~~", "onAdClicked");
                }

                @Override
                public void onAdShow() {
                    callback.onEvent(200);
                    Log.e("~~~~", "onAdShow");
                }

                @Override
                public void onADClosed() {
                    Log.e("~~~~", "onADClose");
                    callback.onEvent(2);
                }

                @Override
                public void onRenderSuccess() {
                    Log.e("~~~~", "onRenderSuccess");
                }

                @Override
                public void onRenderFail() {
                    Log.e("~~~~", "onRenderFail");
                }

                @Override
                public void onAdTypeNotSupport() {
                    Log.e("~~~~", "onAdTypeNotSupport");
                }
            });
        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }

    //插屏半屏
    public void halfUnifiedInterstitialAD(Context mContext, String event, Callback callback) {
        try {
            if (!hasEvent("插屏") || !hasInit()) {
                callback.onEvent(0);
                Log.d(TAG, "halfUnifiedInterstitialAD: false");
                return;
            }
            Log.d(TAG, "halfUnifiedInterstitialAD: true");
            new SuperHalfUnifiedInterstitialAD((Activity) mContext, Long.parseLong(getId("插屏")), new SuperHalfUnifiedInterstitialADListener() {
                @Override
                public void onError(AdError error) {
                    Log.e("~~~~~", "onError" + error.getMsg());
                    Notify.show("巨量SDK:" + error.getMsg());
                    callback.onEvent(1);
                }

                @Override
                public void onAdLoad() {
                    Log.e("~~~~~", "onAdLoad");
                }

                @Override
                public void onAdClicked() {
                    Log.e("~~~~~", "onAdClicked");
                }

                @Override
                public void onAdShow() {
                    callback.onEvent(200);
                    Log.e("~~~~~", "onAdShow");
                }

                @Override
                public void onADClosed() {
                    Log.e("~~~~~", "onADClosed");
                    callback.onEvent(2);
                }

                @Override
                public void onRenderSuccess() {
                    Log.e("~~~~~", "onRenderSuccess");
                }

                @Override
                public void onRenderFail() {
                    Log.e("~~~~~", "onRenderFail");
                }

                @Override
                public void onAdTypeNotSupport() {

                }
            }, false);
        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }

    // 横幅
    public void bannerAD(Context mContext, FrameLayout banner_container, Callback callback) {
        try {
            if (!hasEvent("横幅") || !hasInit()) {
                callback.onEvent(0);
                return;
            }
            new SuperBannerAD((Activity) mContext, banner_container, Long.parseLong(getId("横幅")), new SuperUnifiedBannerADListener() {
                @Override
                public void onError(AdError var1) {
                    callback.onEvent(1);
                    Notify.show("巨量SDK:" + var1.getMsg());
                    Log.e("~~~~", "onError");
                }

                @Override
                public void onADLoad() {
                    Log.e("~~~~", "onADLoad");
                }

                @Override
                public void onADShow() {
                    callback.onEvent(200);
                    Log.e("~~~~", "onADShow");
                }

                @Override
                public void onADClick() {
                    Log.e("~~~~", "onADClick");
                }

                @Override
                public void onADClose() {
                    callback.onEvent(2);
                    banner_container.removeAllViews();
                    Log.e("~~~~", "onADClose");
                }

                @Override
                public void onRenderFail() {
                    Log.e("~~~~", "onRenderFail");
                }

                @Override
                public void onRenderSuccess() {
                    Log.e("~~~~", "onRenderSuccess");
                }

                @Override
                public void onAdTypeNotSupport() {
                    Log.e("~~~~", "onAdTypeNotSupport");
                }
            });
        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }

    //信息流
    public void nativeExpressAD(Activity mActivity, FrameLayout container, Callback callback) {
        try {
            if (!hasEvent("信息流") || !hasInit()) {
                callback.onEvent(0);
                return;
            }
            new SuperNativeExpressAD(mActivity, container, Long.parseLong(getId("信息流")), new SuperNativeADListener() {

                @Override
                public void onError(AdError var1) {
                    callback.onEvent(1);
                    container.setVisibility(View.GONE);
                    Notify.show("巨量SDK:" + var1.getMsg());
                    Log.e("~~~~~", "onError" + var1.getMsg());
                }

                @Override
                public void onADLoad() {
                    Log.e("~~~~~", "onADLoad");
                }

                @Override
                public void onADShow() {
                    container.setVisibility(View.VISIBLE);
                    callback.onEvent(200);
                    Log.e("~~~~~", "onADShow");
                }

                @Override
                public void onADClick() {
                    Log.e("~~~~~", "onADClick");
                }

                @Override
                public void onADClose() {
                    Log.e("~~~~~", "onADClose");
                }

                @Override
                public void onRenderFail() {
                    Log.e("~~~~~", "onRenderFail");
                }

                @Override
                public void onRenderSuccess() {
                    Log.e("~~~~~", "onRenderSuccess");
                }

                @Override
                public void onAdTypeNotSupport() {
                }
            });
        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }

    //播放器贴片
    public void nativeUnifiedAD(Activity activity, FrameLayout mContainer, Callback callback) {
        try {
            if (!hasEvent("视频贴片") || !hasInit()) {
                mContainer.setVisibility(View.GONE);
                return;
            }
            if (ad == null) {
                ad = new SuperNativeUnifiedAD(mContainer, activity, Long.parseLong(getId("视频贴片")), new SuperNativeUnifiedADListener() {
                    @Override
                    public void onError(AdError var1) {
                        callback.onEvent(1);
                        mContainer.setVisibility(View.GONE);
                        Log.e(TAG, "onError" + var1.toString());
                    }

                    @Override
                    public void onADLoad() {
                        Log.e(TAG, "onADLoad: ");
                    }

                    @Override
                    public void onADShow() {
                        callback.onEvent(200);
                        Log.e(TAG, "onADShow: ");
                    }

                    @Override
                    public void onADClick() {
                        Log.e(TAG, "onADClick: ");
                    }

                    @Override
                    public void onADClose() {
                        callback.onEvent(2);
                        mContainer.setVisibility(View.GONE);
                        Log.e(TAG, "onADClose: ");
                    }

                    @Override
                    public void onRenderFail() {
                        Log.e(TAG, "onRenderFail: ");
                    }

                    @Override
                    public void onRenderSuccess() {
                        Log.e(TAG, "onRenderSuccess: ");
                    }

                    @Override
                    public void onAdTypeNotSupport() {
                        mContainer.setVisibility(View.GONE);
                        Log.e(TAG, "onAdTypeNotSupport: 广告类型错误或者没有该广告");
                    }
                });
            }
            ad.showAd();
        } catch (Exception e) {
            e.fillInStackTrace();
        }
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
