import java.util.*;

public class XBeePacketizer {
    char seqno = 1;

    void set_seqno(int i) {
        char awesome = (char)i;
        seqno = awesome;
    }

    public byte seqno() {
        char awesome = seqno;

        seqno = (char) (awesome + 1);
        if( seqno >= 256 )
            seqno = 1;

        return (byte) awesome;
    }

    public List build_tx(Address64 dst, String msg) {
        int packets        = (int)Math.ceil(msg.length()/100.0);
        List<XBeePacket> p = new ArrayList<XBeePacket>();

        int hard_ending = msg.length();

        for(int i=0; i<packets; i++) {
            int beginning = i*100;
            int ending    = (i+1)*100;

            if( ending > hard_ending )
                ending = hard_ending;

            try {
                p.add( XBeePacket.tx(this.seqno(), dst, msg.substring(beginning, ending)) );
            }

            catch(PayloadException e) {
                System.err.println("problem building packet: " + e.getMessage() );
            }
        }

        return p;
    }
}
