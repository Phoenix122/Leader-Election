/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hs;

import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Random;

/**
 *  The Ringer exports a remote method that nodes can use 
 *  to register themselves into the Ring.
 * 
 *  The Ringer joins the nodes together and then chooses a random
 *  node to begin the leader election process
 * 
 * @author Surya Selvaraj
 *
 */


public class Ringer extends UnicastRemoteObject implements Remote, Loggable, 
        Reachable, RingerService {
    
    private static Ringer ringer; //Ringer object

    public final int MAX_NODES; //Max #nodes

    private LinkedList<NodeService> ring; //LinkedList to hold the ring nodes
    
    private ReportSet reportSet;

   // private boolean started = false;
    
    private final String SERVICE_NAME = "Ringer"; //This is the Ringer
    
    private static final String EXPORT_FILE = "/Users/SuryaSelvaraj/Documents/Hirschberg-Sinclair-master/stats.txt"; //The file to write the stats
    
    public Ringer( int maxNodes ) throws RemoteException { //Constructor
        super(); //Super Class Constructor is executed first
        ring = new LinkedList<NodeService>(); //The LinkedList for the ring nodes is instantiated
        reportSet = new ReportSet( SERVICE_NAME, maxNodes ); //ReportSet is instantiated
        MAX_NODES = maxNodes; //Storing max #nodes
    }
    
    @Override
    public String logIdent() { //Implementing Logger, so need to override logIdent()
        return SERVICE_NAME;
    }
    
    @Override
    public String reachMeAt() { //Implementing Reachable, so need to override reachMeAt()
        return logIdent();
    }

    //Register node to RMI registry. Needs to be synchronized to maintain transactional properties
    @Override
    public synchronized void registerNode( NodeService node )
            throws RemoteException,
            NotBoundException, UnknownHostException, MalformedURLException
            
    {
        
        if( ring.size() < MAX_NODES ) { //Check if ring size < Max #nodes

            node.left( ring.peekLast() ); //Setting the left pointer of the current node
            node.right( ring.peekFirst() ); //Setting the right pointer of the current node
            
            ring.add( node ); //Adding the current node to the ring LinkedList

            //Updating the log with the information of the node registered, #spots left, current size of ring
            Logger.info( String.format( "Node-%x registered with registry "
                    + "(%d spots left, current ring size: %d)", 
                    node.pid(), MAX_NODES - ring.size(), ring.size() ), this );
            
        } 
        
        if( ring.size() == MAX_NODES ) { //Completed the construction of ring topology
            init();
        }
        
    }

    //Randomly choose a node for activation of the leader election process
    private void init() throws RemoteException {
        try {
            Thread.sleep( 10000 );
        } catch( InterruptedException ie ) {
            Logger.error( "init() sleep messed up: " + ie, this );
        }
        
        Random rand = new Random();
        rand.setSeed( Calendar.getInstance().getTimeInMillis() );
        
        NodeService n = ring.get( rand.nextInt( ring.size() ) );
        //NodeService n = ring.peekFirst();
        n.activate();
        //started = true;

        //Update log with information on the node chosen for activation
        Logger.info( String.format( "Node-%x has been chosen for "
                + "activation", n.pid() ), this );
    }

    //Checking for preparing report
    @Override
    public void report( Report report ) throws RemoteException {
        reportSet.add( report );
        
        if( reportSet.size() == MAX_NODES && reportSet.available() ) {
            new Thread( new Runnable() {
                @Override
                public void run() {
                    prepareReport();
                }
            }).start();
        }
    }

    //Preparing Report, printing the report's content and updating on the file
    private void prepareReport() {
        Logger.info( "Releasing report...", this );
        
        System.out.println( reportSet.summary() );

        reportSet.export( String.format( "%s", EXPORT_FILE ) );

        //Done, so initiating shutdown
        shutdown();
    }

    //Shutdown
    private void shutdown() {
        try {
            Naming.unbind( ringer.reachMeAt() );
            UnicastRemoteObject.unexportObject(ringer, true);
            Logger.info( "Ringer is shutting down", this);
        } catch( Exception e ) {
            Logger.error( "Failed to shut down Ringer: " + e, this );
        }
        
        System.exit( 0 );
    }

    //psvm()
    public static void main( String[] args ) {
        int max; //To store max #nodes
        
        System.setSecurityManager( new RMISecurityManager() );
        
        try {
            
            if( args.length >= 1 ) //To accept max #nodes via command line arguments
                max = Integer.parseInt( args[0] );
            else //Default value is set to 10
                max = 10;

            ringer = new Ringer( max ); //Ringer object instantiated with max #nodes
            
            LocateRegistry.createRegistry( PORT ); //Creating RMI Registry on port

            Logger.info( "Registry created on port " + PORT, ringer ); //Updating log
            
            Naming.rebind( ringer.reachMeAt(), ringer ); //Naming binding
            
            Logger.info( "Ringer bound and loaded....", ringer ); //Updating log
            
            Logger.info( "Waiting for " + ringer.MAX_NODES + 
                    " node registrations...", ringer ); //Updating log
            
        } catch( UnknownHostException uhe ) {
            Logger.error( "Host not recognized: " + uhe, ringer );
        } catch( RemoteException re ) {
            Logger.error( "Error starting service: " + re, ringer );
        } catch( Exception e ) {
            Logger.error( "Exception occurred: " + e, ringer );
        } finally {
            
        }
    }
}
