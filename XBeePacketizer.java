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

    public XBeePacket[] at(String s[][]) throws PayloadException {
        XBeePacket p[] = new XBeePacket[ s.length ];

        for(int i=0; i<s.length; i++) {
            if( s[i].length < 1 || s[i].length > 2 )
                throw new PayloadException("commands have one or two parts: the command and the paramters (optional) -- received " + s[i].length);

            if( s[i].length == 1 ) p[i] = XBeePacket.at(this.seqno(), s[i][0]);
            else                   p[i] = XBeePacket.at(this.seqno(), s[i][0], s[i][1]);
        }

        return p;
    }

    public Queue tx(Address64 dst, String msg) {
        int packets         = (int)Math.ceil(msg.length()/100.0);
        Queue<XBeePacket> q = new ArrayDeque<XBeePacket>();

        int hard_ending = msg.length();

        for(int i=0; i<packets; i++) {
            int beginning = i*100;
            int ending    = (i+1)*100;

            if( ending > hard_ending )
                ending = hard_ending;

            try {
                q.add( XBeePacket.tx(this.seqno(), dst, msg.substring(beginning, ending)) );
            }

            catch(PayloadException e) {
                // NOTE: This really shouldn't happen at all.  If it does, it'll happen a lot.
                System.err.println("internal error building packet: " + e.getMessage() );
            }
        }

        return q;
    }
}
