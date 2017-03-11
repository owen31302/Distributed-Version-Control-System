package com.owen31302.quorumcloud;

import java.io.Serializable;

/**
 * Created by owen on 3/9/17.
 */
public class VersionData implements Serializable {
    private int _val;
    private long _timestamp;

    public VersionData(int val){
        this._val = val;
    }

    public VersionData(int val, long timestamp){
        this._val = val;
        this._timestamp = timestamp;
    }

    public int get_val(){
        return _val;
    }
    public long get_timestamp(){
        return _timestamp;
    }
}

