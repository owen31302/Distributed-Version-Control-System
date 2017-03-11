package com.owen31302.quorumcloud;

/**
 * Created by owen on 3/11/17.
 */
public class TimeStamp {
    private Long _time;

    public TimeStamp(Long time){
        _time = time;
    }

    public void set_time(Long time){
        _time = time;
    }

    public Long get_time(){
        return _time;
    }
}
