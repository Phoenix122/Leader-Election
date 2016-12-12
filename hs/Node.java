package hs;

import java.lang.Thread.State;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.UnknownHostException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 * The Node class represents a single node in the topology. It has
 * server and client capabilites and registers itself through the Ringer
 * Service.
 * 
 * 
 * @author Surya Selvaraj
 */
//Standard Node class impleme nting the features discussed in class
public class Node extends UnicastRemoteObject implements NodeService, Runnable, 
        Loggable  {
   
    private static NodeService node = null; //Current node
    private static RingerService ringer = null; //Topology
    
    private NodeService left; //Left pointer
    private NodeService right; //Right pointer
    
    private long pid; //Process ID
    private long leaderPid; //Leader ID
    
    private boolean participating = false; //Participation flag
    private boolean isLeader = false; //Leader's leader
    private boolean hasLeader = false; //Has leader
    
    private boolean announcedAsLeader = false; //Announced Leader flag
    
    private int phase = 0;
    private int replies = 0;
    
    private boolean activated = false; //Activation flag
    
    
    private Queue<Probe> messages; //Queue to store messages
    
    private Random rand = new Random();
    
    private Thread me;
    
    private Report report;
    
    private Node() throws RemoteException { //Constructor
        super();
        
        rand.setSeed( Calendar.getInstance().getTimeInMillis() );
        
        pid = rand.nextLong(); //Random process ID
        
        messages = new LinkedList<Probe>(); //Messages queue instantiated
        report = new Report( String.format( "Node-%x", pid ) ); //Report object instantiated
    }

    //Creating new nodes
    private static Node newInstance() throws RemoteException {
        Node instance = new Node();
        
        instance.me = new Thread( instance );
        instance.me.setName( instance.logIdent() );
        
        return instance;
    }

    //Activation
    @Override
    public synchronized void activate() {
        if( me.getState() == State.NEW ) {
            me.start();
            activated = true;
            Logger.debug( "I have been activated", this ); //Log update
        }
    }

    //Return Process ID
    @Override
    public long pid() { return pid; }

    //Return left neighbor in ring
    @Override
    public NodeService left() throws RemoteException { return left; }

    //Return right neighbor in ring
    @Override
    public NodeService right() throws RemoteException { return right; }

    //Assigning left neighbor for a given node
    @Override
    public void left( NodeService n ) throws RemoteException {
        left = n;
        if( n != null && ( n.right() == null || n.right().pid() != pid ) )
            n.right( (NodeService)this );
    }

    //Assigning right neighbor for a given node
    @Override
    public void right( NodeService n ) throws RemoteException {
        right = n;
        if( n != null && ( n.left() == null || n.left().pid() != pid ) )
            n.left( (NodeService)this );
    }

    //Just got a probe, adding to messages queue
    @Override
    public void send( Probe probe ) throws RemoteException {
        Logger.debug( String.format( "Just got a probe(%x) of type: %s", 
                probe.id, probe.type ), this );
        if( me.getState() == State.NEW )
            activate();
        
        messages.add( probe );
    }

    //Reading the earliest msg from queue - FIFO
    @Override
    public  Probe receive() throws RemoteException {
        //Logger.debug( "I am trying to read in a message", this );
        return messages.poll();
    }

    //Sending a probe
    private void send( NodeService n, Probe p ) throws RemoteException {
        n.send( p );
        report.msg();
    }

    //Probing
    @Override
    public void probe( MessageType type, Direction direction ) 
            throws RemoteException {
        Probe probe = new Probe( pid, type, phase );
        
        Logger.debug( String.format( "I'm about to probe(%x)", probe.id ), this );

        //Checking direction of probe -> left and both
        if( direction == Direction.BOTH || direction == Direction.LEFT ) {
        
            probe.direction = Direction.LEFT;
            Logger.debug( "before send to left", this );
            send( left, probe );
            Logger.debug( "after send to left", this );
        }

        //Checking direction of probe -> right and both
        if( direction == Direction.BOTH || direction == Direction.RIGHT ) {
        
            probe.direction = Direction.RIGHT;
            Logger.debug( "before send to right", this );
            send( right, probe );
            Logger.debug( "after send to right", this );
        }
        
        Logger.debug( String.format( "Ok, I probed(%x)", probe.id ), this );
        
    }

    //Forwarding the probe based on direction
    private  void forward( Probe probe ) throws RemoteException {
        if( probe.direction == Direction.RIGHT )
            send( right, probe );
        else 
            send( left, probe );
    }

    //Reply to a probe
    private  void reply( Probe probe ) throws RemoteException {
        probe.type = MessageType.REPLY;

        //Replying left
        if( probe.direction == Direction.RIGHT ) {
            probe.direction = Direction.LEFT;
            send( left, probe );
        } else { //Replying right
            probe.direction = Direction.RIGHT;
            send( right, probe );
        }
    }

    //Process a probe
    private  void processProbe( Probe probe ) 
            throws RemoteException {
        
        report.rcv(); // report a received msg
        
        Logger.debug( String.format( "I am processing probe(%x) "
                + "now", probe.id ), this );
        
        probe.last_pid = pid;
        
        probe.hops++;
        
        switch( probe.type ) {
            case ELECTION: //Election probe
                Logger.debug( String.format( "It's an Election probe "
                        + "originally from Node-%x", probe.src_pid ), this );
                if( probe.src_pid > pid ) { //Hop according to Process ID
                    
                    int maxHops = (int)Math.pow( 2, probe.phase );
                    
                    if( probe.hops < maxHops ) { //Hops < Max #Hops

                        Logger.debug( String.format( "Probe max hops (2^%d): "
                                + "%d, it's at %d hops so far...forwarding to "
                                + "the %s", probe.phase, maxHops, 
                                probe.hops, probe.direction ), this );
                        forward( probe );

                    }
                    else if( probe.hops == maxHops ) { //Hops reached max #hops
                        Logger.debug( String.format( "Probe(%x) has reached "
                                + "it's max hops(%d), sending a reply "
                                + "back", probe.id, probe.hops ), this);
                        
                        reply( probe ); //Send a reply
                        
                    } 
                    
                }
                else if( probe.src_pid == pid ) { //Finished a complete circle of probe
                    
                    if( hasLeader ) { //Check if already has a leader
                        Logger.debug( String.format( "I already have a "
                                + "leader: Node-%x", leaderPid ), this );
                        break;
                    }
                    //If it doesn't have a leader, it means it is the leader
                    isLeader = true;
                    hasLeader = true;
                    leaderPid = pid;
                    Logger.debug( "Announcing myself as winner", this );
                    
                    probe( MessageType.ANNOUNCEMENT, Direction.RIGHT ); //Probe an Announcement
                    
                } else //src pid < cuurent pid
                    Logger.debug( String.format( "Swallowed probe(%x) "
                        + "because I'm a greater node than Node-%x", 
                        probe.id, probe.src_pid ), this );
            break;
                
            case REPLY: //Reply Probe
                Logger.debug( String.format( "It's an Reply probe for "
                        + "Node-%x", probe.src_pid ), this );
                if( probe.src_pid != pid ) { //If it is not the reply for me, forward it
                    
                    forward( probe );
                   
                    
                } else { //If the reply is for me, increment reply count
                    replies++;
                    if( replies >= 2 ) { //I got at least 2 replies, Entering next phase
                        phase++;
                        
                        Logger.debug( "I am entering phase " + phase, this );
                        
                        probe( MessageType.ELECTION, Direction.BOTH ); //Probe Election on both sides
                        
                        replies = 0; //Resetting reply count to zero
                    }
                }
                
            break;
                
            case ANNOUNCEMENT: //Announcement probe
                Logger.debug( String.format( "It's an Announcement probe "
                        + "from Node-%x", probe.src_pid ), this );
                if( !hasLeader && probe.src_pid != pid ) { //If I don't have a leader and I am not the one initiated it
                   
                    forward( probe ); //Forward the probe
                    
                    leaderPid = probe.src_pid; //Set the leader as the Process that initiated it
                    hasLeader = true;   //Set hasLeader flag to true
                } else announcedAsLeader = true; //I am the leader
                
            break;
        }
        
        Logger.debug( String.format( "I finished processing probe(%x) "
                + "now", probe.id ), this );
        
    }

    //Implementing Runnable, so override run()
    @Override
    public void run() {
        
        if( Thread.currentThread() != me ) return;
        
        Logger.debug( "I am running. ", this );
        
        if( !participating ) { //If not participating
            try {
                probe( MessageType.ELECTION, Direction.BOTH ); //Probe Election on both sides
            } catch( RemoteException re ) {
                Logger.error( "The service failed: " + re, this );
            }
            participating = true; //Set participating flag to true
            Logger.debug( "I am now participating", this );
        }


        Probe p; //Probe object
        while( true ) {
            //If leader is me and I have been announced
           if( isLeader && announcedAsLeader )
                break;
           
            try {
                //If a probe is received, process it
                if( (p = receive()) != null ) {
                    processProbe( p );

                    //If I have a leader and I am not the leader, I am done. I no longer contend
                    if( hasLeader && !isLeader )
                        break;
                }
                // if you are running over 128 nodes, a lil sleep helped my system cope
                //Thread.sleep( 5000 );
            } catch( RemoteException re ) {
                Logger.error( "The service failed in run(): " + re, this );
                re.printStackTrace();
            } 

 
        }
        
        me.interrupt();
        
        finish();
        
    }
    
    public void finish() {
        try {
            //If I am not the leader, print the leader
            if( !isLeader ) //{
                Logger.info( String.format( "Node-%x has chosen leader: "
                        + "Node-%x", pid, leaderPid ), this );

            else //I am the leader !!
                Logger.info( "I have conquered all...I am leader", this );
            
            Logger.info( "Reporting to Ringer", this );
            
            Node.ringer.report( report );
            
            Thread.sleep( 100 );
            me.join();
            Thread.sleep( 100 );
            
            
            
        } catch( InterruptedException ie ) {
        } catch( RemoteException re ) {
            Logger.error( "The service failed in finish(): " + re, this );
        }
            
        System.exit(0);
    }

    //Implemeting Loggable, so overriding logIdent()
    @Override
    public String logIdent() {
        return String.format( "Node-%x", pid );
    }

    //psvm()
    public static void main( String[] args ) {
        node = null;
        try {
            //Instantiating Node object
           node = Node.newInstance();
            
           Logger.info( "node created with pid: " + String.format( "%x", 
                   node.pid() ), (Loggable)node );

            //Ring look-up
           ringer = (RingerService)Naming.lookup( "Ringer" );

            //Register node to ring
           ringer.registerNode( node );
            
        } catch( NotBoundException nbe ) {
            Logger.error( "A remote object was not bound: " + nbe, 
                    (Loggable)node );
        } catch( UnknownHostException uhe ) {
            Logger.error( "Host not recognized: " + uhe, (Loggable)node );
        } catch( RemoteException re ) {
            Logger.error( "Error starting service: " + re.getMessage(), 
                    (Loggable)node );
        } catch( MalformedURLException mal ) {
            Logger.error( "URL is possibly malformed: " + mal, (Loggable)node );
        }
        
    }
    
}
