
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

    public XBeePacket[] build_tx(String bytes) {
        XBeePacket p[] = new XBeePacket[1];

        /*
        for(int i=0, int j=1; i*100<bytes.length(); i++, j++)
            System.out.println("i: " + i + "; j: " + j);
            */

        return p;
    }
}
