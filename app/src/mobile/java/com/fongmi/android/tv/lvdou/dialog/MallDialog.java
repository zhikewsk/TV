package com.fongmi.android.tv.lvdou.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogMallBinding;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.lvdou.AdmUtils;
import com.fongmi.android.tv.lvdou.HawkConfig;
import com.fongmi.android.tv.lvdou.HawkUser;
import com.fongmi.android.tv.lvdou.Utils;
import com.fongmi.android.tv.lvdou.adapter.MallAdapter;
import com.fongmi.android.tv.lvdou.bean.AdmGroup;
import com.fongmi.android.tv.lvdou.bean.AdmUser;
import com.fongmi.android.tv.lvdou.impl.UserEvent;
import com.fongmi.android.tv.ui.activity.WebActivity;
import com.fongmi.android.tv.ui.dialog.BaseDialog;
import com.fongmi.android.tv.utils.Notify;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.orhanobut.hawk.Hawk;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Objects;

public class MallDialog extends BaseDialog {

    private String data;
    private double money;
    private Context context;
    private DialogMallBinding binding;

    public static MallDialog create(String data, double money) {
        return new MallDialog(data, money);
    }

    public MallDialog(String data, double money) {
        this.data = data;
        this.money = money;
    }

    public void show(FragmentActivity activity) {
        try {
            this.context = activity;
            this.userInfo = HawkUser.userInfo();
            for (Fragment f : activity.getSupportFragmentManager().getFragments()) {
                if (f instanceof BottomSheetDialogFragment) return;
            }
            show(activity.getSupportFragmentManager(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogMallBinding.inflate(inflater, container, false);
    }

    @Override
    @SuppressLint("SetTextI18n")
    protected void initView() {
        setRecyclerView();
        admGroup = AdmGroup.objectFrom(data);
        if (admGroup != null) mAdapter.addAll(admGroup.getData());
        binding.balance.setText("余额￥" + userInfo.getMoney());
        int orientation = getResources().getConfiguration().orientation;
        if (orientation != Configuration.ORIENTATION_PORTRAIT) {
            binding.privilege.setVisibility(View.GONE);
            binding.rule.setVisibility(View.GONE);
        }
    }

    @Override
    protected void initEvent() {
        binding.cami.setOnClickListener(this::onCami);
        binding.pay.setOnClickListener(this::createOrder);
        binding.payRule.setOnClickListener(v -> WebActivity.start(getActivity(), Utils.getApi("uploads/tvbox/web/payrule.html")));
        binding.payTutorial.setOnClickListener(v -> WebActivity.start(getActivity(), Utils.getApi("uploads/tvbox/web/paytutorial.html")));
        binding.payProblem.setOnClickListener(v -> WebActivity.start(getActivity(), Utils.getApi("uploads/tvbox/web/payproblem.html")));
        binding.radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.zfb) payType = "alipay";
            if (checkedId == R.id.wx) payType = "wechat";
        });
    }

    private void onCami(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("请输入卡密");
        final EditText cardCodeInput = new EditText(context);
        builder.setView(cardCodeInput);
        builder.setPositiveButton("兑换", (dialog, which) -> {
            String cardCode = cardCodeInput.getText().toString();
            new AdmUtils().camiRecharge(cardCode, new Callback() {
                @Override
                public void success(String body) {
                    try {
                        JSONObject jsonObject = new JSONObject(body);
                        if (jsonObject.getInt("code") == 1) {
                            Notify.show(jsonObject.getString("msg"));
                            dialog.dismiss();
                        } else Notify.show(jsonObject.getString("msg"));
                    } catch (JSONException e) {
                        Notify.show("数据异常");
                        e.fillInStackTrace();
                    }
                }

                @Override
                public void error(String error) {
                    Notify.show("请求服务器失败~");
                }
            });
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private AdmGroup admGroup;
    private double balance;
    private MallAdapter mAdapter;
    private AdmGroup.DataBean mItems;
    private AdmUser.DataBean.UserinfoBean userInfo;
    private String payType = "wechat";

    @SuppressLint("SetTextI18n")
    private void setRecyclerView() {
        binding.recycler.setHasFixedSize(true);
        Objects.requireNonNull(binding.recycler.getItemAnimator()).setChangeDuration(0);
        binding.recycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recycler.setAdapter(mAdapter = new MallAdapter(item -> {
            binding.balance.setText("余额￥" + userInfo.getMoney());
            mItems = item;
            if (item == null) {
                binding.payMoney.setText("请选择");
            } else {
                DecimalFormat df = new DecimalFormat("#.##");
                df.setRoundingMode(java.math.RoundingMode.HALF_UP);
                double userMoney = userInfo.getMoney();
                balance = Double.parseDouble(df.format(userMoney - mItems.getPrice()));
                if (balance >= 0) binding.payMoney.setText("支付￥0");
                else binding.payMoney.setText("支付￥" + Math.abs(balance));
            }
        }, 1));
        mAdapter.setSize(Product.getSpec(context));
    }

    private void createOrder(View view) {
        if (mItems == null) {
            Notify.show("请先选择套餐");
        } else if (mItems.getPrice() <= 0) {
            Notify.show("套餐价格有误");
        } else {
            if (balance >= 0) {
                new AdmUtils().upgradeGroup(mItems.getId(), new Callback() {
                    @Override
                    public void success(String body) {
                        try {
                            JSONObject jsonObject = new JSONObject(body);
                            String msg = jsonObject.getString("msg");
                            int code = jsonObject.getInt("code");
                            if (code == 1) {
                                dismiss();
                            }
                            Notify.show(msg);
                        } catch (JSONException e) {
                            Notify.show(e.getMessage());
                            e.fillInStackTrace();
                        }
                    }

                    @Override
                    public void error(String error) {
                        Notify.show("升级会员组失败");
                    }
                });
            } else {
                double finalMoney = Math.abs(balance);
                Hawk.put(HawkConfig.SELECT_PACKAGE, mItems.getId());
                String url = Utils.getApi(HawkConfig.WEB_CREATE_ORDER) + "?money=" + finalMoney + "&paytype=" + payType + "&memo=" + mItems.getName();
                Intent intent = new Intent(requireContext(), WebActivity.class);
                intent.putExtra("url", url);
                startActivityForResult(intent, REQUEST_CODE);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            Log.d("TAG", "onActivityResult: 1");
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("是否已完成支付?");
            builder.setNeutralButton("支付遇到问题", (dialog, which) -> Notify.show("支付遇到问题请点击右上角···联系客服"));
            builder.setNegativeButton("未支付", null);
            builder.setPositiveButton("已支付", (dialog, which) -> upgradeGroup());
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void upgradeGroup(){
        new AdmUtils().upgradeGroup(Hawk.get(HawkConfig.SELECT_PACKAGE, 88888), new Callback() {
            @Override
            public void success(String body) {
                try {
                    JSONObject jsonObject = new JSONObject(body);
                    if (jsonObject.getInt("code") == 1) {
                        Notify.show(jsonObject.getString("msg"));
                        dismiss();
                    } else Notify.show("检测到订单未支付。如有疑问请联系客服");
                } catch (JSONException e) {
                    e.fillInStackTrace();
                    Notify.show("数据异常");
                }
            }

            @Override
            public void error(String error) {
                Notify.show("请求服务器失败~");
            }
        });
    }

    private static final int REQUEST_CODE = 10086;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        UserEvent.userInfo();
    }
}
