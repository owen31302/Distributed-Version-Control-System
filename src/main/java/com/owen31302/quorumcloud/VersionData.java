package com.owen31302.quorumcloud;

import java.io.Serializable;

/**
 * Created by owen on 3/9/17.
 */
public class VersionData implements Serializable {
    private int _val;

    public VersionData(int val){
        this._val = val;
    }

    public int get_val(){
        return _val;
    }
}

