
public class XBeePacket {
    public static final int FRAME_DELIMITER_LEN  = 1;
    public static final int FRAME_LENGTH_LEN     = 2;
    public static final int FRAME_CHECKSUM_LEN   = 1;
    public static final int FRAME_HEADER_LEN     = FRAME_DELIMITER_LEN + FRAME_LENGTH_LEN + FRAME_CHECKSUM_LEN;

    public static final int API_MESSAGE_TYPE_LEN = 1;

    public static final int TX64_PAYLOAD_LIMIT   = 100;
    public static final int TX64_SEQNO_LEN       = 1;
    public static final int TX64_DST_ADDR_LEN    = 8;
    public static final int TX64_OPTIONS_LEN     = 1;
    public static final int TX64_HEADER_LEN      = API_MESSAGE_TYPE_LEN + TX64_SEQNO_LEN + TX64_DST_ADDR_LEN + TX64_OPTIONS_LEN;

    byte packet[];

    public byte[] getBytes() {
        return packet;
    }

    XBeePacket() {} // there's no way to know what kind of packet = new byte[????], so it has to happen in the 
                    // specific packet type builder

    XBeePacket(byte b[]) throws FrameException {
        // maybe sanity check the frame a little later on for now, just
        // minimally look for things that make this packet a packet
        if( b.length < FRAME_HEADER_LEN )
            throw new FrameException("This packet doesn't seem long enough to parse the frame.");

        if( b[0] != 0x7e )
            throw new FrameException("This packet doesn't seem parseable, frame delimiter not found in the 0th position");

        int length = b[2];
            length += (b[1] << 8);

        int packet_length = b.length-FRAME_HEADER_LEN;

        if( length != packet_length )
            throw new FrameException("The packet length (" + packet_length
                + ") does not equal the stated length in the packet header (" + length + ")");

        packet = b;
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

        int content_length = payload.length + TX64_HEADER_LEN;
        packet = new byte[ FRAME_HEADER_LEN + content_length ];

        // frame header:
        packet[0]  = 0x7e;
        packet[1]  = (byte) ((0xff00 & content_length) >> 8);
        packet[2]  = (byte) (0xff & content_length);
        packet[3]  = 0x00; // Tx packet
        packet[4]  = seqno;
        packet[13] = 0x0; // 0x01 is disable ACK and 0x04 is use broadcast, neither interest us

        // destination address
        for(int i=0; i<8; i++)
            packet[5+i] = _dstb[0+i]; // 5-12

        // the payload bytes:
        for(int i=0; i<payload.length; i++)
            packet[i+14] = payload[i]; // 14-n

        packet[packet.length-1] = this.calculate_checksum();
    }

    private byte calculate_checksum() {
        int sum = 0;

        for(int i=3; i < packet.length-1; i++)
            sum += (packet[i] & 0xff);

        sum = 0xff - (sum & 0xff); // subtract that last byte from 0xff

        return (byte) (0xff - sum);
    }

    private boolean check_checksum() {
        int sum = 0;

        for(int i=3; i < packet.length; i++)
            sum += (packet[i] & 0xff);

        if( sum == 0xff )
            return true; // oh, goodie don't fall through to the pessimistic assumption

        return false; // :( let's be pessimistic
    }
}

