package com.fongmi.android.tv.lvdou;

import android.text.TextUtils;
import android.util.Log;

import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.lvdou.bean.AdmUser;
import com.fongmi.android.tv.lvdou.bean.UrlResponse;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HawkCustom {

    public static final String HAS_CONFIG = "has_config";
    private static final String CUSTOM_CONFIG = "custom_config";

    private static class Loader {
        static volatile HawkCustom INSTANCE = new HawkCustom();
    }

    public boolean hasConfig() {
        return Hawk.get(HAS_CONFIG, false);
    }

    public static HawkCustom get() {
        return Loader.INSTANCE;
    }

    public void load(Callback callback) {
        OkGo.<String>get(Utils.getApi("uploads/tvbox/config/" + Utils.getAppId() + ".json"))
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        String body = response.body();
                        Hawk.put(CUSTOM_CONFIG, body);
                        UrlResponse data = UrlResponse.object(body);
                        if (data != null && data.getUrls() != null && !data.getUrls().isEmpty()) {
                            Hawk.put(HawkAdm.ADM_URLS, body);
                        }
                        callback.success();
                    }

                    @Override
                    public void onError(Response<String> error) {
                        String body = error.body();
                        callback.error(body);
                    }
                });
    }

    public void addConfig(String body) {
        Hawk.put(CUSTOM_CONFIG, body);
        Hawk.put(HAS_CONFIG, true);
    }

    public int getConfig(String fieldName, int defaultValue) {
        JSONObject jsonArray = getConfig();
        if (jsonArray == null) return 0;
        return jsonArray.optInt(fieldName, defaultValue);
    }

    public String getConfig(String fieldName, String defaultValue) {
        JSONObject jsonArray = getConfig();
        if (jsonArray == null) return "";
        return jsonArray.optString(fieldName, defaultValue);
    }

    public boolean getConfig(String fieldName, boolean defaultValue) {
        JSONObject jsonArray = getConfig();
        if (jsonArray == null) return false;
        String value = jsonArray.optString(fieldName);
        if (!TextUtils.isEmpty(value) && !value.isEmpty()) {
            if ("custom_depot".equals(fieldName)) {
                if ("自动".equals(value) && HawkUser.isLifeVip()) return true;
            }
            return "开启".equals(value);
        }
        return defaultValue;
    }

    //返回某个项
    public String getConfigItem(String sectionName, String targetName) {
        String values = getConfigByName(sectionName, targetName, "");
        if (values == null) return "";
        return values;
    }

    //返回某个值
    public String getConfigValue(String sectionName, String targetName, String value) {
        String values = getConfigByName(sectionName, targetName, value);
        if (values == null) return "";
        return values;
    }

    //返回奖励值
    public int getConfigReward(String sectionName, String targetName, String value) {
        String values = getConfigByName(sectionName, targetName, value);
        if (values == null || values.isEmpty()) return 0;
        return Integer.parseInt(values);
    }

    /**
     * 返回自定义配置某个值
     *
     * @param sectionName 如 "sdk"、"img"
     * @param targetName 如 "激励视频"
     * @param value 如 "reward"、"event"、”url“
     */
    private String getConfigByName(String sectionName, String targetName, String value) {
        try {
            JSONArray jsonArray = getConfig().getJSONArray(sectionName);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                if (item.getString("name").equals(targetName)) {
                    if (value.isEmpty()) return item.toString();
                    if (item.has(value)) return item.getString(value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject getConfig() {
        JSONObject jsonObject = null;
        String config = Hawk.get(CUSTOM_CONFIG, "{}");
        if (!config.equals("{}")) {
            try {
                jsonObject = new JSONObject(config);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return jsonObject;
    }
}
