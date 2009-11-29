
public class XBeeRadio64Packet extends XBeePacket {
    public static final int R64_PAYLOAD_LIMIT = 100;
    public static final int R64_ADDR_LEN      = 8;
    public static final int R64_OPTIONS_LEN   = 1;
    public static final int R64_HEADER_LEN    = API_MESSAGE_TYPE_LEN + FRAME_ID_LEN + R64_ADDR_LEN + R64_OPTIONS_LEN;

    public static final int R64_PAYLOAD_START = FRAME_DELIMITER_LEN + FRAME_LENGTH_LEN + R64_HEADER_LEN;
    public static final int R64_ADDRESS_START = FRAME_DELIMITER_LEN + FRAME_LENGTH_LEN + API_MESSAGE_TYPE_LEN + FRAME_ID_LEN;

    XBeeRadio64Packet() {};
    XBeeRadio64Packet(byte b[]) { super(b); }

    public int payloadLength() {
        if( !conditionalCheckPacket() )
            return -1;

        int pktlen = frameLength();
            pktlen -= R64_HEADER_LEN;

        return pktlen;
    }

    public byte[] getPayloadBytes() {
        int len = payloadLength();
        byte ret[] = new byte[ len ];

        for(int i=0; i<len; i++)
            ret[i] = packet[ i + R64_PAYLOAD_START ];

        return ret;
    }
}
