package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogInfoBinding;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Map;

public class InfoDialog {

    private final DialogInfoBinding binding;
    private final Listener callback;
    private AlertDialog dialog;
    private CharSequence title;
    private String header;
    private String url;

    public static InfoDialog create(Activity activity) {
        return new InfoDialog(activity);
    }

    public InfoDialog(Activity activity) {
        this.binding = DialogInfoBinding.inflate(LayoutInflater.from(activity));
        this.callback = (Listener) activity;
    }

    public InfoDialog title(CharSequence title) {
        this.title = title;
        return this;
    }

    public InfoDialog headers(Map<String, String> headers) {
        StringBuilder sb = new StringBuilder();
        for (String key : headers.keySet()) sb.append(key).append(" : ").append(headers.get(key)).append("\n");
        this.header = Util.substring(sb.toString());
        return this;
    }

    public InfoDialog url(String url) {
        if (TextUtils.isEmpty(url)) url = "";
        this.url = url.startsWith("data") ? url.substring(0, Math.min(url.length(), 128)).concat("...") : url;
        return this;
    }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setView(binding.getRoot()).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        binding.url.setText(url);
        binding.title.setText(title);
        binding.header.setText(header);
        binding.url.setVisibility(TextUtils.isEmpty(url) ? View.GONE : View.VISIBLE);
        binding.header.setVisibility(TextUtils.isEmpty(header) ? View.GONE : View.VISIBLE);
    }

    private void initEvent() {
        binding.url.setOnClickListener(this::onShare);
        binding.url.setOnLongClickListener(v -> onCopy(url));
        binding.header.setOnLongClickListener(v -> onCopy(header));
    }

    private void onShare(View view) {
        callback.onShare(title, convert(url));
        dialog.dismiss();
    }

    private boolean onCopy(String text) {
        Notify.show(R.string.copied);
        Util.copy(text);
        return true;
    }

    private String convert(String url) {
        if (TextUtils.isEmpty(url)) url = "";
        return url.startsWith("http://127.0.0.1:7777") ? Uri.parse(url).getQueryParameter("url") : url;
    }

    public interface Listener {

        void onShare(CharSequence title, String url);
    }
}
