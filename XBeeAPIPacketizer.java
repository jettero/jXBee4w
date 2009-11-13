

public class XBeeAPIPacketizer {
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
}
