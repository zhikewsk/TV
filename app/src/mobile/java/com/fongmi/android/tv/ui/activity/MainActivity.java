package com.fongmi.android.tv.ui.activity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.databinding.ActivityMainBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.event.StateEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.lvdou.AdSdkUtils;
import com.fongmi.android.tv.lvdou.AdmUtils;
import com.fongmi.android.tv.lvdou.HawkAdm;
import com.fongmi.android.tv.lvdou.HawkCustom;
import com.fongmi.android.tv.lvdou.HawkUser;
import com.fongmi.android.tv.lvdou.Utils;
import com.fongmi.android.tv.lvdou.bean.AdmUser;
import com.fongmi.android.tv.lvdou.dialog.BoxDialog;
import com.fongmi.android.tv.lvdou.dialog.NoticeDialog;
import com.fongmi.android.tv.lvdou.impl.AdmCallback;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.receiver.ShortcutReceiver;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.FragmentStateManager;
import com.fongmi.android.tv.ui.fragment.SettingCustomFragment;
import com.fongmi.android.tv.ui.fragment.SettingFragment;
import com.fongmi.android.tv.ui.fragment.SettingPlayerFragment;
import com.fongmi.android.tv.ui.fragment.VodFragment;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.UrlUtil;
import com.google.android.material.navigation.NavigationBarView;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

public class MainActivity extends BaseActivity implements NavigationBarView.OnItemSelectedListener {

    private ActivityMainBinding mBinding;
    private FragmentStateManager mManager;
    private boolean confirm;

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityMainBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkAction(intent);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        Hawk.put(HawkUser.MARK_CODE, Utils.getAndroidId());
        Updater.get().release().start(this);
        initFragment(savedInstanceState);
        Server.get().start();
        getAdmConf();
    }

    private void getAdmConf() {
        LoadImage();
        mBinding.speed.setText("正在链接服务器");
        HawkAdm.get().load(new AdmCallback() {

            @Override
            public void message(String msg) {
                mBinding.speed.setText(msg);
            }

            @Override
            public void success() {
                mBinding.home.setFitsSystemWindows(true);
                App.post(() -> {
                    if (isVisible(mBinding.splash)) {
                        mBinding.splash.setVisibility(View.GONE);
                        NoticeDialog.get().start(getActivity());
                    }
                }, Constant.INTERVAL_HIDE);
                getAppConfig();
                initConfig();
                infoList();
            }

            @Override
            public void error(String msg) {
                mBinding.speed.setText(msg);
                BoxDialog.create(getActivity(), new BoxDialog.Callback() {
                    @Override
                    public void retry() {
                        Hawk.delete(HawkAdm.ADM_URL);
                        Hawk.delete(HawkAdm.ADM_URLS);
                        getAdmConf();
                    }

                    @Override
                    public void close() {
                        finish();
                    }
                }).show();
            }
        });
    }

    private void LoadImage() {
        try {
            File file = FileUtil.getWall(8888);
            mBinding.splash.setVisibility(View.VISIBLE);
            if (!file.exists() && file.length() < 1) {
                mBinding.splash.setBackgroundResource(R.drawable.ic_app_splash);
            } else {
                mBinding.splash.setBackgroundDrawable(WallConfig.drawable(Drawable.createFromPath(file.getAbsolutePath())));
            }
        } catch (Exception e) {
            FileUtil.getWall(9999).delete();
            mBinding.splash.setBackgroundResource(R.drawable.ic_app_splash);
        }
        App.post(() -> new AdSdkUtils().splashAD(this, mBinding.splash, code -> {}), 1000);
    }

    public void infoList() {
//        App.execute(() -> {
//            List<Adm.DataBean.NoticeListBean> noticeList = HawkAdm.getNoticeList();
//            if (noticeList != null) {
//                for (int i = 0; i < noticeList.size(); i++) {
//                    String id = String.valueOf(noticeList.get(i).getId());
//                    if (!HawkInfo.isMessageRead(id)) {
//                        App.post(() -> mBinding.info.setImageResource(R.drawable.ic_home_info_unread));
//                        break;
//                    } else {
//                        App.post(() -> mBinding.info.setImageResource(R.drawable.ic_home_info));
//                    }
//                }
//            }
//        });
    }

    private void getAppConfig() {
        if (!HawkCustom.get().hasConfig()) {
            HawkCustom.get().load(new Callback() {
                @Override
                public void success() {
                    setAppConfig();
                }
            });
            return;
        }
        setAppConfig();
    }

    private void setAppConfig() {
        index(HawkCustom.get().getConfig("auto_logon", false));
    }

    private void index(boolean autoRegister) {
        if (HawkUser.checkLogin()) {
            new AdmUtils().index(new Callback() {
                @Override
                public void success(String result) {
                    AdmUser userData = AdmUser.objectFromData(result);
                    if (userData != null && userData.getCode() == 1 && userData.getData() != null) {
                        HawkUser.saveUser(userData);
                    } else {
                        HawkUser.saveUser(null);
                    }
                }
            });
        } else if (autoRegister) {
            new AdmUtils().autoLogon(new Callback() {
                @Override
                public void success(String result) {
                    AdmUser userData = AdmUser.objectFromData(result);
                    if (userData != null && userData.getCode() == 1 && userData.getData() != null) {
                        HawkUser.saveUser(userData);
                    } else {
                        new AdmUtils().autoLogon(null, false);
                    }
                }
            }, true);
        }
    }

    @Override
    protected void initEvent() {
        mBinding.navigation.setOnItemSelectedListener(this);
        mBinding.navigation.findViewById(R.id.live).setOnLongClickListener(this::addShortcut);
    }

    private void checkAction(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            VideoActivity.push(this, intent.getStringExtra(Intent.EXTRA_TEXT));
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            if ("text/plain".equals(intent.getType()) || UrlUtil.path(intent.getData()).endsWith(".m3u")) {
                loadLive("file:/" + FileChooser.getPathFromUri(this, intent.getData()));
            } else {
                Uri data = intent.getData();
                String id = data.getQueryParameter("id");
                String cid = data.getQueryParameter("cid");
                String keys = data.getQueryParameter("keys");
                String name = data.getQueryParameter("name");
                String pics = data.getQueryParameter("pics");
                if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(cid)) {
                    Config config = Config.find(Integer.parseInt(cid)); //仓库id
                    if (config == null) CollectActivity.start(getActivity(), name);
                    else if (Integer.parseInt(cid) != VodConfig.getCid())  //数据的仓库id不等于当前配置的仓库id
                        loadConfig(config, keys, id, name, pics); //切换仓库
                    else
                        VideoActivity.start(getActivity(), keys, id, name, pics); //播放【仓库key、视频id、视频名称、封面】
                    return;
                }
                VideoActivity.push(this, intent.getData().toString());
            }
        }
    }

    private void loadConfig(Config config, String site, String vod, String name, String pic) {
        VodConfig.load(config, new Callback() {
            @Override
            public void success() {
                VideoActivity.start(getActivity(), site, vod, name, pic);
                RefreshEvent.config();
                RefreshEvent.video();
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
            }
        });
    }

    private void initFragment(Bundle savedInstanceState) {
        mManager = new FragmentStateManager(mBinding.container, getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                if (position == 0) return VodFragment.newInstance();
                if (position == 1) return SettingFragment.newInstance();
                if (position == 2) return SettingPlayerFragment.newInstance();
                if (position == 3) return SettingCustomFragment.newInstance();
                return null;
            }
        };
        if (savedInstanceState == null) mManager.change(0);
    }

    private void initConfig() {
        WallConfig.get().init();
        LiveConfig.get().init().load();
        VodConfig.get().init().load(getCallback(), true);
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                checkAction(getIntent());
                RefreshEvent.config();
                RefreshEvent.video();
            }

            @Override
            public void error(String msg) {
                RefreshEvent.config();
                StateEvent.empty();
                Notify.show(msg);
            }
        };
    }

    private void loadLive(String url) {
        LiveConfig.load(Config.find(url, 1), new Callback() {
            @Override
            public void success() {
                openLive();
            }
        });
    }

    private void setNavigation() {
        mBinding.navigation.getMenu().findItem(R.id.vod).setVisible(true);
        mBinding.navigation.getMenu().findItem(R.id.setting).setVisible(true);
        mBinding.navigation.getMenu().findItem(R.id.live).setVisible(LiveConfig.hasUrl());
    }

    private boolean openLive() {
        LiveActivity.start(this);
        return false;
    }

    private boolean addShortcut(View view) {
        ShortcutInfoCompat info = new ShortcutInfoCompat.Builder(this, getString(R.string.nav_live)).setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher)).setIntent(new Intent(Intent.ACTION_VIEW, null, this, LiveActivity.class)).setShortLabel(getString(R.string.nav_live)).build();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, ShortcutReceiver.class).setAction(ShortcutReceiver.ACTION), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        ShortcutManagerCompat.requestPinShortcut(this, info, pendingIntent.getIntentSender());
        return true;
    }

    private void setConfirm() {
        confirm = true;
        Notify.show(R.string.app_exit);
        App.post(() -> confirm = false, 5000);
    }

    public void change(int position) {
        mManager.change(position);
    }

    @Override
    public void onRefreshEvent(RefreshEvent event) {
        super.onRefreshEvent(event);
        if (event.getType().equals(RefreshEvent.Type.CONFIG)) setNavigation();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        if (event.getType() != ServerEvent.Type.PUSH) return;
        VideoActivity.push(this, event.getText());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (mBinding.navigation.getSelectedItemId() == item.getItemId()) return false;
        if (item.getItemId() == R.id.vod) return mManager.change(0);
        if (item.getItemId() == R.id.setting) return mManager.change(1);
        if (item.getItemId() == R.id.live) return openLive();
        return false;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        RefreshEvent.video();
    }

    protected boolean handleBack() {
        return true;
    }

    @Override
    protected void onBackPress() {
        if (!mBinding.navigation.getMenu().findItem(R.id.vod).isVisible()) {
            setNavigation();
        } else if (mManager.isVisible(3)) {
            change(1);
        } else if (mManager.isVisible(2)) {
            change(1);
        } else if (mManager.isVisible(1)) {
            mBinding.navigation.setSelectedItemId(R.id.vod);
        } else if (mManager.canBack(0)) {
            if (!confirm) setConfirm();
            else finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WallConfig.get().clear();
        LiveConfig.get().clear();
        VodConfig.get().clear();
        AppDatabase.backup();
        Source.get().exit();
        Server.get().stop();
    }
}
