
public class XBeeATPacket extends XBeePacket {
    public static final int AT_PAYLOAD_LIMIT = 100; // no idea what the limit is, if any, but 100 is enough for sure
    public static final int AT_HEADER_LEN    = API_MESSAGE_TYPE_LEN + FRAME_ID_LEN;

    XBeeATPacket() {}
    XBeeATPacket(byte b[]) { super(b); }

    public String cmd() { return new String(this.cmdBytes()); }
    public byte[] cmdBytes() {
        if( !conditionalCheckPacket() )
            return new byte[0];

        byte b[] = new byte[2];

        b[0] = packet[5];
        b[1] = packet[6];

        return b;
    }
}
