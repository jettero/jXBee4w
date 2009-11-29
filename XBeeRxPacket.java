
public class XBeeRxPacket extends XBeeRadio64Packet {
    XBeeRxPacket(byte b[]) { super(b); }

    public Address64 getSourceAddress() {
        Address64 a = new Address64(new byte[0], new byte[0]); // should just fill with 0s

        return a;
    }

    public byte[] getPayloadBytes() {
        return new byte[0];
    }
}
