package com.fongmi.android.tv.lvdou.impl;

import com.fongmi.android.tv.event.RefreshEvent;

import org.greenrobot.eventbus.EventBus;

public class UserEvent {

    private final Type type;
    private String parameter;
    private int code;

    public static void userInfo() {
        EventBus.getDefault().post(new UserEvent(Type.USERINFO));
    }

    public static void money() {
        EventBus.getDefault().post(new UserEvent(Type.MONEY));
    }

    public static void score() {
        EventBus.getDefault().post(new UserEvent(Type.SCORE));
    }

    public static void payment(int code) {
        EventBus.getDefault().post(new UserEvent(Type.PAYMENT, code));
    }

    private UserEvent(Type type) {
        this.type = type;
    }

    public UserEvent(Type type, String parameter) {
        this.type = type;
        this.parameter = parameter;
    }

    public UserEvent(Type type, int code) {
        this.type = type;
        this.code = code;
    }

    public Type getType() {
        return type;
    }

    public String getParameter() {
        return parameter;
    }

    public int getCode() {
        return code;
    }

    public enum Type {
        USERINFO, MONEY, SCORE, PAYMENT
    }
}
