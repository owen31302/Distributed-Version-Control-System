package com.owen31302.quorumcloud;

/**
 * Created by owen on 3/11/17.
 */
public interface TestCase {
    void corruptValue();
    void corruptTimestamp();
    void shutDown();
}
