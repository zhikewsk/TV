package com.fongmi.android.tv.lvdou;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.youth.banner.adapter.BannerAdapter;

import java.util.List;

/**
 * 自定义布局，图片+标题+数字指示器
 */
public class HomeBannerAdapter extends BannerAdapter<Banner, HomeBannerAdapter.BannerViewHolder> {

    public HomeBannerAdapter(List<Banner> mDatas) {
        super(mDatas);
    }

    @Override
    public BannerViewHolder onCreateHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_banner, parent, false);
        return new BannerViewHolder(view);
    }

    private boolean showNativeExpressAD = true;

    //绑定数据
    @Override
    public void onBindView(BannerViewHolder holder, Banner data, int position, int size) {
//        holder.indicator.setText((position + 1) + "/" + size);
        showImg(holder, data);
        holder.title.setText(data.title);
        holder.container.setVisibility(data.viewType != 8 ? View.GONE : View.VISIBLE);
    }

    private void showImg(BannerViewHolder holder, Banner data){
        Glide.with(holder.itemView)
                .load(data.imageUrl)
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(30)))
                .into(holder.image);
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, indicator;
        FrameLayout container;

        public BannerViewHolder(@NonNull View view) {
            super(view);
            image = view.findViewById(R.id.image);
            title = view.findViewById(R.id.title);
            container = view.findViewById(R.id.container);
//            indicator = view.findViewById(R.id.indicator);
//            new AdSdkUtils().nativeExpressAD(App.activity(), container, code -> {});
        }
    }
}
