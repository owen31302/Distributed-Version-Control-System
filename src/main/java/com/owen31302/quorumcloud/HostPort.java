package com.owen31302.quorumcloud;

/**
 * Created by owen on 3/8/17.
 */
public enum HostPort {

    SERVER1(10001),SERVER2(10002),SERVER3(10003),SERVER4(10004),SERVER5(10005);
    private int _value;
    public static int count = HostPort.values().length;

    HostPort(int value){
        this._value = value;
    }

    public int getValue(){
        return _value;
    }
}

