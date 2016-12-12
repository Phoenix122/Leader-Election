
package hs;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.security.AccessControlException;

/**
 *
 * @author Surya Selvaraj
 */
public class Report implements Loggable, Serializable {
    
    protected String author; //Author name
    
    private int sentMessageCount = 0; //Tracking sent messages
    private int receiveMessageCount = 0; //Tracking received messages
    
    //Constructor
    public Report( String a ) {
        author = a;
    }
    //Incrementing sent msgs count on encounter
    public void msg() { sentMessageCount++; }
    //Incrementing received msgs count on encounter
    public void rcv() { receiveMessageCount++; }
    //Getting author
    public String author() { return author; }
    //Getting sent msgs count
    public int sentMessageCount() { return sentMessageCount; }
    //Getting received msgs count
    public int receiveMessageCount() { return receiveMessageCount; }

    //Getting summary of author, total #msgs sent and received
    public String summary() { 
        return String.format(
          "Report for %s - Total Messages Sent: %d | " +
          "Total Received Messages: %d\n",
          author,
          sentMessageCount,
          receiveMessageCount
        );
    }

    //Getting the raw data of author, sent count and received count
    public String raw() {
        return String.format( "%s %d %d\n", author, sentMessageCount, receiveMessageCount );
    }
    //Implementing Loggable - override logIdent() - Log Identification
    @Override
    public String logIdent() {
        return String.format( "%s Reporter", author );
    }

    //export() for exporting a file to write concurrently
    public void export( String filename ) {
        
        if( filename == null ) filename = author; //default file name
        
        try { //writing on to file
            FileWriter fstream = new FileWriter( filename, true );
            BufferedWriter out = new BufferedWriter( fstream );
            
            out.write( raw() );
            
            out.close();
        } catch( IOException ioe ) {
            Logger.error( "There was an error writing to the file: " + ioe, this );
        } catch( AccessControlException ace ) {
						Logger.error( "There was a problem with writing to the file. You probably have a " +
							"bad java security policy file. Check it and make sure that writing to this " + 
							"directory is permitted: " + ace, this );
				}
        
    }
   
    
    
    
}
