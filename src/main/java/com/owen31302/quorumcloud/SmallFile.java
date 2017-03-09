package com.owen31302.quorumcloud;

/**
 * Created by owen on 3/9/17.
 */
public class SmallFile {
    private int _val;
    private int _timestamp;

    public SmallFile(int val, int timestamp){
        this._val = val;
        this._timestamp = timestamp;
    }

    public int get_val(){
        return _val;
    }
    public int get_timestamp(){
        return _timestamp;
    }
}
