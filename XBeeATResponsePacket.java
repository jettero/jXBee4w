
public class XBeeATResponsePacket extends XBeeATPacket {
    XBeeATResponsePacket(byte b[]) { super(b); }

    public String response() { return new String(this.responseBytes()); }
    public int responseLength() {
        // TODO: do things

        return -1;
    }
    public byte[] responseBytes() {
        int length = responseLength();
        if( length < 0 )
            return new byte[0];

        byte b[] = new byte[length];

        // TODO: do something

        return b;
    }
}
