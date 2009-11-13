
public class XBeePacketizer {
    static int seqno = 0;

    void set_seqno(int i) {
        char awesome = (char)i;
        seqno = awesome;
    }

    public char seqno() {
        char awesome = (char)seqno;
        seqno = awesome + 1;

        return awesome;
    }

    public XBeePacket[] build_tx(String msg) {
        XBeePacket p[] = new XBeePacket[ (int)Math.ceil(msg.length()/100) ];

        return p;
    }
}
