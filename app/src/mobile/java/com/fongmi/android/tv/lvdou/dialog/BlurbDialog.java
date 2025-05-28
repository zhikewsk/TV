package com.fongmi.android.tv.lvdou.dialog;

import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.lvdou.AdSdkUtils;
import com.fongmi.android.tv.ui.dialog.BaseDialog;
import com.github.bassaer.library.MDColor;
import com.github.catvod.utils.Trans;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.DialogBlurbBinding;
import com.fongmi.android.tv.ui.custom.CustomMovement;
//import com.fongmi.android.tv.utils.AdSdkUtils;
import com.fongmi.android.tv.utils.Sniffer;

import java.util.regex.Matcher;

public class BlurbDialog extends BaseDialog {

    private DialogBlurbBinding binding;
    private Vod item;

    public static BlurbDialog create() {
        return new BlurbDialog();
    }

    public BlurbDialog() {

    }

    public BlurbDialog vod(Vod items) {
        this.item = items;
        return this;
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogBlurbBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        new AdSdkUtils().nativeExpressAD(getActivity(), binding.container, b -> {});
        binding.close.setOnClickListener(v -> dismiss());
        setSpanCount();
    }

    private void setSpanCount() {
        setText(binding.actor, R.string.detail_actor, Html.fromHtml(item.getVodActor()).toString());
        setText(binding.director, R.string.detail_director, Html.fromHtml(item.getVodDirector()).toString());
        setText(binding.content, 0, Html.fromHtml(getTextContent(item)).toString());
        Log.d("TAG", "setSpanCount: " + item.getVodContent());
    }

    private String getTextContent(Vod item) {
        try {
            String vodContent = item.getVodContent();
            vodContent = vodContent.replaceAll("\n", "");
            vodContent = vodContent.replaceAll("<p>", "");
            vodContent = vodContent.replaceAll("<br>", "");
            vodContent = vodContent.replaceAll("\\s+", "");
            if (!vodContent.isEmpty()) {
                Log.d("TAG", "getTextContent: " + vodContent);
                if (vodContent.contains("&gt;")){
                    return vodContent.split("&gt;")[1];
                }
                if (vodContent.contains(":")){
                    return vodContent.split(":")[1];
                }
                if (vodContent.contains("：")){
                    return vodContent.split("：")[1];
                }
            }
            return TextUtils.isEmpty(vodContent) ? "" : vodContent.trim();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "未知";
    }

    private void setText(TextView view, int resId, String text) {
        view.setText(getSpan(resId, text), TextView.BufferType.SPANNABLE);
        view.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
        view.setLinkTextColor(MDColor.YELLOW_500);
        CustomMovement.bind(view);
        view.setTag(text);
    }

    private SpannableStringBuilder getSpan(int resId, String text) {
        if (resId > 0) text = getString(resId, text);
        Matcher m = Sniffer.CLICKER.matcher(text);
        while (m.find()) {
            String key = Trans.s2t(m.group(2)).trim();
            text = text.replace(m.group(), key);
        }
        return new SpannableStringBuilder(text);
    }
}
