
package hs;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author Surya Selvaraj
 */
public class ReportSet extends Report implements Loggable {

    private Set<Report> reports; //Set for reports
    
    private int totalSentCount = 0; //sent count
    private int totalReceiveCount = 0; //received count
    
    private double sentAverage = 0.0; //sent average
    private double receiveAverage = 0.0; //received average
    
    private boolean available = false; //boolean flag
    
    private final int MAX_REPORTS; //Max #reports
    
    public ReportSet( String a, int maxReports ) {
        super( a ); //Calling super class constructor
        reports = new HashSet<Report>(); //Instantiating the set reports
        MAX_REPORTS = maxReports; //Max #reports
    }

    //adding into report
    public void add( Report report ) {


        if( available && reports.size() < MAX_REPORTS )
            available = false;
        
        reports.add(report);
        
        if( reports.size() == MAX_REPORTS )
            prepare();
    }

    //Getting size of report
    public int size() {
        return reports.size();
    }

    //Preparing report
    private void prepare() {
        
        totalSentCount = 0; //Sent count
        totalReceiveCount = 0; //Receive count

        sentAverage = 0.0; //Sent average
        receiveAverage = 0.0; //Receive average

        //Calculating total sent and receive count
        for( Report r : reports ) {
            totalSentCount += r.sentMessageCount();
            totalReceiveCount += r.receiveMessageCount();
        } 

        //Calculating avg sent and receive count
        sentAverage = (double)totalSentCount / reports.size();
        receiveAverage = (double)totalSentCount / reports.size();

        //Setting available flag to true
        available = true;
    }
    //Getting available flag
    public boolean available() {
        return available;
    }

    //Getting identification of the log
    @Override
    public String logIdent()
    {
        return String.format( "%s ReportSet", author );
    }

    //Creating summary
    @Override
    public String summary() {
        if( !available )
             prepare();
        String summary = String.format( "Report for %s\n", author );
        
        summary += "===================================\n";
                
        summary += String.format( "Total Nodes: %d\n", reports.size() );
        
        for( Report r : reports ) {
            summary += r.summary();
        }
        
        summary += "===================================\n";
        
        summary += String.format( "%d total sent messages from all nodes"
                + "\n", totalSentCount );
        summary += String.format( "%d total received messages from all nodes"
                + "\n", totalReceiveCount );
        summary += String.format( "%f sent messages average\n", 
                (double)totalSentCount/reports.size() );
        summary += String.format( "%f received messages average\n",
                (double)totalReceiveCount/reports.size() );
        
        return summary;

    }
    
    @Override
    public String raw() {
        if( !available )
             prepare();
        return String.format( "%s %d %f %f\n", author, reports.size(),
                sentAverage, receiveAverage );
    } 
}
