package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;
import com.fongmi.android.tv.databinding.ActivityNoticeBinding;
import com.fongmi.android.tv.lvdou.HawkAdm;
import com.fongmi.android.tv.lvdou.adapter.NoticeAdapter;
import com.fongmi.android.tv.lvdou.bean.Adm;
import com.fongmi.android.tv.ui.base.BaseActivity;

import java.util.List;

public class NoticeActivity extends BaseActivity {

    private ActivityNoticeBinding mBinding;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, NoticeActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityNoticeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        initAdapter();
    }

    private void initAdapter() {
        mBinding.list.setLayoutManager(new LinearLayoutManager(this));
        NoticeAdapter adapter = new NoticeAdapter(this, loadNoticeData());
        mBinding.list.setAdapter(adapter);
    }

    @Override
    protected void initEvent() {
        super.initEvent();
    }

    private List<Adm.DataBean.NoticeListBean> loadNoticeData() {
        return HawkAdm.getNoticeList();
    }
}
