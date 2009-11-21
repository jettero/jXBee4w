
public class XBeeTxPacket extends XBeePacket {
    public static final int TX64_PAYLOAD_LIMIT   = 100;
    public static final int TX64_DST_ADDR_LEN    = 8;
    public static final int TX64_OPTIONS_LEN     = 1;
    public static final int TX64_HEADER_LEN      = API_MESSAGE_TYPE_LEN + FRAME_ID_LEN + TX64_DST_ADDR_LEN + TX64_OPTIONS_LEN;

    XBeeTxPacket(byte seqno, Address64 dst, byte []payload) throws PayloadException {
        byte _dstb[] = dst.getBytes();

        if( payload.length > TX64_PAYLOAD_LIMIT )
            throw new PayloadException("asked to packetize " + payload.length + " bytes, but payloads are restricted to " + TX64_PAYLOAD_LIMIT + " bytes");

        int content_length = payload.length + TX64_HEADER_LEN;
        packet = new byte[ FRAME_HEADER_LEN + content_length ];

        // frame header:
        packet[0]  = FRAME_DELIMITER;
        packet[1]  = (byte) ((0xff00 & content_length) >> 8);
        packet[2]  = (byte)  (0x00ff & content_length);
        packet[3]  = AMT_TX;
        packet[4]  = seqno;
        packet[13] = 0x0; // 0x01 is disable ACK and 0x04 is use broadcast, neither interest us

        // destination address
        for(int i=0; i<8; i++)
            packet[5+i] = _dstb[0+i]; // 5-12

        // the payload bytes:
        for(int i=0; i<payload.length; i++)
            packet[i+14] = payload[i]; // 14-n

        packet[packet.length-1] = this.calculateChecksum();
    }

    public int payloadLength() {
        if( !conditionalCheckPacket() )
            return -1;

        int pktlen = (packet[1] << 8) + packet[2];
            pktlen -= TX64_HEADER_LEN;

        return pktlen;
    }
}
