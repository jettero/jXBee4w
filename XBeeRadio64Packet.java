
public class XBeeRadio64Packet extends XBeePacket {
    public static final int R64_PAYLOAD_LIMIT = 100;
    public static final int R64_DST_ADDR_LEN  = 8;
    public static final int R64_OPTIONS_LEN   = 1;
    public static final int R64_HEADER_LEN    = API_MESSAGE_TYPE_LEN + FRAME_ID_LEN + R64_DST_ADDR_LEN + R64_OPTIONS_LEN;

    XBeeRadio64Packet(byte b[]) { super(b); }

    public int payloadLength() {
        if( !conditionalCheckPacket() )
            return -1;

        int pktlen = (packet[1] << 8) + packet[2];
            pktlen -= R64_HEADER_LEN;

        return pktlen;
    }
}
