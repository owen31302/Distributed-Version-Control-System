package com.owen31302.quorumcloud;

/**
 * Created by owen on 3/8/17.
 */
public enum HostPort {

    SERVER1(10000),SERVER2(10001),SERVER3(10002),SERVER4(10003),SERVER5(10004);
    private int _value;
    public static int count = HostPort.values().length;

    HostPort(int value){
        this._value = value;
    }

    public int getValue(){
        return _value;
    }
}

