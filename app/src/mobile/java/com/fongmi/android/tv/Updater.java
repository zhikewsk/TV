package com.fongmi.android.tv;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.databinding.DialogUpdateBinding;
import com.fongmi.android.tv.lvdou.Utils;
import com.fongmi.android.tv.utils.Download;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Github;
import com.github.catvod.utils.Path;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

public class Updater implements Download.Callback {

    private DialogUpdateBinding binding;
    private AlertDialog dialog;
    private boolean dev;
    private String downloadUrl;
    private String packagesize;
    private int enforce;

    private static class Loader {
        static volatile Updater INSTANCE = new Updater();
    }

    public static Updater get() {
        return Loader.INSTANCE;
    }

    private File getFile() {
        return Path.cache("update.apk");
    }

    private String getJson() {
        return Utils.getApi("api/update/app?app_id=" + Utils.getAppId() + "&version=" + BuildConfig.VERSION_NAME + "&apk_mark=" + Utils.getAppMark());
    }

    private String getApk() {
        return downloadUrl;
    }

    public Updater force() {
        Notify.show(R.string.update_check);
        Setting.putUpdate(true);
        return this;
    }

    public Updater release() {
        this.dev = false;
        return this;
    }

    public Updater dev() {
        this.dev = true;
        return this;
    }

    private Updater check() {
        dismiss();
        return this;
    }

    public void start(Activity activity) {
        App.execute(() -> doInBackground(activity));
    }

    private boolean need(String code, String name) {
        return !code.equals(name);
    }

    private void doInBackground(Activity activity) {
        try {
            String encryptedJson = OkHttp.string(getJson());
            String decryptedJson = Utils.dataDecryption(encryptedJson, "版本更新", true);
            JSONObject object = new JSONObject(decryptedJson);
            int code = object.optInt("code");
            if (code == 1 && object.has("data")) {
                JSONObject dataObject = object.getJSONObject("data");
                if (dataObject.has("upgradetext")){
                    enforce = dataObject.getInt("enforce");
                    packagesize = dataObject.getString("packagesize");
                    downloadUrl = dataObject.getString("downloadurl");
                    String version = dataObject.getString("version");
                    String newVersion = dataObject.getString("newversion");
                    String upgradetext = dataObject.getString("upgradetext");
                    if (need(version, newVersion)) App.post(() -> show(activity, newVersion, upgradetext));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void show(Activity activity, String version, String desc) {
        binding = DialogUpdateBinding.inflate(LayoutInflater.from(activity));
        check().create(activity, ResUtil.getString(R.string.update_version, version)).show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this::confirm);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(this::cancel);
        binding.desc.setText(desc);
    }

    private AlertDialog create(Activity activity, String title) {
        return dialog = new MaterialAlertDialogBuilder(activity).setTitle(title).setView(binding.getRoot()).setPositiveButton(R.string.update_confirm, null).setNegativeButton(R.string.dialog_negative, null).setCancelable(false).create();
    }

    private void cancel(View view) {
        if (enforce == 1) {
            Notify.show("此版本为强制更新,不可取消");
            return;
        }
        Setting.putUpdate(false);
        dialog.dismiss();
    }

    private void confirm(View view) {
        Download.create(getApk(), getFile(), this).start();
        view.setEnabled(false);
    }

    private void dismiss() {
        try {
            if (dialog != null) dialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void progress(int progress) {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(String.format(Locale.getDefault(), "%1$d%%", progress));
    }

    @Override
    public void error(String msg) {
        Notify.show(msg);
        if (enforce == 1) return;
        dismiss();
    }

    @Override
    public void success(File file) {
        FileUtil.openFile(file);
        if (enforce == 1) return;
        dismiss();
    }
}
