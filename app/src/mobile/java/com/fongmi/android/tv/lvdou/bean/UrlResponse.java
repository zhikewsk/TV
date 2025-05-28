package com.fongmi.android.tv.lvdou.bean;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;

import java.util.List;

public class UrlResponse {

    private int time;
    private List<UrlItem> urls;

    public static UrlResponse object(String str) {

        return App.gson().fromJson(str, UrlResponse.class);
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public List<UrlItem> getUrls() {
        return urls;
    }

    public void setUrls(List<UrlItem> urls) {
        this.urls = urls;
    }

    public static class UrlItem {

        private String url;
        private String name;

        public static UrlItem object(String str) {

            return App.gson().fromJson(str, UrlItem.class);
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @NonNull
        @Override
        public String toString() {
            return "UrlItem{" +
                    "url='" + url + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "UrlResponse{" +
                "time=" + time +
                ", urls=" + urls +
                '}';
    }
}
