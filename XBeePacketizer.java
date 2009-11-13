import java.util.*;

public class XBeePacketizer {
    int seqno = 0;

    void set_seqno(int i) {
        char awesome = (char)i;
        seqno = awesome;
    }

    public char seqno() {
        char awesome = (char)seqno;
        seqno = awesome + 1;

        return awesome;
    }

    public List build_tx(String msg) {
        int packets        = (int)Math.ceil(msg.length()/100.0);
        List<XBeePacket> p = new ArrayList<XBeePacket>();

        int hard_ending = msg.length();

        for(int i=0; i<packets; i++) {
            int beginning = i*100;
            int ending    = (i+1)*100;

            if( ending > hard_ending )
                ending = hard_ending;

            try {
                p.add( XBeePacket.tx(this.seqno(), msg.substring(beginning, ending)) );
            }

            catch(PayloadException e) {
                System.err.println("problem building packet: " + e.getMessage() );
            }
        }

        return p;
    }
}
