package com.fongmi.android.tv.lvdou.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogNoticeBinding;
import com.fongmi.android.tv.lvdou.HawkAdm;
import com.fongmi.android.tv.lvdou.bean.Adm;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class NoticeDialog {

    private DialogNoticeBinding binding;
    private AlertDialog dialog;
    private String content;

    private static class Loader {
        static volatile NoticeDialog INSTANCE = new NoticeDialog();
    }

    public static NoticeDialog get() {
        return Loader.INSTANCE;
    }

    private void setContent(String content) {
        this.content = content;
    }

    private String getContent() {
        return content;
    }

    public void start(Activity activity) {
        if (!Setting.getNotice()) return;
        App.execute(() -> doInBackground(activity));
    }

    private void doInBackground(Activity activity) {
        try {
            List<Adm.DataBean.NoticeListBean> mNotice = HawkAdm.getNoticeList();
            if (mNotice != null) {
                setContent(mNotice.get(0).getContent());
                App.post(() -> show(activity));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void show(Activity activity) {
        binding = DialogNoticeBinding.inflate(LayoutInflater.from(activity));
        binding.content.setText(getContent());
        binding.close.setOnClickListener(this::dismiss);
        binding.hideClose.setOnClickListener(this::hideClose);
        create(activity).show();
    }

    private AlertDialog create(Activity activity) {
        return dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).setCancelable(false).create();
    }

    private void dismiss(View view) {
        dialog.dismiss();
    }

    private void hideClose(View view) {
        Setting.putNotice(false);
        dialog.dismiss();
    }
}
