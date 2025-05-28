package com.fongmi.android.tv.lvdou.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.databinding.DialogBoxBinding;
import com.fongmi.android.tv.lvdou.HawkAdm;
import com.fongmi.android.tv.lvdou.HawkCustom;
import com.fongmi.android.tv.lvdou.bean.Demo;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.orhanobut.hawk.Hawk;

public class BoxDialog implements DialogInterface.OnDismissListener {

    private final DialogBoxBinding binding;
    private final AlertDialog dialog;
    private final Callback callback;
    private Activity activity;
    public static BoxDialog create(Activity activity, Callback callback) {
        return new BoxDialog(activity, callback);
    }

    public interface Callback {
        void retry();

        void close();
    }

    public BoxDialog(Activity activity, Callback callback) {
        this.callback = callback;
        this.activity = activity;
        this.binding = DialogBoxBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
    }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
//        params.width = (int) (ResUtil.getScreenWidth() * 0.55f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.setOnDismissListener(this);
        dialog.show();
    }

    private void initView() {
        binding.info.setText("连接服务器失败~建议尝试扫码或访问下面的地址查阅是否有更新的版本" + getReleaseUrl());
    }

    private String getReleaseUrl() {
        String url = HawkCustom.get().getConfig("release_url", Demo.getInstance().getApp_api());
        if (url.contains("blob")) {
            url = url.split("blob")[0];
        }
        if (url.contains("raw")) {
            url = url.split("raw")[0];
        }
        return url;
    }

    private void initEvent() {
        binding.positive.setOnClickListener(this::onPositive);
        binding.negative.setOnClickListener(this::onNegative);
        binding.test.setOnClickListener(v -> activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getReleaseUrl()))));
    }

    private void onPositive(View view) {
        callback.retry();
        dialog.dismiss();
    }

    private void onNegative(View view) {
        callback.close();
        dialog.dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {

    }
}
