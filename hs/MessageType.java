
package hs;

/**
 *
 * @author Surya Selvaraj
 */
//Defining types of msgs
public enum MessageType {
    ELECTION,
    REPLY,
    ANNOUNCEMENT;
    //Overriding toString() to return the type of message accordingly
    @Override
    public String toString() {
        String ret = super.toString();
        switch( this ) {
            case ELECTION:
                ret = "Election";
                break;
            case REPLY:
                ret = "Reply";
                break;
            case ANNOUNCEMENT:
                ret = "Announcement";
                break;
        }
        return ret; 
    }
}
