
package hs;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author Surya Selvaraj
 */
public interface Reachable extends Remote { //Specifying the Host and Port
    
    public static final String HOST_NAME = "localhost";
    public static final int PORT = 1099;
    
    public String reachMeAt() throws RemoteException;
    
}
