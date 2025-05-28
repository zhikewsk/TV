package com.fongmi.android.tv.lvdou.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.lvdou.Utils;
import com.fongmi.android.tv.lvdou.bean.Adm;
import com.fongmi.android.tv.lvdou.bean.Notice;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.ViewHolder>  {
    private Activity activity;
    private List<Notice> noticeList;

    public NoticeAdapter(Activity activity, List<Adm.DataBean.NoticeListBean> noticeList) {
        this.activity = activity;
        this.noticeList = new ArrayList<>();
        this.loadNoticeData(noticeList);
    }

    private void loadNoticeData(List<Adm.DataBean.NoticeListBean> noticeListBeans) {
        for (Adm.DataBean.NoticeListBean bean : noticeListBeans) {
            Notice notice = new Notice();
            notice.setTitle(bean.getTitle());
            notice.setContent(bean.getContent());
            notice.setUpdateTime(bean.getUpdatetime());
            this.noticeList.add(notice);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notice, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notice notice = noticeList.get(position);
        holder.info.setText(notice.getContent());
        holder.time.setText(Utils.stampToDate(notice.getUpdateTime() * 1000));
        holder.info.setOnClickListener(v -> showInfo(notice.getContent()));
    }

    private void showInfo(String info) {
        new MaterialAlertDialogBuilder(activity)
                .setMessage(info)
                .setNegativeButton(R.string.dialog_negative, null)
                .setPositiveButton(R.string.dialog_positive, null)
                .show();
    }

    @Override
    public int getItemCount() {
        return noticeList == null ? 0 : noticeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, time, info;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            info = itemView.findViewById(R.id.info);
            time = itemView.findViewById(R.id.time);
        }
    }
}
