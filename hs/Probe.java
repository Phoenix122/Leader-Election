/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hs;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Random;

/**
 *
 * @author Surya Selvaraj
 */
public class Probe implements Serializable {
        
    public long src_pid; //Source initiating probe
    public long last_pid; //Last process in probe
    public long id;
    public int phase = 0; //Phase number
    public int hops = 0; // #Hops
    
    private Random rand = new Random();
    
    public MessageType type; //Msg type object
    public Direction direction; //Direction object

    public Probe( long pid, MessageType msgtype, int phase ) { //Constructor
        src_pid = pid;
        last_pid = pid;
        type = msgtype;
        
        this.phase = phase;
        
        rand.setSeed( Calendar.getInstance().getTimeInMillis() );
        id = rand.nextLong();
    }
    
}
