package com.fongmi.android.tv.bean;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.db.AppDatabase;
import com.github.catvod.utils.Json;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Entity
public class Live {

    @NonNull
    @PrimaryKey
    @SerializedName("name")
    private String name;

    @Ignore
    @SerializedName("type")
    private int type;

    @Ignore
    @SerializedName("group")
    private String group;

    @Ignore
    @SerializedName("url")
    private String url;

    @Ignore
    @SerializedName("jar")
    private String jar;

    @Ignore
    @SerializedName("logo")
    private String logo;

    @Ignore
    @SerializedName("epg")
    private String epg;

    @Ignore
    @SerializedName("ua")
    private String ua;

    @Ignore
    @SerializedName("click")
    private String click;

    @Ignore
    @SerializedName("origin")
    private String origin;

    @Ignore
    @SerializedName("referer")
    private String referer;

    @Ignore
    @SerializedName("timeout")
    private Integer timeout;

    @Ignore
    @SerializedName("header")
    private JsonElement header;

    @Ignore
    @SerializedName("playerType")
    private Integer playerType;

    @Ignore
    @SerializedName("channels")
    private List<Channel> channels;

    @Ignore
    @SerializedName("groups")
    private List<Group> groups;

    @Ignore
    @SerializedName("catchup")
    private Catchup catchup;

    @Ignore
    @SerializedName("core")
    private Core core;

    @SerializedName("boot")
    private boolean boot;

    @SerializedName("pass")
    private boolean pass;

    @Ignore
    private boolean activated;

    @Ignore
    private int width;

    public static Live objectFrom(JsonElement element) {
        return App.gson().fromJson(element, Live.class);
    }

    public static List<Live> arrayFrom(String str) {
        Type listType = new TypeToken<List<Live>>() {}.getType();
        List<Live> items = App.gson().fromJson(str, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public Live() {
    }

    public Live(String url) {
        this.name = url.startsWith("file") ? new File(url).getName() : Uri.parse(url).getLastPathSegment();
        this.url = url;
    }

    public Live(@NonNull String name, String url) {
        this.name = name;
        this.url = url;
    }

    public int getType() {
        return type;
    }

    public boolean isBoot() {
        return boot;
    }

    public void setBoot(boolean boot) {
        this.boot = boot;
    }

    public boolean isPass() {
        return pass;
    }

    public void setPass(boolean pass) {
        this.pass = pass;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public String getGroup() {
        return TextUtils.isEmpty(group) ? "" : group;
    }

    public String getUrl() {
        return TextUtils.isEmpty(url) ? "" : url;
    }

    public String getJar() {
        return TextUtils.isEmpty(jar) ? "" : jar;
    }

    public String getLogo() {
        return TextUtils.isEmpty(logo) ? "" : logo;
    }

    public String getEpg() {
        return TextUtils.isEmpty(epg) ? "" : epg;
    }

    public String getUa() {
        return TextUtils.isEmpty(ua) ? "" : ua;
    }

    public String getOrigin() {
        return TextUtils.isEmpty(origin) ? "" : origin;
    }

    public String getReferer() {
        return TextUtils.isEmpty(referer) ? "" : referer;
    }

    public String getClick() {
        return TextUtils.isEmpty(click) ? "" : click;
    }

    public Integer getTimeout() {
        return timeout == null ? Constant.TIMEOUT_PLAY : Math.max(timeout, 1) * 1000;
    }

    public JsonElement getHeader() {
        return header;
    }

    public int getPlayerType() {
        return playerType == null ? -1 : Math.min(playerType, 2);
    }

    public List<Channel> getChannels() {
        return channels = channels == null ? new ArrayList<>() : channels;
    }

    public List<Group> getGroups() {
        return groups = groups == null ? new ArrayList<>() : groups;
    }

    public Catchup getCatchup() {
        return catchup == null ? new Catchup() : catchup;
    }

    public Core getCore() {
        return core == null ? new Core() : core;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public void setActivated(Live item) {
        this.activated = item.equals(this);
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public boolean isEmpty() {
        return getName().isEmpty();
    }

    public Live check() {
        boolean proxy = getChannels().size() > 0 && getChannels().get(0).getUrls().size() > 0 && getChannels().get(0).getUrls().get(0).startsWith("proxy");
        if (proxy) setProxy();
        return this;
    }

    private void setProxy() {
        this.url = getChannels().get(0).getUrls().get(0);
        this.name = getChannels().get(0).getName();
        this.type = 2;
    }

    public Group find(Group item) {
        for (Group group : getGroups()) if (group.getName().equals(item.getName())) return group;
        getGroups().add(item);
        return item;
    }

    public int getBootIcon() {
        return isBoot() ? R.drawable.ic_live_boot : R.drawable.ic_live_block;
    }

    public int getPassIcon() {
        return isPass() ? R.drawable.ic_live_block : R.drawable.ic_live_pass;
    }

    public Live boot(boolean boot) {
        setBoot(boot);
        return this;
    }

    public Live pass(boolean pass) {
        getGroups().clear();
        setPass(pass);
        return this;
    }

    public Live sync() {
        Live item = find(getName());
        if (item == null) return this;
        setBoot(item.isBoot());
        setPass(item.isPass());
        return this;
    }

    public Map<String, String> getHeaders() {
        Map<String, String> headers = Json.toMap(getHeader());
        if (!getUa().isEmpty()) headers.put(HttpHeaders.USER_AGENT, getUa());
        if (!getOrigin().isEmpty()) headers.put(HttpHeaders.ORIGIN, getOrigin());
        if (!getReferer().isEmpty()) headers.put(HttpHeaders.REFERER, getReferer());
        return headers;
    }

    public static Live find(String name) {
        return AppDatabase.get().getLiveDao().find(name);
    }

    public void save() {
        AppDatabase.get().getLiveDao().insertOrUpdate(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Live)) return false;
        Live it = (Live) obj;
        return getName().equals(it.getName());
    }
}
