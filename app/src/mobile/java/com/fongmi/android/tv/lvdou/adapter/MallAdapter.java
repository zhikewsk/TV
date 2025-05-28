package com.fongmi.android.tv.lvdou.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.AdapterMallBinding;
import com.fongmi.android.tv.lvdou.bean.AdmGroup;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MallAdapter extends RecyclerView.Adapter<MallAdapter.ViewHolder> {

    private int selectedItemPosition = RecyclerView.NO_POSITION;
    private final List<AdmGroup.DataBean> mItems;
    private final OnClickListener mListener;
    private int width, height;
    private int type;

    public interface OnClickListener {
        void onItemClick(AdmGroup.DataBean item);
    }

    public MallAdapter(OnClickListener listener, int type) {
        this.mItems = new ArrayList<>();
        this.mListener = listener;
        this.type = type;
    }

    public void setSize(int[] size) {
        this.width = size[0];
        this.height = size[1];
    }

    public void addAll(List<AdmGroup.DataBean> items) {
        List<AdmGroup.DataBean> filteredItems = new ArrayList<>();
        for (AdmGroup.DataBean item : items) if (item.getPrice() > 0) filteredItems.add(item);
        mItems.clear();
        mItems.addAll(filteredItems);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolder holder = new ViewHolder(AdapterMallBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        holder.binding.getRoot().getLayoutParams().width = width;
        holder.binding.getRoot().getLayoutParams().height = height;
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdmGroup.DataBean item = mItems.get(position);
        double price = item.getPrice();
        holder.binding.message.setText(item.getIntro());
        holder.binding.name.setText(item.getName());
        holder.binding.money.setText(String.valueOf(price));
        if (type == 2) {
            holder.binding.s.setVisibility(View.GONE);
            holder.binding.money.setText(String.valueOf(price * 100).replaceAll("\\.0", ""));
            holder.binding.remark.setText("积分");
        } else {
            holder.binding.s.setVisibility(View.VISIBLE);
            holder.binding.remark.setText(setRemark(price, item.getDays()));
        }
        setClickListener(holder.binding.getRoot(), position, item);
    }

    private String setRemark(double money, int days) {
        double result = money / days;
        DecimalFormat decimalFormat = new DecimalFormat("#0.00");
        String formattedResult = decimalFormat.format(result);
        return formattedResult + "元/天";
    }

    private void setClickListener(View root, int position, AdmGroup.DataBean item) {
        root.setOnClickListener(view -> {
            if (selectedItemPosition == position) {
                selectedItemPosition = RecyclerView.NO_POSITION;
                mListener.onItemClick(null);
            } else {
                selectedItemPosition = position;
                mListener.onItemClick(item);
            }
            notifyDataSetChanged();
        });
        if (position == selectedItemPosition) {
            root.setBackgroundResource(R.drawable.selector_item_unchecked);
        } else {
            root.setBackgroundResource(R.drawable.selector_item_select);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterMallBinding binding;

        ViewHolder(@NonNull AdapterMallBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
