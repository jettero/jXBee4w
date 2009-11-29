
public class XBeeRxPacket extends XBeePacket {
    public static final int RX64_RSSI_LEN = 1;

    public static final int RX64_PAYLOAD_LIMIT = 100;
    public static final int RX64_SRCADDR_LEN   = 8;
    public static final int RX64_OPTIONS_LEN   = 1;
    public static final int RX64_HEADER_LEN    = API_MESSAGE_TYPE_LEN + RX64_SRCADDR_LEN + RX64_RSSI_LEN + RX64_OPTIONS_LEN;

    public static final int RX64_PAYLOAD_START = FRAME_HEADER_LEN + RX64_HEADER_LEN;
    public static final int RX64_SRCADDR_START = FRAME_HEADER_LEN + API_MESSAGE_TYPE_LEN;

    XBeeRxPacket(byte b[]) { super(b); }

    public Address64 getSourceAddress() {
        byte _ab[] = new byte[ RX64_SRCADDR_LEN ];

        for(int i=0; i<_ab.length; i++)
            _ab[i] = packet[ RX64_SRCADDR_START+i ];

        Address64 a = new Address64(_ab);

        return a;
    }

    public int payloadLength() {
        if( !conditionalCheckPacket() )
            return -1;

        int pktlen = frameLength();
            pktlen -= RX64_HEADER_LEN;

        return pktlen;
    }

    public byte[] getPayloadBytes() {
        int len = payloadLength();
        byte ret[] = new byte[ len ];

        for(int i=0; i<len; i++)
            ret[i] = packet[ i + RX64_PAYLOAD_START ];

        return ret;
    }
}
