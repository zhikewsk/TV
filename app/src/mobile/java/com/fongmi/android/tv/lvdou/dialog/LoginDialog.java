package com.fongmi.android.tv.lvdou.dialog;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.lvdou.AdmUtils;
import com.fongmi.android.tv.lvdou.bean.AdmUser;
import com.fongmi.android.tv.lvdou.impl.UserEvent;
import com.fongmi.android.tv.ui.dialog.BaseDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.lvdou.HawkUser;
import com.fongmi.android.tv.databinding.DialogLoginBinding;
import com.fongmi.android.tv.utils.Notify;

import com.orhanobut.hawk.Hawk;

public class LoginDialog extends BaseDialog {

    private Context context;
    private DialogLoginBinding binding;

    public static LoginDialog create() {
        return new LoginDialog();
    }

    public LoginDialog() {

    }

    public void show(FragmentActivity activity) {
        context = activity;
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) {
            if (f instanceof BottomSheetDialogFragment) return;
        }
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogLoginBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.etUser.addTextChangedListener(textWatcher());
        binding.etPass.addTextChangedListener(textWatcher());
        binding.etUser.setText(Hawk.get(HawkUser.USER_ACCOUNT, ""));
//        ImgUtil.loadVod("验证码", "http://am.lvdoui.net/api/Common/captcha", binding.pngSend);
    }

    private TextWatcher textWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                String userName = binding.etUser.getText().toString();
                String passwd = binding.etPass.getText().toString();
                Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.button_re3_no, null);
                if (userName.length() >= 5 && passwd.length() >= 5) {
                    drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.button_re3, null);
                }
                binding.btnLogin.setBackground(drawable);
            }
        };
    }

    @Override
    protected void initEvent() {
        binding.close.setOnClickListener(v -> dismiss());
        binding.btnLogin.setOnClickListener(this::Login);
    }

    private void Login(View view) {
        String userName = binding.etUser.getText().toString();
        String passwd = binding.etPass.getText().toString();
        if (userName.length() < 5 || passwd.length() < 5) {
            Notify.show("账号及密码不能小于6位");
            return;
        }
        login((userName + "|" + passwd).split("\\|"));
    }

    private void login(String[] paramsAll) {
        new AdmUtils().logon(paramsAll[0] + "|" + paramsAll[1], new Callback() {
            @Override
            public void success(String body) {
                loginStatus(body, paramsAll);
            }

            @Override
            public void error(String error) {
                Log.d("TAG", "error: " + error);
                Notify.show(error);
            }
        });
    }

    private void register(String[] paramsAll) {
        new AdmUtils().register(paramsAll[0] + "|" + paramsAll[1], new Callback() {
            @Override
            public void success(String body) {
                loginStatus(body, paramsAll);
            }

            @Override
            public void error(String error) {
                Log.d("TAG", "error: " + error);
                Notify.show(error);
            }
        });
    }

    private void loginStatus(String body, String[] paramsAll) {
        if (!TextUtils.isEmpty(body)) {
            AdmUser userData = App.gson().fromJson(body, AdmUser.class);
            if (userData == null) {
                Notify.show("登录失败~请再试一次或联系客服");
                return;
            }
            if (userData.getCode() == 1) {
                Hawk.put(HawkUser.USER_ACCOUNT, paramsAll[0]);
                HawkUser.saveUser(userData);
                Notify.show(userData.getMsg());
                UserEvent.userInfo();
                dismiss();
            } else {
                if (userData.getMsg().equals("账户不正确")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("账号不存在")
                            .setMessage("是否使用" + paramsAll[0] + "进行注册?")
                            .setPositiveButton("确定", (dialog, which) -> register(paramsAll))
                            .setNegativeButton("取消", null)
                            .create()
                            .show();
                    return;
                }
                Notify.show(userData.getMsg());
            }
        } else {
            Notify.show("登录失败~请再试一次或联系客服");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
