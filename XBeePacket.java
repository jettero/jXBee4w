
public class XBeePacket {
    public static final int FRAME_DELIMITER_LEN  = 1;
    public static final int FRAME_LENGTH_LEN     = 2;
    public static final int FRAME_CHECKSUM_LEN   = 1;
    public static final int FRAME_HEADER_LEN     = FRAME_DELIMITER_LEN + FRAME_LENGTH_LEN + FRAME_CHECKSUM_LEN;

    public static final int API_MESSAGE_TYPE_LEN = 1;

    public static final int TX64_PAYLOAD_LIMIT   = 100;
    public static final int TX64_SEQNO_LEN       = 1;
    public static final int TX64_DST_ADDR_LEN    = 5;
    public static final int TX64_OPTIONS_LEN     = 1;
    public static final int TX64_HEADER_LEN      = API_MESSAGE_TYPE_LEN + TX64_SEQNO_LEN + TX64_DST_ADDR_LEN + TX64_OPTIONS_LEN;

    byte packet[];

    // there is no default packet type, so no constructor

    public byte[] getBytes() {
        return packet;
    }

    // shortcut for the below set_tx() function + new
    public static XBeePacket tx(byte seqno, Address64 dst, String payload) throws PayloadException {
        XBeePacket p = new XBeePacket();
        p.set_tx(seqno, dst, payload);
        return p;
    }

    public void set_tx(byte seqno, Address64 dst, String _payload) throws PayloadException {
        byte payload[] = _payload.getBytes();
        byte _dstb[]   = dst.getBytes();

        if( payload.length > TX64_PAYLOAD_LIMIT )
            throw new PayloadException("asked to packetize " + payload.length + " bytes, but payloads are restricted to 100 bytes");

        packet = new byte[ payload.length + FRAME_HEADER_LEN + TX64_HEADER_LEN ];

        // frame header:
        packet[0]  = 0x7e;
        packet[1]  = (byte) ((0xff00 & packet.length) >> 8);
        packet[2]  = (byte) (0xff & packet.length);
        packet[3]  = 0x00; // Tx packet
        packet[4]  = seqno;
        packet[13] = 0x0; // 0x01 is disable ACK and 0x04 is use broadcast, neither interest us

        // destination address
        for(int i=0; i<8; i++)
            packet[5+i] = _dstb[0+i]; // 5-12

        // the payload bytes:
        for(int i=0; i<payload.length; i++)
            packet[i+14] = payload[i]; // 14-n

        this.calculate_checksum();
    }

    private void calculate_checksum() {
        int sum = 0;
        for(int i=0; i<packet.length; i++) {
            sum += packet[i];
            System.out.println(" sum += " + packet[i] + " = " + sum);
        }
    }
}

