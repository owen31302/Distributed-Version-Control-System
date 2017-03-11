package com.owen31302.quorumcloud;

/**
 * Created by owen on 3/9/17.
 */
public enum UIFSM {
    IDLE(0), INITIALRETRIEVE(1), SET(2), PRINTALL(3), GET(4), WRITE(5), PUSH(6), MERGE(7);
    private int _value;
    public static int count = UIFSM.values().length;

    UIFSM(int value){
        this._value = value;
    }

    public int getValue(){
        return _value;
    }
}
