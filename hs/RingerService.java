package hs;

import java.net.MalformedURLException;
import java.rmi.*;


/**
 *
 * @author Surya Selvaraj
 */
public interface RingerService extends Remote {
    //Registering a node to RMI Registry - Override in Ringer class
    public void registerNode( NodeService n ) throws RemoteException,
            NotBoundException, UnknownHostException, MalformedURLException;
    //Preparing Report - Override in Ringer class
    public void report( Report report ) throws RemoteException;
}
