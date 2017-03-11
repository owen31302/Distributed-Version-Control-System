package com.owen31302.quorumcloud.past;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by owen on 3/9/17.
 */
public interface RmiServerInterface extends Remote{
    public String getMessage() throws RemoteException;
}
