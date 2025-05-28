package com.fongmi.android.tv.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.viewbinding.ViewBinding;
import com.fongmi.android.tv.databinding.ActivityWebviewBinding;
import com.fongmi.android.tv.lvdou.HawkUser;
import com.fongmi.android.tv.lvdou.Utils;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.tencent.smtt.sdk.CookieManager;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import java.util.HashMap;
import java.util.Objects;

public class WebActivity extends BaseActivity {

    private ActivityWebviewBinding mBinding;

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityWebviewBinding.inflate(getLayoutInflater());
    }

    public static void start(Activity activity, String url) {
        Log.d("TAG", "urlStart: " + url);
        Intent intent = new Intent(activity, WebActivity.class);
        intent.putExtra("url", url);
        activity.startActivity(intent);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        HashMap<String, String> httpHeaders = new HashMap<>();
        httpHeaders.put("token", HawkUser.token());
        setWebView(httpHeaders);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setWebView(HashMap<String, String> httpHeaders) {
        mBinding.context.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                CookieManager.getInstance().setCookie(url, "token=" + HawkUser.token());
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView webView, String s, Bitmap bitmap) {
                super.onPageStarted(webView, s, bitmap);
                if (s.contains("?return=true")) {
                    finish();
                } else if (s.contains("/index/user/logout")) {
                    HawkUser.saveUser(null);
//                    UserEvent.userInfo();
                    finish();
                } else if (s.equals(Utils.getApi(""))) {
                    finish();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
        });

        mBinding.context.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                mBinding.progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    mBinding.progressBar.setVisibility(View.GONE);
                } else {
                    mBinding.progressBar.setVisibility(View.VISIBLE);
                }
            }
        });

        mBinding.context.requestFocusFromTouch();
        WebSettings settings = mBinding.context.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setUserAgentString("PC");
        settings.setSupportZoom(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        CookieManager.getInstance().setAcceptCookie(true);
        Intent intent = getIntent();
        String url = intent.getStringExtra("url");
        mBinding.context.loadUrl(Objects.requireNonNull(url), httpHeaders);
    }

    @Override
    protected void onDestroy() {
        try {
            mBinding.context.clearCache(true);
            mBinding.context.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            mBinding.context.clearHistory();
            mBinding.context.removeAllViews();
            mBinding.context.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}