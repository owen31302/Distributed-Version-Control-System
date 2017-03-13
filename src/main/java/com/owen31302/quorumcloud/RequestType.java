package com.owen31302.quorumcloud;

/**
 * Created by owen on 3/9/17.
 */
public class RequestType {
    public static final int CHECKCONNECTION = 0;
    public static final int INITIALRETRIEVE = 1;
    public static final int SET = 2;
    public static final int GET = 3;
    public static final int PUSH = 4;
    public static final int CORRUPT_TIMESTAMP = 5;
    public static final int CORRUPT_VALUE = 6;
    public static final int SHUTDOWN = 7;
}
