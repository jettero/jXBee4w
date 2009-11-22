
public class XBeeATPacket extends XBeePacket {
    public static final int AT_CMD_B1 = 5;
    public static final int AT_CMD_B2 = 6;

    XBeeATPacket() {}
    XBeeATPacket(byte b[]) { super(b); }

    public String cmd() { return new String(this.cmdBytes()); }
    public byte[] cmdBytes() {
        if( !conditionalCheckPacket() )
            return new byte[0];

        byte b[] = new byte[2];

        b[0] = packet[AT_CMD_B1];
        b[1] = packet[AT_CMD_B2];

        return b;
    }
}
