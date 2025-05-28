package com.fongmi.android.tv.lvdou;

import com.fongmi.android.tv.lvdou.bean.Adm;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Banner {

    public String title;
    public String imageUrl;
    public String parameter;

    public int viewType;

    public Banner(String imageUrl, String title, int viewType, String parameter) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.viewType = viewType;
        this.parameter = parameter;
    }

    public static List<Banner> getHomeBanner() {
        List<Banner> list = new ArrayList<>();
        List<Adm.DataBean.HomeConfigBean> homeData = HawkAdm.getHomeConfig();
        for (int i = 0; i < Objects.requireNonNull(homeData).size(); i++) {
            String img = Utils.getAdminUrl(homeData.get(i).getCoverimage());
            String parameter = homeData.get(i).getParameter();
            int viewType = parameter.equals("sdk") ? 8 : 1;
            String title = homeData.get(i).getTitle();
            list.add(new Banner(img, title, viewType, parameter));
        }
        return list;
    }
}
