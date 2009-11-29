
public class XBeeRxPacket extends XBeeRadio64Packet {
    XBeeRxPacket(byte b[]) { super(b); }

    public Address64 getSourceAddress() {
        byte _srcb[] = new byte[ R64_ADDR_LEN ];

        for(int i=0; i<_srcb.length; i++)
            _srcb[i] = packet[ R64_ADDRESS_START+i ];

        Address64 a = new Address64(_srcb);

        return a;
    }
}
