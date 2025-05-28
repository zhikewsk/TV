package com.fongmi.android.tv.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.cast.ScanEvent;
import com.fongmi.android.tv.databinding.FragmentSettingBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.ConfigCallback;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.impl.ProxyCallback;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.lvdou.AdSdkUtils;
import com.fongmi.android.tv.lvdou.AdmUtils;
import com.fongmi.android.tv.lvdou.HawkAdm;
import com.fongmi.android.tv.lvdou.HawkCustom;
import com.fongmi.android.tv.lvdou.HawkUser;
import com.fongmi.android.tv.lvdou.Utils;
import com.fongmi.android.tv.lvdou.bean.AdmUser;
import com.fongmi.android.tv.lvdou.dialog.LoginDialog;
import com.fongmi.android.tv.lvdou.dialog.MallDialog;
import com.fongmi.android.tv.lvdou.impl.UserEvent;
import com.fongmi.android.tv.player.ExoUtil;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.ui.activity.MainActivity;
import com.fongmi.android.tv.ui.activity.NoticeActivity;
import com.fongmi.android.tv.ui.activity.ScanActivity;
import com.fongmi.android.tv.ui.activity.WebActivity;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.dialog.ConfigDialog;
import com.fongmi.android.tv.ui.dialog.HistoryDialog;
import com.fongmi.android.tv.ui.dialog.LiveDialog;
import com.fongmi.android.tv.ui.dialog.ProxyDialog;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.ui.dialog.TransmitActionDialog;
import com.fongmi.android.tv.ui.dialog.TransmitDialog;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.bean.Doh;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.permissionx.guolindev.PermissionX;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SettingFragment extends BaseFragment implements ConfigCallback, SiteCallback, LiveCallback, ProxyCallback {

    private FragmentSettingBinding mBinding;
    private boolean customDepot;
    private String[] backup;
    private int type;

    public static SettingFragment newInstance() {
        return new SettingFragment();
    }

    private int getDohIndex() {
        return Math.max(0, VodConfig.get().getDoh().indexOf(Doh.objectFrom(Setting.getDoh())));
    }

    private String[] getDohList() {
        List<String> list = new ArrayList<>();
        for (Doh item : VodConfig.get().getDoh()) list.add(item.getName());
        return list.toArray(new String[0]);
    }

    private MainActivity getRoot() {
        return (MainActivity) getActivity();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        EventBus.getDefault().register(this);
        mBinding.vodUrl.setText(VodConfig.getDesc());
        mBinding.liveUrl.setText(LiveConfig.getDesc());
//        mBinding.wallUrl.setText(WallConfig.getDesc());
//        mBinding.dohText.setText(getDohList()[getDohIndex()]);
        mBinding.versionText.setText(BuildConfig.VERSION_NAME);
        customDepot = HawkCustom.get().getConfig("custom_depot", false);
        mBinding.backupText.setText((backup = ResUtil.getStringArray(R.array.select_backup))[Setting.getBackupMode()]);
//        mBinding.aboutText.setText(BuildConfig.FLAVOR_mode + "-" + BuildConfig.FLAVOR_api + "-" + BuildConfig.FLAVOR_abi);
//        mBinding.proxyText.setText(UrlUtil.scheme(Setting.getProxy()));
        setCacheText();
    }

    public void initData() {
        isSign();
        new AdmUtils().index(new Callback() {
            @Override
            public void success(String body) {
                AdmUser userData = App.gson().fromJson(body, AdmUser.class);
                if (userData != null && userData.getCode() == 1 && userData.getData() != null) {
                    AdmUser.DataBean.UserinfoBean userInfo = userData.getData().getUserinfo();
                    if (userInfo != null) {
                        long vipTime = userInfo.getVipendtime();
                        customDepot = HawkCustom.get().getConfig("custom_depot", false);
                        ImgUtil.loadAvatar(Utils.getAdminUrl(userInfo.getAvatar()), mBinding.avatar);
                        mBinding.money.setText(String.valueOf(userInfo.getMoney()));
                        mBinding.score.setText(userInfo.getScore());
                        mBinding.time.setTextColor(vipTime == 88888888 ? getResources().getColor(R.color.selected_text_color) : getResources().getColor(R.color.primary_0));
                        mBinding.time.setText(vipTime == 88888888 ? "永久会员" : Utils.stampToDate(vipTime * 1000));
//                        mBinding.openVip.setText(vipTime != 0 ? "权益中心" : "开通会员");
                        mBinding.name.setText(userInfo.getNickname());
                        HawkUser.saveUser(userData);
                        isSign();
                    }
                } else {
                    HawkUser.saveUser(null);
                    mBinding.score.setText("0");
                    mBinding.money.setText("0");
                    mBinding.time.setText("登录后查看");
//                    mBinding.openVip.setText("立即登录");
                    mBinding.name.setText("未登录");
                }
            }

            @Override
            public void error(String error) {
                Notify.show(error);
            }
        });
    }

    private void setCacheText() {
        FileUtil.getCacheSize(new Callback() {
            @Override
            public void success(String result) {
                mBinding.cacheText.setText(result);
            }
        });
        mBinding.species.setText(String.valueOf(HawkUser.species()));
        String vodValue = HawkCustom.get().getConfigValue("img", "个人中心", "img");
        ImgUtil.load("个人中心", R.drawable.ad_test,Utils.getAdminUrl(vodValue), mBinding.mall);
        if (!HawkCustom.get().getConfig("home_live", false)) {
            mBinding.live.setVisibility(View.GONE);
            mBinding.liveh.setVisibility(View.GONE);
        }
    }

    @Override
    protected void initEvent() {
        mBinding.vodUrl.setOnClickListener(this::onVod);
        mBinding.live.setOnClickListener(this::onLive);
//        mBinding.wall.setOnClickListener(this::onWall);
        mBinding.proxy.setOnClickListener(this::onProxy);
        mBinding.cache.setOnClickListener(this::onCache);
        mBinding.cache.setOnLongClickListener(this::onCacheLongClick);
        mBinding.transmit.setOnClickListener(this::onTransmit);
        mBinding.notice.setOnClickListener(this::onNotice);
//        mBinding.pull.setOnClickListener(this::onPull);
        mBinding.backup.setOnClickListener(this::onBackup);
        mBinding.restore.setOnClickListener(this::onRestore);
        mBinding.player.setOnClickListener(this::onPlayer);
        mBinding.version.setOnClickListener(this::onVersion);
        mBinding.vodUrl.setOnLongClickListener(this::onVodEdit);
        mBinding.vodHome.setOnClickListener(this::onVodHome);
        mBinding.live.setOnLongClickListener(this::onLiveEdit);
        mBinding.liveHome.setOnClickListener(this::onLiveHome);
//        mBinding.wall.setOnLongClickListener(this::onWallEdit);
        mBinding.backup.setOnLongClickListener(this::onBackupMode);
        mBinding.vodHistory.setOnClickListener(this::onVodHistory);
        mBinding.version.setOnLongClickListener(this::onVersionDev);
        mBinding.liveHistory.setOnClickListener(this::onLiveHistory);
        mBinding.wallDefault.setOnClickListener(this::setWallDefault);
        mBinding.wallRefresh.setOnClickListener(this::setWallRefresh);
        mBinding.doh.setOnClickListener(this::setDoh);
        mBinding.custom.setOnClickListener(this::onCustom);
        mBinding.about.setOnClickListener(this::onAbout);
        mBinding.user.setOnClickListener(this::user);
        mBinding.user.setOnLongClickListener(this::logout);
        mBinding.sign.setOnClickListener(this::sign);
        mBinding.mall.setOnClickListener(this::mall);
        mBinding.qrcode.setOnClickListener(this::qrcode);
        mBinding.service.setOnClickListener(this::qqGroup);
        mBinding.service.setOnLongClickListener(this::service);
        mBinding.test.setOnClickListener(view-> new AdSdkUtils().rewardVideoAD(getContext(), code -> {}));
    }

    private boolean service(View view) {
        String value = HawkAdm.getService();
        if (!TextUtils.isEmpty(value)) openQQChat(value, "service");
        return true;
    }

    private void qqGroup(View view) {
        String value = HawkAdm.getQqgroup();
        if (!TextUtils.isEmpty(value)) openQQChat(value, "qqGroup");
    }

    private void openQQChat(String value, String type) {
        try {
            Intent intent = new Intent();
            intent.setData(Uri.parse(getUrl(value, type)));
            startActivity(intent);
        } catch (Exception e) {
            Notify.show("未安装手Q或安装的版本不支持");
        }
    }

    private String getUrl(String value, String type){
        String url = "mqqwpa://im/chat?chat_type=wpa&uin=" + value + "&version=1";
        if (type.equals("qqGroup")) {
            url = "mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D" + value;
        }
        return url;
    }

    private void onNotice(View view) {
        NoticeActivity.start(getActivity());
    }

    private void qrcode(View view) {
        ScanActivity.start(getRoot());
    }

    private void login(String text) {
        if (HawkUser.checkLogin()) {
            OkGo.<String>post(text.replace("?tab=5", "") + "/action")
                    .params("do", "logon")
                    .params("account", "token_" + HawkUser.token())
                    .params("pass", "token")
                    .execute(new StringCallback() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            Notify.show(response.body());
                        }

                        @Override
                        public void onError(Response<String> error) {
                            Notify.show(error.body());
                        }
                    });
        } else {
            Notify.show("请先在本设备登录");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onScanEvent(ScanEvent event) {
        String text = event.getAddress();
        if (text.endsWith("?tab=5")) login(text);
    }

    private void mall(View view) {
        if (!HawkUser.checkLogin()) {
            Notify.show("请先登录");
            user(mBinding.user);
            return;
        }
        Notify.progress(getActivity());
        App.execute(() -> {
            new AdmUtils().getMall(new Callback() {
                @Override
                public void success(String body) {
                    MallDialog.create(body, 10).show(getActivity());
                    Notify.dismiss();
                }

                @Override
                public void error(String error) {
                    Notify.show("获取套餐列表失败" + error);
                }
            });
        });
    }

    private void isSign() {
        if (HawkUser.checkLogin()) {
            new AdmUtils().isSign(new Callback() {
                @Override
                public void success(String body) {
                    try {
                        JSONObject object = new JSONObject(body);
                        String data = object.optString("data");
                        mBinding.sign.setText(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void error(String error) {
                    Notify.show(error);
                }
            });
        }
    }

    private void sign(View view) {
        if (!HawkUser.checkLogin()) {
            user(mBinding.user);
        } else {
            new AdSdkUtils().rewardVideoAD(getContext(), code -> {
                new AdmUtils().toSign(new Callback() {
                    @Override
                    public void success(String body) {
                        try {
                            JSONObject object = new JSONObject(body);
                            String msg = object.optString("msg");
                            int code = object.optInt("code");
                            if (code == 1) initData();
                            Notify.show(msg);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void error(String error) {
                        Notify.show(error);
                    }
                });
            });
        }
    }

    private boolean logout(View view) {
        if (!HawkUser.checkLogin()) {
            user(mBinding.user);
        } else {
            new AdmUtils().logout(new Callback() {
                @Override
                public void success(String body) {
                    HawkUser.saveUser(null);
                    Notify.show(body);
                    initData();
                }

                @Override
                public void error(String error) {
                    Notify.show(error);
                }
            });
        }
        return true;
    }

    private void user(View view) {
        if (!HawkUser.checkLogin()) {
            LoginDialog.create().show(getActivity());
            return;
        }
        WebActivity.start(getActivity(), Utils.getApi("index/user/index"));
    }

    @Override
    public void setConfig(Config config) {
        if (config.getUrl().startsWith("file") && !PermissionX.isGranted(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> load(config));
        } else {
            load(config);
        }
    }

    private void load(Config config) {
        switch (config.getType()) {
            case 0:
                Notify.progress(getActivity());
                VodConfig.load(config, getCallback());
                mBinding.vodUrl.setText(config.getDesc());
                break;
            case 1:
                Notify.progress(getActivity());
                LiveConfig.load(config, getCallback());
                mBinding.liveUrl.setText(config.getDesc());
                break;
            case 2:
                Notify.progress(getActivity());
                WallConfig.load(config, getCallback());
//                mBinding.wallUrl.setText(config.getDesc());
                break;
        }
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                setConfig();
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
                setConfig();
            }
        };
    }

    private void setConfig() {
        switch (type) {
            case 0:
                Notify.dismiss();
                RefreshEvent.video();
                RefreshEvent.config();
                break;
            case 1:
                Notify.dismiss();
                RefreshEvent.config();
                break;
            case 2:
                Notify.dismiss();
                RefreshEvent.config();
                break;
        }
    }

    @Override
    public void setSite(Site item) {
        VodConfig.get().setHome(item);
        RefreshEvent.video();
    }

    @Override
    public void onChanged() {
    }

    @Override
    public void setLive(Live item) {
        LiveConfig.get().setHome(item);
    }

    private void onVod(View view) {
        if (!customDepot) return;
        ConfigDialog.create(this).type(type = 0).show();
    }

    private void onLive(View view) {
        if (!customDepot) return;
        ConfigDialog.create(this).type(type = 1).show();
    }

    private void onWall(View view) {
        ConfigDialog.create(this).type(type = 2).show();
    }

    private boolean onVodEdit(View view) {
        if (!customDepot) return false;
        ConfigDialog.create(this).type(type = 0).edit().show();
        return true;
    }

    private boolean onLiveEdit(View view) {
        if (!customDepot) return false;
        ConfigDialog.create(this).type(type = 1).edit().show();
        return true;
    }

    private boolean onWallEdit(View view) {
        ConfigDialog.create(this).type(type = 2).edit().show();
        return true;
    }

    private void onVodHome(View view) {
        SiteDialog.create(this).all().show();
    }

    private void onLiveHome(View view) {
        LiveDialog.create(this).action().show();
    }

    private void onVodHistory(View view) {
        HistoryDialog.create(this).type(type = 0).show();
    }

    private void onLiveHistory(View view) {
        HistoryDialog.create(this).type(type = 1).show();
    }

    private void onPlayer(View view) {
        getRoot().change(2);
    }

    private void onCustom(View view) {
        getRoot().change(3);
    }

    private void onAbout(View view) {
        String about = HawkCustom.get().getConfig("about", BuildConfig.FLAVOR_mode + "-" + BuildConfig.FLAVOR_api + "-" + BuildConfig.FLAVOR_abi);
        new MaterialAlertDialogBuilder(getActivity())
                .setMessage(about)
                .setNegativeButton(R.string.dialog_negative, null)
                .setPositiveButton(R.string.dialog_positive, null)
                .show();
    }

    private void onVersion(View view) {
        Updater.get().force().release().start(getActivity());
    }

    private boolean onVersionDev(View view) {
        Updater.get().force().dev().start(getActivity());
        return true;
    }

    private void setWallDefault(View view) {
        WallConfig.refresh(Setting.getWall() == 4 ? 1 : Setting.getWall() + 1);
    }

    private void setWallRefresh(View view) {
        Notify.progress(getActivity());
        WallConfig.get().load(new Callback() {
            @Override
            public void success() {
                Notify.dismiss();
                setCacheText();
            }
        });
    }

    private void setDoh(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.setting_doh).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(getDohList(), getDohIndex(), (dialog, which) -> {
            setDoh(VodConfig.get().getDoh().get(which));
            dialog.dismiss();
        }).show();
    }

    private void setDoh(Doh doh) {
        ExoUtil.reset();
        Source.get().stop();
        OkHttp.get().setDoh(doh);
        Notify.progress(getActivity());
        Setting.putDoh(doh.toString());
//        mBinding.dohText.setText(doh.getName());
        VodConfig.load(Config.vod(), getCallback());
    }

    private void onProxy(View view) {
        ProxyDialog.create(this).show();
    }

    @Override
    public void setProxy(String proxy) {
        ExoUtil.reset();
        Source.get().stop();
        Setting.putProxy(proxy);
        OkHttp.get().setProxy(proxy);
        Notify.progress(getActivity());
        VodConfig.load(Config.vod(), getCallback());
//        mBinding.proxyText.setText(UrlUtil.scheme(proxy));
    }

    private void onCache(View view) {
        FileUtil.clearCache(new Callback() {
            @Override
            public void success() {
                VodConfig.get().getConfig().json("").save();
                setCacheText();
            }
        });
    }

    private boolean onCacheLongClick(View view) {
        FileUtil.clearCache(new Callback() {
            @Override
            public void success() {
                setCacheText();
                Config config = VodConfig.get().getConfig().json("").save();
                if (!config.isEmpty()) setConfig(config);
            }
        });
        return true;
    }

    private void onRestore(View view) {
        FileChooser.from(this).type(FileChooser.TYPE_RESTORE).show();
    }

    private void onTransmit(View view) {
        PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> {
            if (allGranted) {
                TransmitActionDialog.create(this).show();
            }
        });
    }

    private void onPull(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.transmit_pull_restore).setMessage(R.string.transmit_pull_restore_desc).setNegativeButton(R.string.dialog_negative, null).setCancelable(true).setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
            TransmitDialog.create().pullRetore().show(this);
            dialog.dismiss();
        }).show();
    }

    private void initConfig() {
        WallConfig.get().init();
        LiveConfig.get().init().load();
        VodConfig.get().init().load(getCallback());
    }

    private void restore(File file) {
        PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> AppDatabase.restore(file, new Callback() {
            @Override
            public void success() {
                if (allGranted) {
                    Notify.progress(getActivity());
                    App.post(() -> initConfig(), 3000);
                }
            }
        }));
    }

    private void onBackup(View view) {
        PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> AppDatabase.backup(new Callback() {
            @Override
            public void success(String path) {
                Notify.show(R.string.backed);
            }
        }));
    }

    private boolean onBackupMode(View view) {
        int index = Setting.getBackupMode();
        Setting.putBackupMode(index = index == backup.length - 1 ? 0 : ++index);
        mBinding.backupText.setText(backup[index]);
        return true;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) return;
        mBinding.vodUrl.setText(VodConfig.getDesc());
        mBinding.liveUrl.setText(LiveConfig.getDesc());
//        mBinding.wallUrl.setText(WallConfig.getDesc());
//        mBinding.dohText.setText(getDohList()[getDohIndex()]);
        setCacheText();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || requestCode != FileChooser.REQUEST_PICK_FILE)
            return;
        String path = FileChooser.getPathFromUri(getContext(), data.getData());
        if (FileChooser.type() == FileChooser.TYPE_APK)
            TransmitDialog.create().apk(path).show(getActivity());
        else if (FileChooser.type() == FileChooser.TYPE_RESTORE) restore(new File(path));
        else if (FileChooser.type() == FileChooser.TYPE_PUSH_WALLPAPER)
            TransmitDialog.create().wallConfig(path).show(getActivity());
        else setConfig(Config.find("file:/" + path.replace(Path.rootPath(), ""), type));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUserEvent(UserEvent event) {
        if (event.getType() == UserEvent.Type.USERINFO) initData();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        switch (event.getType()) {
            case CONFIG:
                setCacheText();
                mBinding.vodUrl.setText(VodConfig.getDesc());
                mBinding.liveUrl.setText(LiveConfig.getDesc());
//                mBinding.wallUrl.setText(WallConfig.getDesc());
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

}
