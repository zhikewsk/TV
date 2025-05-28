package com.fongmi.android.tv.lvdou;

import android.util.Base64;
import android.util.Log;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Depot;
import com.fongmi.android.tv.lvdou.bean.Adm;
import com.fongmi.android.tv.lvdou.bean.Demo;
import com.fongmi.android.tv.lvdou.bean.UrlResponse;
import com.fongmi.android.tv.lvdou.impl.AdmCallback;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Prefers;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.HttpParams;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kotlin.jvm.internal.Intrinsics;

public class HawkAdm {

    private JSONArray urlsArray;
    private AdmCallback callback;
    public static final String APP_URL = "app_url"; //APP对接地址
    public static final String ADM_URL = "adm_url"; //上次使用的后台地址
    public static final String ADM_URLS = "app_urls"; //缓存的后台地址集
    public static final String ADM_CONFIG = "adm_config"; //缓存的后台配置

    private static class Loader {
        static volatile HawkAdm INSTANCE = new HawkAdm();
    }

    public static HawkAdm get() {
        return Loader.INSTANCE;
    }

    public void load(AdmCallback callback) {
        this.callback = callback;
        if (!Hawk.contains(ADM_URL)) loadAppUrl(); //首次启动没有adm_url(上次启动用的后台地址)
        else checkAdmUrl(Hawk.get(ADM_URL, ""), -1, 888); //非首次启动先用adm_url，-1、888让它有机会进入轮询
    }

    private void loadAppUrl() {
        String url = Demo.getInstance().getApp_api();
        int count = sub_str_count(url, '/');
        OkGo.<String>get(count < 3 ? url + "/" + HawkConfig.API_MAIN : url)
                .params(mGtParams(count))
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        String body = response.body();
                        if (body == null || body.isEmpty()) {
                            body = getUrls(); //使用缓存的urls再次请求
                            if (body.equals("{}")) {
                                callback.error("400-请求" + url + "返回数据为空且没有缓存的urls");
                                return;
                            }
                        }
                        checkConfig(body, url);
                    }

                    @Override
                    public void onError(Response<String> error) {
                        String body = getUrls(); //使用缓存的urls再次请求
                        if (body.equals("{}")) {
                            callback.error("401-请求" + url + "失败");
                            return;
                        }
                        checkConfig(body, url);
                    }
                });
    }

    private void checkAdmUrl(String url, int index, int size) { //后台地址、当前位置、数据长度
        OkGo.<String>get(url + "/" + HawkConfig.API_MAIN)
                .params(mGtParams(2))
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        String body = response.body();
                        if (!parseAdmConfig(body, url)) { //解析后台配置失败时请求下一个后台地址
                            nextRoute(index, size, body);
                        } else if (index != -1) {
                            Notify.show(getUrlName(index));
                        }
                    }

                    @Override
                    public void onError(Response<String> error) {
                        nextRoute(index, size, error.body());
                    }
                });
    }

    private void checkConfig(String body, String url) {
        //先检测是否符合【后台配置的标准格式】
        if (!parseAdmConfig(body, url)) {
            try {
                //检测是否为json格式的urls列表
                UrlResponse response = UrlResponse.object(body);
                if (response != null && response.getUrls() != null && !response.getUrls().isEmpty()) {
                    setUrls(body);
                    checkUrls(0);
                    callback.message("正在解析json配置");
                    return;
                }

                //检测是否为decodeBase64格式的urls列表
                if (body.contains("lvdou-") && body.contains("-lvdou")) {
                    callback.message("正在解析web配置");
                    Pattern pattern = Pattern.compile("lvdou-(.*?)-lvdou");
                    Matcher matcher = pattern.matcher(body);
                    boolean success = false;
                    String urls = "";
                    while (matcher.find()) {
                        urls = matcher.group(1);
                        urls = Utils.decodeBase64(urls);
                        if (urls != null && urls.contains("urls")) {
                            if (Json.valid(urls)) {
                                success = true;
                                break;
                            }
                        }
                    }
                    if (success) {
                        setUrls(urls);
                        checkUrls(0);
                    } else callback.error("402-选择线路失败【URLS配置错误】");
                    return;
                }
            } catch (JsonSyntaxException e) {
                // 处理解析失败的逻辑
                Log.e("MyActivity", "JSON格式不正确", e);
                // 可以在这里设置一些错误提示或者执行其他错误处理逻辑
            } catch (Exception e) {
                // 捕获其他可能的异常
                Log.e("MyActivity", "解析JSON时发生未知错误", e);
            }
            callback.error("403-连接服务器失败" + body);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean parseAdmConfig(String body, String url) {
        try {
            callback.message("正在配置线路");
            String config = Utils.dataDecryption(body, "基础配置", true);
            if (config != null && !config.isEmpty()) { //如果已经解密成功
                Adm admConfig = Adm.objectFromData(config);
                if (admConfig.getCode() == 1 && admConfig.getData() != null) {
                    initDepot(config); //初始化仓库
                    initConfig(config); //初始化后台配置
                    initAdmJson(admConfig); //初始化JSON配置
                    Hawk.put(ADM_URL, url); //将url设为到缓存
                    Hawk.put(ADM_CONFIG, config); //保存后台配置
                    callback.message("配置线路完成,请稍后...");
                    callback.success();
                    return true;
                }
            }
        } catch (Exception e) {
            Notify.show(e.toString());
            e.printStackTrace();
        }
        return false;
    }

    private void nextRoute(int index, int size, String msg) {
        if (index < size - 1) {
            callback.message("正在尝试更换线路");
            App.post(() -> checkUrls(index + 1), Constant.INTERVAL_HIDE);
        } else if (index >= size - 1) {
            // 所有接口都用完了,删除本地缓存使用对接地址重新获取一次
            // 如果有ADM_URL缓存表示非首次启动
            if (Hawk.contains(ADM_URL)) {
                // loadAppUrl前删除缓存配置
                Hawk.delete(ADM_URLS);
                Hawk.delete(ADM_URL);
                loadAppUrl();
            } else callback.error("405-连接服务器失败" + msg);
        }
    }

    private void checkUrls(int index) {
        String url = getUrls(index, "url");
        if (url != null) checkAdmUrl(url, index, urlsArray.length());
    }

    private String getUrlName(int index) {
        String name = getUrls(index, "name");
        return name != null ? "为您选择" + name : "请稍后...";
    }

    private String getUrls(int index, String type) {
        String text = null;
        try {
            JSONObject urlsObject = new JSONObject(getUrls());
            urlsArray = urlsObject.getJSONArray("urls");
            JSONObject urlsData = urlsArray.getJSONObject(index);
            text = urlsData.getString(type);
        } catch (Exception e) {
            callback.error("404-线路选择失败【urls格式错误】"); //回调失败逻辑。让用户选择
            e.printStackTrace();
        }
        return text;
    }

    private void initDepot(String config) {
        JsonObject object = Json.parse(config).getAsJsonObject();
        JsonObject mainData = App.gson().fromJson(object.get("data"), JsonObject.class);
        String admDepot = mainData.getAsJsonArray("depotConfig").toString();
        if (admDepot != null && !admDepot.isEmpty()) {
            List<Depot> items = Depot.arrayFrom(admDepot);
            List<Config> depotConfig = new ArrayList<>();
            for (Depot item : items) depotConfig.add(Config.find(item, 0));
            Config mConfig = depotConfig.get(0);
            Config.find(mConfig.getUrl(), mConfig.getName(), 1);
            LiveConfig.load(mConfig, null);
            List<Config> dbItems = Config.getAll(0);
            for (int i = 0; i < dbItems.size(); i++) {
                if (isDeleteDepot(dbItems.get(i).getUrl(), depotConfig)) {
                    Config.delete(dbItems.get(i).getUrl());
                }
            }
        }
    }

    private void initConfig(String config) {
        Adm object = Adm.objectFromData(config);
        if (config != null && object.getCode() == 1 && object.getData() != null) {
            Hawk.put(HawkConfig.COUNTY_KEY, object.getData().getSiteConfig().getQweather_key());
            Hawk.put(HawkConfig.PICTURE_LOGO_IMG, Utils.getAdminUrl(object.getData().getSiteConfig().getApp_config().getLogoimage()));
            write(FileUtil.getWall(8888), Utils.getAdminUrl(object.getData().getSiteConfig().getApp_config().getSplashimage()));
            write(FileUtil.getWall(9999), Utils.getAdminUrl(object.getData().getSiteConfig().getApp_config().getPlayerimage()));
            String backdropImage = Utils.getAdminUrl(object.getData().getSiteConfig().getApp_config().getBackdropimage());
            int defaultPlayer = Integer.parseInt(object.getData().getSiteConfig().getDefault_player());
            if (Prefers.getInt("player", 888888) == 888888) {
                Setting.putPlayer(defaultPlayer - 1);
            }
            if (backdropImage.length() < 8) Hawk.delete(HawkConfig.API_BACKGROUND);
            else Hawk.put(HawkConfig.API_BACKGROUND, backdropImage);
        }
    }

    private void initAdmJson(Adm admConfig) {
        String about = admConfig.getData().getSiteConfig().getApp_config().getAbout();
        String text = new String(Base64.decode(about, Base64.DEFAULT));
        if (!text.equals("{}") && Json.valid(text)) {
            HawkCustom.get().addConfig(text);
        } else {
            Hawk.put(HawkCustom.HAS_CONFIG, false);
        }
    }

    private boolean isDeleteDepot(String url, List<Config> yConfig) {
        for (int i = 0; i < yConfig.size(); i++) {
            if (Objects.equals(yConfig.get(i).getUrl(), url)) {
                return false;
            }
        }
        return true;
    }

    private void write(File file, String imgUrl) {
        if (!imgUrl.startsWith("http")) {
            file.delete();
        } else {
            App.execute(() -> {
                try {
                    Path.write(file, OkHttp.newCall(Utils.getAdminUrl(imgUrl)).execute().body().bytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * count 小于3是后台地址,如: https://app.lvdoui.net
     * count 不小于3非后台地址,如: https://app.lvdoui.net/data.json
     **/
    private HttpParams mGtParams(int count) {
        HttpParams mParams = new HttpParams();
        if (count < 3) {
            mParams.put("time", "time");
            mParams.put("token", HawkUser.token());
            mParams.put("app_id", Utils.getAppId());
            mParams.put("apk_mark", Utils.getAppMark());
            mParams.put("version", BuildConfig.VERSION_NAME);
        }
        return mParams;
    }

    private void setUrls(String urls) {
        Hawk.put(ADM_URLS, urls);
    }

    private String getUrls() {
        return Hawk.get(ADM_URLS, "{}");
    }

    public static int sub_str_count(String str, char c) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    public static String getAdmConfig(String defaultValue) {
        return Hawk.get(ADM_CONFIG, defaultValue);
    }

    public static Adm loadAmdConfig(@Nullable String defaultValue) {
        Intrinsics.checkParameterIsNotNull(defaultValue, "defaultValue");
        String msg = getAdmConfig(defaultValue);
        return msg == null || ((CharSequence) msg).length() == 0 ? null : Adm.objectFromData(msg);
    }

    public static String getService() {
        Adm.DataBean.SiteConfigBean siteConfig = getSiteConfig();
        return siteConfig == null || siteConfig.getApp_config().getServiceimage().length() < 8 ? null : siteConfig.getApp_config().getServiceimage();
    }

    public static String getQqgroup() {
        Adm.DataBean.SiteConfigBean siteConfig = getSiteConfig();
        return siteConfig == null || siteConfig.getApp_config().getQqgroup().length() < 8 ? null : siteConfig.getApp_config().getQqgroup();
    }

    public static String hideParse() {
        return getSiteConfig().getDepot_parses_hide();
    }

    public static String hideSite() {
        return getSiteConfig().getDepot_site_hide();
    }

    public static Adm.DataBean.SiteConfigBean getSiteConfig() {
        Adm adm = loadAmdConfig("");
        return adm == null || adm.getCode() != 1 ? null : adm.getData().getSiteConfig();
    }

    public static List<Adm.DataBean.HomeConfigBean> getHomeConfig() {
        Adm adm = loadAmdConfig("");
        return adm == null || adm.getCode() != 1 ? null : adm.getData().getHomeConfig();
    }

    public static List<Adm.DataBean.NoticeListBean> getNoticeList() {
        Adm adm = loadAmdConfig("");
        return adm == null || adm.getCode() != 1 ? null : adm.getData().getNoticeList();
    }
}
