
public class XBeeTxPacket extends XBeeRadio64Packet {

    XBeeTxPacket(byte seqno, Address64 dst, byte []payload) throws PayloadException {
        byte _dstb[] = dst.getBytes();

        if( payload.length > R64_PAYLOAD_LIMIT )
            throw new PayloadException("asked to packetize " + payload.length + " bytes, but payloads are restricted to " + R64_PAYLOAD_LIMIT + " bytes");

        int content_length = payload.length + R64_HEADER_LEN;
        packet = new byte[ FRAME_HEADER_LEN + content_length ];

        // frame header:
        packet[0]  = FRAME_DELIMITER;
        packet[1]  = (byte) ((0xff00 & content_length) >> 8);
        packet[2]  = (byte)  (0x00ff & content_length);
        packet[3]  = AMT_TX64;
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

}
