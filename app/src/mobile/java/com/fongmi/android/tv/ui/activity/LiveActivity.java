package com.fongmi.android.tv.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.ui.PlayerView;
import androidx.viewbinding.ViewBinding;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.EpgData;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.cast.CastVideo;
import com.fongmi.android.tv.databinding.ActivityLiveBinding;
import com.fongmi.android.tv.event.ActionEvent;
import com.fongmi.android.tv.event.ErrorEvent;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.impl.PassCallback;
import com.fongmi.android.tv.impl.SubtitleCallback;
import com.fongmi.android.tv.lvdou.AdmUtils;
import com.fongmi.android.tv.lvdou.HawkUser;
import com.fongmi.android.tv.lvdou.bean.AdmUser;
import com.fongmi.android.tv.lvdou.dialog.LoginDialog;
import com.fongmi.android.tv.lvdou.dialog.MallDialog;
import com.fongmi.android.tv.lvdou.impl.UserEvent;
import com.fongmi.android.tv.model.LiveViewModel;
import com.fongmi.android.tv.player.ExoUtil;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.ui.adapter.ChannelAdapter;
import com.fongmi.android.tv.ui.adapter.EpgDataAdapter;
import com.fongmi.android.tv.ui.adapter.GroupAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.CustomKeyDownLive;
import com.fongmi.android.tv.ui.dialog.CastDialog;
import com.fongmi.android.tv.ui.dialog.InfoDialog;
import com.fongmi.android.tv.ui.dialog.LiveDialog;
import com.fongmi.android.tv.ui.dialog.PassDialog;
import com.fongmi.android.tv.ui.dialog.SubtitleDialog;
import com.fongmi.android.tv.ui.dialog.TrackDialog;
import com.fongmi.android.tv.utils.Biometric;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PiP;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Traffic;
import com.fongmi.android.tv.utils.Util;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import tv.danmaku.ijk.media.player.ui.IjkVideoView;

public class LiveActivity extends BaseActivity implements Clock.Callback, CustomKeyDownLive.Listener, TrackDialog.Listener, Biometric.Callback, PassCallback, LiveCallback, GroupAdapter.OnClickListener, ChannelAdapter.OnClickListener, EpgDataAdapter.OnClickListener, SubtitleCallback, CastDialog.Listener, InfoDialog.Listener {

    private ActivityLiveBinding mBinding;
    private ChannelAdapter mChannelAdapter;
    private EpgDataAdapter mEpgDataAdapter;
    private Observer<Channel> mObserveUrl;
    private CustomKeyDownLive mKeyDown;
    private GroupAdapter mGroupAdapter;
    private Observer<Epg> mObserveEpg;
    private LiveViewModel mViewModel;
    private List<Group> mHides;
    private Players mPlayers;
    private Channel mChannel;
    private Group mGroup;
    private Runnable mR0;
    private Runnable mR1;
    private Runnable mR2;
    private Runnable mR3;
    private Runnable mR4;
    private Clock mClock;
    private boolean foreground;
    private boolean redirect;
    private boolean rotate;
    private boolean stop;
    private boolean lock;
    private int toggleCount;
    private int passCount;
    private PiP mPiP;

    public static void start(Context context) {
        if (!LiveConfig.isEmpty()) context.startActivity(new Intent(context, LiveActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("empty", false));
    }

    private boolean isEmpty() {
        return getIntent().getBooleanExtra("empty", true);
    }

    private PlayerView getExo() {
        return Setting.getRender() == 0 ? mBinding.surface : mBinding.texture;
    }

    private IjkVideoView getIjk() {
        return mBinding.ijk;
    }

    private Group getKeep() {
        return mGroupAdapter.get(0);
    }

    private Live getHome() {
        return LiveConfig.get().getHome();
    }

    private int getPlayerType(int playerType) {
        return playerType != -1 ? playerType : Setting.getLivePlayer();
    }

    private int getTimeout() {
        return getHome().isEmpty() ? Constant.TIMEOUT_PLAY : getHome().getTimeout();
    }

    @Override
    protected boolean customWall() {
        return false;
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityLiveBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.hideSystemUI(this);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mKeyDown = CustomKeyDownLive.create(this, mBinding.video);
        mClock = Clock.create(Arrays.asList(mBinding.widget.clock, mBinding.display.clock));
        setPadding(mBinding.control.getRoot());
        setPadding(mBinding.widget.epg, true);
        setPadding(mBinding.recycler, true);
        mPlayers = new Players().init(this);
        mObserveEpg = this::setEpg;
        mObserveUrl = this::start;
        mHides = new ArrayList<>();
        mR0 = this::stopService;
        mR1 = this::hideControl;
        mR2 = this::setTraffic;
        mR3 = this::hideInfo;
        mR4 = this::verification;
        mPiP = new PiP();
        Server.get().start();
        setForeground(true);
        setRecyclerView();
        setVideoView();
        setDisplayView();
        setViewModel();
        checkLive();
    }

    private boolean isVip;
    private AdmUser admUser;
    private AdmUser.DataBean.UserinfoBean userInfo;

    private void setVip(boolean isVip) {
        this.isVip = isVip;
    }

    private boolean getVip() {
        return isVip;
    }

    private void setR4Callback() {
        App.post(mR4, Constant.TIMEOUT_LIVE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUserEvent(UserEvent event) {
        if (event.getType() == UserEvent.Type.USERINFO) checkVip(true);
    }

    private void verification() {
        if (getVip()) return;
        if (!mPlayers.isPlaying()) return;
        if (HawkUser.checkFree(2)) return;
        if (!HawkUser.checkLogin()) {
            checkPlay();
            login();
            return;
        }
        checkVip(false);
    }

    private void login() {
        LoginDialog.create().show(this);
    }

    private void checkVip(boolean close) {
        try {
            new AdmUtils().index(new Callback() {
                @Override
                public void success(String body) {
                    admUser = AdmUser.objectFromData(body);
                    if (admUser == null || admUser.getCode() != 1 || admUser.getData() == null || admUser.getData().getUserinfo() == null) {
                        HawkUser.saveUser(null);
                    } else {
                        userInfo = admUser.getData().getUserinfo();
                        if (userInfo != null) {
                            HawkUser.saveUser(admUser);
                            if (userInfo.getVip() > 0) {
                                setVip(true);
                                if (!mPlayers.isPlaying()) checkPlay();
                                return;
                            }
                        }
                    }
                    if (close)finish();
                    if (!close)getMall();
                }

                @Override
                public void error(String error) {
                    finish();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getMall() {
        try {
            checkPlay();
            App.execute(() -> {
                new AdmUtils().getMall(new Callback() {
                    @Override
                    public void success(String body) {
                        if (body != null) {
                            MallDialog.create(body, 10).show((FragmentActivity) getActivity());
                        } else {
                            Notify.show("获取套餐列表为空");
                            finish();
                        }
                    }

                    @Override
                    public void error(String error) {
                        Notify.show("获取套餐列表失败" + error);
                        finish();
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void initEvent() {
        mBinding.control.seek.setListener(mPlayers);
        mBinding.control.cast.setOnClickListener(view -> onCast());
        mBinding.control.info.setOnClickListener(view -> onInfo());
        mBinding.control.right.back.setOnClickListener(view -> onBack());
        mBinding.control.right.lock.setOnClickListener(view -> onLock());
        mBinding.control.right.rotate.setOnClickListener(view -> onRotate());
        mBinding.control.action.text.setOnClickListener(this::onTrack);
        mBinding.control.action.audio.setOnClickListener(this::onTrack);
        mBinding.control.action.video.setOnClickListener(this::onTrack);
        mBinding.control.action.home.setOnClickListener(view -> onHome());
        mBinding.control.action.line.setOnClickListener(view -> onLine());
        mBinding.control.action.scale.setOnClickListener(view -> onScale());
        mBinding.control.action.speed.setOnClickListener(view -> onSpeed());
        mBinding.control.action.invert.setOnClickListener(view -> onInvert());
        mBinding.control.action.across.setOnClickListener(view -> onAcross());
        mBinding.control.action.change.setOnClickListener(view -> onChange());
        mBinding.control.action.player.setOnClickListener(view -> onPlayer());
        mBinding.control.action.decode.setOnClickListener(view -> onDecode());
        mBinding.control.action.text.setOnLongClickListener(view -> onTextLong());
        mBinding.control.action.player.setOnLongClickListener(view -> onChoose());
        mBinding.control.action.speed.setOnLongClickListener(view -> onSpeedLong());
        mBinding.control.action.getRoot().setOnTouchListener(this::onActionTouch);
        mBinding.video.setOnTouchListener((view, event) -> mKeyDown.onTouchEvent(event));
    }

    private void setRecyclerView() {
        mBinding.group.setItemAnimator(null);
        mBinding.channel.setItemAnimator(null);
        mBinding.widget.epgData.setItemAnimator(null);
        mBinding.group.setAdapter(mGroupAdapter = new GroupAdapter(this));
        mBinding.channel.setAdapter(mChannelAdapter = new ChannelAdapter(this));
        mBinding.widget.epgData.setAdapter(mEpgDataAdapter = new EpgDataAdapter(this));
    }

    private void setPlayerView() {
        getIjk().setPlayer(mPlayers.getPlayer());
        mBinding.control.action.speed.setText(mPlayers.getSpeedText());
        mBinding.control.action.player.setText(mPlayers.getPlayerText());
        mBinding.control.action.speed.setEnabled(mPlayers.canAdjustSpeed());
        getExo().setVisibility(mPlayers.isExo() ? View.VISIBLE : View.GONE);
        getIjk().setVisibility(mPlayers.isIjk() ? View.VISIBLE : View.GONE);
        mBinding.video.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> mPiP.update(getActivity(), view));
    }

    private void setDecodeView() {
        mBinding.control.action.decode.setText(mPlayers.getDecodeText());
    }

    private void setVideoView() {
        mPlayers.set(getExo(), getIjk());
        setScale(Setting.getLiveScale());
        setSubtitle(Setting.getSubtitle());
        getExo().getSubtitleView().setStyle(ExoUtil.getCaptionStyle());
        getIjk().getSubtitleView().setStyle(ExoUtil.getCaptionStyle());
        mBinding.control.action.invert.setActivated(Setting.isInvert());
        mBinding.control.action.across.setActivated(Setting.isAcross());
        mBinding.control.action.change.setActivated(Setting.isChange());
        mBinding.control.action.home.setVisibility(LiveConfig.isOnly() ? View.GONE : View.VISIBLE);
    }

    private void setDisplayView() {
        mBinding.display.getRoot().setVisibility(View.VISIBLE);
        mBinding.display.progress.setVisibility(View.GONE);
        showDisplayInfo();
    }

    @Override
    public void setSubtitle(int size) {
        getExo().getSubtitleView().setFixedTextSize(Dimension.SP, size);
        getIjk().getSubtitleView().setFixedTextSize(Dimension.SP, size);
    }

    private void setScale(int scale) {
        getExo().setResizeMode(scale);
        getIjk().setResizeMode(scale);
        mBinding.control.action.scale.setText(ResUtil.getStringArray(R.array.select_scale)[scale]);
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(LiveViewModel.class);
        mViewModel.url.observeForever(mObserveUrl);
        mViewModel.epg.observeForever(mObserveEpg);
        mViewModel.live.observe(this, live -> {
            hideProgress();
            setGroup(live);
            setWidth(live);
        });
    }

    private void checkLive() {
        if (isEmpty()) {
            LiveConfig.get().init().load(getCallback());
        } else {
            getLive();
        }
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                getLive();
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
            }
        };
    }

    private void getLive() {
        mBinding.control.action.home.setText(getHome().getName());
        mPlayers.setPlayer(Setting.getLivePlayer());
        mViewModel.getLive(getHome());
        setPlayerView();
        setDecodeView();
        showProgress();
    }

    private void setGroup(Live live) {
        List<Group> items = new ArrayList<>();
        items.add(Group.create(R.string.keep));
        for (Group group : live.getGroups()) (group.isHidden() ? mHides : items).add(group);
        mGroupAdapter.addAll(items);
        setPosition(LiveConfig.get().find(items));
    }

    private void setWidth(Live live) {
        int padding = ResUtil.dp2px(48);
        if (live.getWidth() == 0) for (Group item : live.getGroups()) live.setWidth(Math.max(live.getWidth(), ResUtil.getTextWidth(item.getName(), 14)));
        mBinding.group.getLayoutParams().width = live.getWidth() == 0 ? 0 : Math.min(live.getWidth() + padding, ResUtil.getScreenWidth() / 4);
        mBinding.divide.setVisibility(live.getWidth() == 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    public void setWidth(Group group) {
        int logo = ResUtil.dp2px(56);
        int padding = ResUtil.dp2px(60);
        if (group.isKeep()) group.setWidth(0);
        if (group.getWidth() == 0) for (Channel item : group.getChannel()) group.setWidth(Math.max(group.getWidth(), (item.getLogo().isEmpty() ? 0 : logo) + ResUtil.getTextWidth(item.getNumber() + item.getName(), 14)));
        mBinding.channel.getLayoutParams().width = group.getWidth() == 0 ? 0 : Math.min(group.getWidth() + padding, ResUtil.getScreenWidth() / 3);
    }

    private void setWidth(Epg epg) {
        int padding = ResUtil.dp2px(40);
        if (epg.getList().isEmpty()) return;
        int minWidth = ResUtil.getTextWidth(epg.getList().get(0).getTime(), 14);
        if (epg.getWidth() == 0) for (EpgData item : epg.getList()) epg.setWidth(Math.max(epg.getWidth(), ResUtil.getTextWidth(item.getTitle(), 14)));
        mBinding.widget.epgData.getLayoutParams().width = epg.getWidth() == 0 ? 0 : Math.min(Math.max(epg.getWidth(), minWidth) + padding, ResUtil.getScreenWidth() / 3);
    }

    private void setPosition(int[] position) {
        if (position[0] == -1) return;
        if (mGroupAdapter.getItemCount() == 1) return;
        if (position[0] >= mGroupAdapter.getItemCount()) return;
        mGroup = mGroupAdapter.get(position[0]);
        mGroup.setPosition(position[1]);
        onItemClick(mGroup);
        onItemClick(mGroup.current());
    }

    private void setPosition() {
        if (mChannel == null) return;
        mGroup = mChannel.getGroup();
        int position = mGroupAdapter.indexOf(mGroup);
        boolean change = mGroupAdapter.getPosition() != position;
        if (change) mGroupAdapter.setSelected(position);
        if (change) mChannelAdapter.addAll(mGroup.getChannel());
        if (change) mChannelAdapter.setSelected(mGroup.getPosition());
        mBinding.channel.scrollToPosition(mGroup.getPosition());
        mBinding.group.scrollToPosition(position);
    }

    private void onCast() {
        CastDialog.create().video(CastVideo.get(mBinding.control.title.getText().toString(), mPlayers.getUrl())).fm(false).show(this);
    }

    private void onInfo() {
        InfoDialog.create(this).title(mBinding.control.title.getText()).headers(mPlayers.getHeaders()).url(mPlayers.getUrl()).show();
    }

    private void onBack() {
        finish();
    }

    private void onLock() {
        setLock(!isLock());
        mKeyDown.setLock(isLock());
        checkLockImg();
        showControl();
    }

    private void onRotate() {
        setR1Callback();
        setRotate(!isRotate());
        setRequestedOrientation(ResUtil.isLand(this) ? ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    private void checkPlay() {
        if (mPlayers.isPlaying()) onPaused();
        else onPlay();
    }

    private void onTrack(View view) {
        TrackDialog.create().player(mPlayers).type(Integer.parseInt(view.getTag().toString())).show(this);
        hideControl();
    }

    private boolean onTextLong() {
        SubtitleDialog.create(this).show();
        hideControl();
        return true;
    }

    private void onHome() {
        LiveDialog.create(this).show();
        hideControl();
    }

    private void onLine() {
        nextLine(false);
    }

    private void onScale() {
        int index = Setting.getLiveScale();
        String[] array = ResUtil.getStringArray(R.array.select_scale);
        Setting.putLiveScale(index = index == array.length - 1 ? 0 : ++index);
        setScale(index);
        setR1Callback();
    }

    private void onSpeed() {
        mBinding.control.action.speed.setText(mPlayers.addSpeed());
        setR1Callback();
    }

    private boolean onSpeedLong() {
        mBinding.control.action.speed.setText(mPlayers.toggleSpeed());
        setR1Callback();
        return true;
    }

    private void onInvert() {
        setR1Callback();
        Setting.putInvert(!Setting.isInvert());
        mBinding.control.action.invert.setActivated(Setting.isInvert());
    }

    private void onAcross() {
        setR1Callback();
        Setting.putAcross(!Setting.isAcross());
        mBinding.control.action.across.setActivated(Setting.isAcross());
    }

    private void onChange() {
        setR1Callback();
        Setting.putChange(!Setting.isChange());
        mBinding.control.action.change.setActivated(Setting.isChange());
    }

    private void onPlayer() {
        mPlayers.togglePlayer();
        Setting.putLivePlayer(mPlayers.getPlayer());
        setPlayerView();
        setR1Callback();
        fetch();
    }

    private void onDecode() {
        mPlayers.toggleDecode();
        mPlayers.set(getExo(), getIjk());
        setDecodeView();
        setR1Callback();
        fetch();
    }

    private boolean onChoose() {
        if (mPlayers.isEmpty()) return false;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra("headers", mPlayers.getHeaderArray());
        intent.putExtra("title", mBinding.control.title.getText());
        intent.setDataAndType(mPlayers.getUri(), "video/*");
        startActivity(Util.getChooser(intent));
        setRedirect(true);
        return true;
    }

    private boolean onActionTouch(View v, MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) setR1Callback();
        return false;
    }

    private void hideUI() {
        if (isGone(mBinding.recycler)) return;
        mBinding.recycler.setVisibility(View.GONE);
        setPosition();
    }

    private void showUI() {
        if (isVisible(mBinding.recycler)) return;
        mBinding.recycler.setVisibility(View.VISIBLE);
        mBinding.channel.requestFocus();
        setPosition();
        hideEpg();
    }

    @Override
    public void showEpg(Channel item) {
        if (mChannel == null || mChannel.getData().getList().isEmpty() || mEpgDataAdapter.getItemCount() == 0 || !mChannel.equals(item)) return;
        mBinding.widget.epgData.scrollToPosition(item.getData().getSelected());
        mBinding.widget.epg.setVisibility(View.VISIBLE);
        hideUI();
    }

    private void hideEpg() {
        mBinding.widget.epg.setVisibility(View.GONE);
    }

    private void showProgress() {
        mBinding.widget.progress.setVisibility(View.VISIBLE);
        App.post(mR2, 0);
        hideError();
    }

    private void hideProgress() {
        mBinding.widget.progress.setVisibility(View.GONE);
        App.removeCallbacks(mR2);
        Traffic.reset();
    }

    private void showError(String text) {
        mBinding.widget.error.setVisibility(View.VISIBLE);
        mBinding.widget.text.setText(text);
        hideProgress();
    }

    private void hideError() {
        mBinding.widget.error.setVisibility(View.GONE);
        mBinding.widget.text.setText("");
    }

    private void showControl() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) return;
        mBinding.control.info.setVisibility(mPlayers.isEmpty() ? View.GONE : View.VISIBLE);
        mBinding.control.cast.setVisibility(mPlayers.isEmpty() ? View.GONE : View.VISIBLE);
        mBinding.control.right.rotate.setVisibility(isLock() ? View.GONE : View.VISIBLE);
        mBinding.control.right.back.setVisibility(isLock() ? View.GONE : View.VISIBLE);
        mBinding.control.bottom.setVisibility(isLock() ? View.GONE : View.VISIBLE);
        mBinding.control.top.setVisibility(isLock() ? View.GONE : View.VISIBLE);
        mBinding.control.getRoot().setVisibility(View.VISIBLE);
        setR1Callback();
        hideInfo();
        hideEpg();
    }

    private void hideControl() {
        mBinding.control.getRoot().setVisibility(View.GONE);
        App.removeCallbacks(mR1);
    }

    private void showDisplayInfo() {
        boolean controlVisible = isVisible(mBinding.control.getRoot()) || isVisible(mBinding.widget.info);
        boolean visible = (!controlVisible && !isLock());
        mBinding.display.clock.setVisibility(Setting.isDisplayTime() && visible  ? View.VISIBLE : View.GONE);
        mBinding.display.netspeed.setVisibility(Setting.isDisplaySpeed() && visible ? View.VISIBLE : View.GONE);
        mBinding.display.duration.setVisibility(View.GONE);
    }

    private void onTimeChangeDisplaySpeed() {
        boolean controlVisible = isVisible(mBinding.control.getRoot()) || isVisible(mBinding.widget.info);
        boolean visible = (!controlVisible && !isLock());
        if (Setting.isDisplaySpeed() && visible) Traffic.setSpeed(mBinding.display.netspeed);
        showDisplayInfo();
    }

    private void showInfo() {
        boolean pip = Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N && isInPictureInPictureMode();
        mBinding.widget.infoPip.setVisibility(pip ? View.VISIBLE : View.GONE);
        mBinding.widget.info.setVisibility(pip ? View.GONE : View.VISIBLE);
        setR3Callback();
        hideControl();
        hideEpg();
        setInfo();
    }

    private void hideInfo() {
        mBinding.widget.infoPip.setVisibility(View.GONE);
        mBinding.widget.info.setVisibility(View.GONE);
        App.removeCallbacks(mR3);
    }

    private void setTraffic() {
        Traffic.setSpeed(mBinding.widget.traffic);
        App.post(mR2, Constant.INTERVAL_TRAFFIC);
    }

    private void setR1Callback() {
        App.post(mR1, Constant.INTERVAL_HIDE);
    }

    private void setR3Callback() {
        App.post(mR3, Constant.INTERVAL_HIDE);
    }

    private void onToggle() {
        if (isVisible(mBinding.control.getRoot())) hideControl();
        else if (isVisible(mBinding.recycler)) hideUI();
        else showUI();
        hideInfo();
    }

    private void resetPass() {
        this.passCount = 0;
    }

    private void setArtwork(String url) {
        ImgUtil.load(url, R.drawable.radio, new CustomTarget<>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                getExo().setDefaultArtwork(resource);
                getIjk().setDefaultArtwork(resource);
                setMetadata();
            }

            @Override
            public void onLoadFailed(@Nullable Drawable error) {
                getExo().setDefaultArtwork(error);
                getIjk().setDefaultArtwork(error);
                setMetadata();
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        });
    }

    @Override
    public void onItemClick(Group item) {
        mGroupAdapter.setSelected(mGroup = item);
        mChannelAdapter.addAll(item.getChannel());
        mChannelAdapter.setSelected(item.getPosition());
        mBinding.channel.scrollToPosition(Math.max(item.getPosition(), 0));
        if (!item.isKeep() || ++passCount < 5 || mHides.isEmpty()) return;
        if (Biometric.enable()) Biometric.show(this);
        else PassDialog.create().show(this);
        resetPass();
    }

    @Override
    public void onItemClick(Channel item) {
        mGroup.setPosition(mChannelAdapter.setSelected(item.group(mGroup)));
        mPlayers.setPlayer(getPlayerType(item.getPlayerType()));
        setArtwork(item.getLogo());
        mChannel = item;
        setPlayerView();
        showInfo();
        hideUI();
        fetch();
    }

    @Override
    public boolean onLongClick(Channel item) {
        if (mGroup.isHidden()) return false;
        boolean exist = Keep.exist(item.getName());
        Notify.show(exist ? R.string.keep_del : R.string.keep_add);
        if (exist) delKeep(item);
        else addKeep(item);
        return true;
    }

    @Override
    public void onItemClick(EpgData item) {
        if (item.isFuture() || !mChannel.hasCatchup()) return;
        Notify.show(getString(R.string.play_ready, item.getTitle()));
        mEpgDataAdapter.setSelected(item);
        mViewModel.getUrl(mChannel, item);
        mPlayers.clear();
        showProgress();
        hideEpg();
    }

    private void addKeep(Channel item) {
        getKeep().add(item);
        Keep keep = new Keep();
        keep.setKey(item.getName());
        keep.setType(1);
        keep.save();
    }

    private void delKeep(Channel item) {
        if (mGroup.isKeep()) mChannelAdapter.remove(item);
        getKeep().getChannel().remove(item);
        Keep.delete(item.getName());
    }

    private void setInfo() {
        mViewModel.getEpg(mChannel);
        mBinding.widget.play.setText("");
        mChannel.loadLogo(mBinding.widget.logo);
        mBinding.widget.name.setText(mChannel.getName());
        mBinding.control.title.setText(mChannel.getName());
        mBinding.widget.namePip.setText(mChannel.getName());
        mBinding.widget.line.setText(mChannel.getLineText());
        mBinding.widget.number.setText(mChannel.getNumber());
        mBinding.widget.numberPip.setText(mChannel.getNumber());
        mBinding.widget.name.setMaxEms(mChannel.getName().length());
        mBinding.widget.line.setVisibility(mChannel.getLineVisible());
        mBinding.control.action.line.setText(mBinding.widget.line.getText());
        mBinding.control.action.line.setVisibility(mBinding.widget.line.getVisibility());
    }

    private void setEpg() {
        String epg = mChannel.getData().getEpg();
        List<EpgData> data = mChannel.getData().getList();
        if (epg.length() > 0) mBinding.widget.name.setMaxEms(12);
        mBinding.widget.play.setText(epg);
        mChannelAdapter.changed(mChannel);
        mEpgDataAdapter.addAll(data);
        setWidth(mChannel.getData());
        setMetadata();
    }

    private void setEpg(Epg epg) {
        if (mChannel != null && mChannel.getName().equals(epg.getKey())) setEpg();
    }

    private void fetch() {
        if (mChannel == null) return;
        LiveConfig.get().setKeep(mChannel);
        mViewModel.getUrl(mChannel);
        mPlayers.clear();
        showProgress();
    }

    private void start(Channel result) {
        mPlayers.start(result, getTimeout());
    }

    private void checkPlayImg(boolean playing) {
        mPiP.update(this, playing);
        ActionEvent.update();
    }

    private void checkLockImg() {
        mBinding.control.right.lock.setImageResource(isLock() ? R.drawable.ic_control_lock_on : R.drawable.ic_control_lock_off);
    }

    private void resetAdapter() {
        mBinding.widget.epgData.getLayoutParams().width = 0;
        mBinding.channel.getLayoutParams().width = 0;
        mBinding.group.getLayoutParams().width = 0;
        mBinding.divide.setVisibility(View.GONE);
        mEpgDataAdapter.clear();
        mChannelAdapter.clear();
        mGroupAdapter.clear();
        mHides.clear();
        mChannel = null;
        mGroup = null;
    }

    @Override
    public void onTrackClick(Track item) {
    }

    @Override
    public void onTimeChanged() {
        onTimeChangeDisplaySpeed();
    }

    @Override
    public void setLive(Live item) {
        LiveConfig.get().setHome(item);
        mPlayers.stop();
        resetAdapter();
        hideControl();
        getLive();
    }

    @Override
    public void setPass(String pass) {
        unlock(pass);
    }

    @Override
    public void onBiometricSuccess() {
        unlock(null);
    }

    private void unlock(String pass) {
        boolean first = true;
        Iterator<Group> iterator = mHides.iterator();
        while (iterator.hasNext()) {
            Group item = iterator.next();
            if (pass != null && !pass.equals(item.getPass())) continue;
            mGroupAdapter.add(item);
            if (first) onItemClick(item);
            iterator.remove();
            first = false;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onActionEvent(ActionEvent event) {
        if (ActionEvent.PLAY.equals(event.getAction()) || ActionEvent.PAUSE.equals(event.getAction())) {
            checkPlay();
        } else if (ActionEvent.NEXT.equals(event.getAction())) {
            nextChannel();
        } else if (ActionEvent.PREV.equals(event.getAction())) {
            prevChannel();
        } else if (ActionEvent.STOP.equals(event.getAction())) {
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerEvent(PlayerEvent event) {
        switch (event.getState()) {
            case 0:
                setVip(false);
                setR4Callback();
                setTrackVisible(false);
                mClock.setCallback(this);
                break;
            case Player.STATE_IDLE:
                break;
            case Player.STATE_BUFFERING:
                showProgress();
                break;
            case Player.STATE_READY:
                setMetadata();
                resetToggle();
                hideProgress();
                mPlayers.reset();
                setSpeedVisible();
                setTrackVisible(true);
                checkPlayImg(mPlayers.isPlaying());
                mBinding.control.size.setText(mPlayers.getSizeText());
                if (isVisible(mBinding.control.getRoot())) showControl();
                break;
            case Player.STATE_ENDED:
                checkNext();
                break;
        }
    }

    private void setSpeedVisible() {
        mBinding.control.action.speed.setVisibility(mPlayers.isVod() ? View.VISIBLE : View.GONE);
    }

    private void setTrackVisible(boolean visible) {
        mBinding.control.action.text.setVisibility(visible && mPlayers.haveTrack(C.TRACK_TYPE_TEXT) ? View.VISIBLE : View.GONE);
        mBinding.control.action.audio.setVisibility(visible && mPlayers.haveTrack(C.TRACK_TYPE_AUDIO) ? View.VISIBLE : View.GONE);
        mBinding.control.action.video.setVisibility(visible && mPlayers.haveTrack(C.TRACK_TYPE_VIDEO) ? View.VISIBLE : View.GONE);
    }

    private void setMetadata() {
        String title = mBinding.widget.name.getText().toString();
        String artist = mBinding.widget.play.getText().toString();
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title);
        builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist);
        builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, getIjk().getDefaultArtwork());
        builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mPlayers.getDuration());
        mPlayers.setMetadata(builder.build());
        ActionEvent.update();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onErrorEvent(ErrorEvent event) {
        if (mPlayers.addRetry() > event.getRetry()) checkError(event);
        else fetch();
    }

    private void checkError(ErrorEvent event) {
        if (mChannel != null && mChannel.getPlayerType() == -1 && event.isUrl() && event.getRetry() > 0 && getToggleCount() < 2 && mPlayers.getPlayer() != Players.SYS) {
            toggleCount++;
            nextPlayer();
        } else {
            resetToggle();
            onError(event);
        }
    }

    private void nextPlayer() {
        mPlayers.nextPlayer();
        setPlayerView();
        fetch();
    }

    private void onError(ErrorEvent event) {
        showError(event.getMsg());
        mPlayers.stop();
        startFlow();
    }

    private void startFlow() {
        if (!Setting.isChange()) return;
        if (!mChannel.isLast()) {
            nextLine(true);
        } else if (isGone(mBinding.recycler)) {
            mChannel.setLine(0);
            nextChannel();
        }
    }

    private boolean prevGroup() {
        int position = mGroupAdapter.getPosition() - 1;
        if (position < 0) position = mGroupAdapter.getItemCount() - 1;
        if (mGroup.equals(mGroupAdapter.get(position))) return false;
        mGroup = mGroupAdapter.get(position);
        mGroupAdapter.setSelected(position);
        if (mGroup.skip()) return prevGroup();
        mChannelAdapter.addAll(mGroup.getChannel());
        mGroup.setPosition(mGroup.getChannel().size() - 1);
        return true;
    }

    private boolean nextGroup() {
        int position = mGroupAdapter.getPosition() + 1;
        if (position > mGroupAdapter.getItemCount() - 1) position = 0;
        if (mGroup.equals(mGroupAdapter.get(position))) return false;
        mGroup = mGroupAdapter.get(position);
        mGroupAdapter.setSelected(position);
        if (mGroup.skip()) return nextGroup();
        mChannelAdapter.addAll(mGroup.getChannel());
        mGroup.setPosition(0);
        return true;
    }

    private void prevChannel() {
        if (mGroup == null) return;
        int position = mGroup.getPosition() - 1;
        boolean limit = position < 0;
        if (Setting.isAcross() & limit) prevGroup();
        else mGroup.setPosition(limit ? mChannelAdapter.getItemCount() - 1 : position);
        if (!mGroup.isEmpty()) onItemClick(mGroup.current());
    }

    private void nextChannel() {
        if (mGroup == null) return;
        int position = mGroup.getPosition() + 1;
        boolean limit = position > mChannelAdapter.getItemCount() - 1;
        if (Setting.isAcross() && limit) nextGroup();
        else mGroup.setPosition(limit ? 0 : position);
        if (!mGroup.isEmpty()) onItemClick(mGroup.current());
    }

    private void checkNext() {
        int current = mChannel.getData().getInRange();
        int position = mChannel.getData().getSelected() + 1;
        boolean hasNext = position <= current && position > 0;
        if (hasNext) onItemClick(mChannel.getData().getList().get(position));
        else nextChannel();
    }

    private void prevLine() {
        if (mChannel == null || mChannel.isOnly()) return;
        mChannel.prevLine();
        showInfo();
        fetch();
    }

    private void nextLine(boolean show) {
        if (mChannel == null || mChannel.isOnly()) return;
        mChannel.nextLine();
        if (show) showInfo();
        else setInfo();
        fetch();
    }

    private void onPaused() {
        checkPlayImg(false);
        mPlayers.pause();
    }

    private void onPlay() {
        checkPlayImg(true);
        mPlayers.play();
    }

    public boolean isForeground() {
        return foreground;
    }

    public void setForeground(boolean foreground) {
        this.foreground = foreground;
    }

    public boolean isRedirect() {
        return redirect;
    }

    public void setRedirect(boolean redirect) {
        this.redirect = redirect;
    }

    public boolean isRotate() {
        return rotate;
    }

    public void setRotate(boolean rotate) {
        this.rotate = rotate;
        if (rotate) {
            noPadding(mBinding.recycler);
            noPadding(mBinding.widget.epg);
            noPadding(mBinding.control.getRoot());
        } else {
            setPadding(mBinding.recycler, true);
            setPadding(mBinding.widget.epg, true);
            setPadding(mBinding.control.getRoot());
        }
    }

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public boolean isLock() {
        return lock;
    }

    public void setLock(boolean lock) {
        this.lock = lock;
    }

    public int getToggleCount() {
        return toggleCount;
    }

    public void resetToggle() {
        this.toggleCount = 0;
    }

    private void stopService() {
        PlaybackService.stop();
    }

    @Override
    public void onCasted() {
    }

    @Override
    public void onSpeedUp() {
        if (!mPlayers.isVod() || !mPlayers.isPlaying() || !mPlayers.canAdjustSpeed()) return;
        mBinding.control.action.speed.setText(mPlayers.setSpeed(mPlayers.getSpeed() < 3 ? 3 : 5));
        mBinding.widget.speed.startAnimation(ResUtil.getAnim(R.anim.forward));
        mBinding.widget.speed.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSpeedEnd() {
        mBinding.control.action.speed.setText(mPlayers.setSpeed(1.0f));
        mBinding.widget.speed.setVisibility(View.GONE);
        mBinding.widget.speed.clearAnimation();
    }

    @Override
    public void onBright(int progress) {
        mBinding.widget.bright.setVisibility(View.VISIBLE);
        mBinding.widget.brightProgress.setProgress(progress);
        if (progress < 35) mBinding.widget.brightIcon.setImageResource(R.drawable.ic_widget_bright_low);
        else if (progress < 70) mBinding.widget.brightIcon.setImageResource(R.drawable.ic_widget_bright_medium);
        else mBinding.widget.brightIcon.setImageResource(R.drawable.ic_widget_bright_high);
    }

    @Override
    public void onBrightEnd() {
        mBinding.widget.bright.setVisibility(View.GONE);
    }

    @Override
    public void onVolume(int progress) {
        mBinding.widget.volume.setVisibility(View.VISIBLE);
        mBinding.widget.volumeProgress.setProgress(progress);
        if (progress < 35) mBinding.widget.volumeIcon.setImageResource(R.drawable.ic_widget_volume_low);
        else if (progress < 70) mBinding.widget.volumeIcon.setImageResource(R.drawable.ic_widget_volume_medium);
        else mBinding.widget.volumeIcon.setImageResource(R.drawable.ic_widget_volume_high);
    }

    @Override
    public void onVolumeEnd() {
        mBinding.widget.volume.setVisibility(View.GONE);
    }

    @Override
    public void onFlingUp() {
        if (!mPlayers.isVod()) prevChannel();
    }

    @Override
    public void onFlingDown() {
        if (!mPlayers.isVod()) nextChannel();
    }

    @Override
    public void onFlingLeft() {
        if (!mPlayers.isVod()) prevLine();
    }

    @Override
    public void onFlingRight() {
        if (!mPlayers.isVod()) nextLine(true);
    }

    @Override
    public void onSeek(int time) {
        if (!mPlayers.isVod()) return;
        mBinding.widget.action.setImageResource(time > 0 ? R.drawable.ic_widget_forward : R.drawable.ic_widget_rewind);
        mBinding.widget.time.setText(mPlayers.getPositionTime(time));
        mBinding.widget.seek.setVisibility(View.VISIBLE);
        hideProgress();
    }

    @Override
    public void onSeekEnd(int time) {
        if (!mPlayers.isVod()) return;
        mBinding.widget.seek.setVisibility(View.GONE);
        mPlayers.seekTo(time);
        showProgress();
        onPlay();
    }

    @Override
    public void onSingleTap() {
        onToggle();
    }

    @Override
    public void onDoubleTap() {
        if (isVisible(mBinding.recycler)) hideUI();
        else if (isVisible(mBinding.widget.epg)) hideEpg();
        else if (isVisible(mBinding.control.getRoot())) hideControl();
        else showControl();
    }

    @Override
    public void onShare(CharSequence title, String url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Intent.EXTRA_TEXT, url);
        intent.putExtra("extra_headers", mPlayers.getHeaderBundle());
        intent.putExtra("title", title);
        intent.putExtra("name", title);
        intent.setType("text/plain");
        startActivity(Util.getChooser(intent));
        setRedirect(true);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (isRedirect()) return;
        if (isLock()) App.post(this::onLock, 500);
        if (mPlayers.haveTrack(C.TRACK_TYPE_VIDEO)) mPiP.enter(this, Setting.getLiveScale() == 2);
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        if (isInPictureInPictureMode) {
            PlaybackService.start(mPlayers);
            setSubtitle(10);
            hideControl();
            hideInfo();
            hideUI();
        } else {
            hideInfo();
            setForeground(true);
            PlaybackService.stop();
            setSubtitle(Setting.getSubtitle());
            if (isStop()) finish();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Util.hideSystemUI(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Util.hideSystemUI(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mClock.stop().start();
        setStop(false);
        onPlay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isForeground()) return;
        if (isRedirect()) onPlay();
        App.removeCallbacks(mR0);
        App.post(mR0, 1000);
        setForeground(true);
        setRedirect(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setForeground(false);
        App.removeCallbacks(mR0);
        if (isRedirect()) onPaused();
        if (Setting.isBackgroundOn() && !isFinishing()) PlaybackService.start(mPlayers);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Setting.isBackgroundOff()) onPaused();
        if (Setting.isBackgroundOff()) mClock.stop();
        setStop(true);
    }

    @Override
    public void onBackPressed() {
        if (isVisible(mBinding.control.getRoot())) {
            hideControl();
        } else if (isVisible(mBinding.widget.info)) {
            hideInfo();
        } else if (isVisible(mBinding.widget.epg)) {
            hideEpg();
        } else if (isVisible(mBinding.recycler)) {
            hideUI();
        } else if (!isLock()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mClock.release();
        mPlayers.release();
        PlaybackService.stop();
        mViewModel.url.removeObserver(mObserveUrl);
        mViewModel.epg.removeObserver(mObserveEpg);
        App.removeCallbacks(mR0, mR1, mR2, mR3, mR4);
    }
}
