
public class XBeeRxPacket extends XBeePacket {
    public static final int RX64_PAYLOAD_LIMIT = 100;
    public static final int RX64_DST_ADDR_LEN  = 8;
    public static final int RX64_OPTIONS_LEN   = 1;
    public static final int RX64_HEADER_LEN    = API_MESSAGE_TYPE_LEN + FRAME_ID_LEN + TX64_DST_ADDR_LEN + TX64_OPTIONS_LEN;

    public int payloadLength() {
        if( !conditionalCheckPacket() )
            return -1;

        int pktlen = (packet[1] << 8) + packet[2];
            pktlen -= TX64_HEADER_LEN;

        return pktlen;
    }
}
