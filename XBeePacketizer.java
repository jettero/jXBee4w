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

            if( s[i].length == 1 ) p[i] = XBeePacket.at(seqno(), s[i][0]);
            else                   p[i] = XBeePacket.at(seqno(), s[i][0], s[i][1]);
        }

        return p;
    }

    public Queue <XBeePacket> tx(Address64 dst, String msg) {
        return tx(dst, msg.getBytes());
    }

    public Queue <XBeePacket> tx(Address64 dst, byte input[]) {
        byte blocks[][] = Message.fragmentMessage(input, 100);
        Queue<XBeePacket> q = new ArrayDeque<XBeePacket>();


        for(int i=0; i<blocks.length; i++) {
            try {
                q.add( XBeePacket.tx(seqno(), dst, blocks[i]) );
            }

            catch(PayloadException e) {
                // NOTE: This really shouldn't happen at all.  If it does, it'll happen a lot.
                System.err.println("internal error building packet: " + e.getMessage() );
            }
        }

        return q;
    }
}
