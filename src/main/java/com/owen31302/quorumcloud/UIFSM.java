package com.owen31302.quorumcloud;

/**
 * Created by owen on 3/9/17.
 */
public enum UIFSM {
    IDLE(0), GET(1), SET(2), PRINTALL(3);
    private int _value;
    public static int count = UIFSM.values().length;

    UIFSM(int value){
        this._value = value;
    }

    public int getValue(){
        return _value;
    }
}
