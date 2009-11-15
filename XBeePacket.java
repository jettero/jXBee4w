
public class XBeePacket {
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

        if( payload.length > 100 )
            throw new PayloadException("asked to packetize " + payload.length + " bytes, but payloads are restricted to 100 bytes");

        packet = new byte[ payload.length + 9 ];

        // the payload bytes:
        for(int i=0; i<payload.length; i++)
            packet[i+8] = payload[i];

        byte _dstb[] = dst.serialize();

        // frame header:
        packet[0]  = 0x7e;
        packet[1]  = (byte) ((0xff00 & packet.length) >> 8);
        packet[2]  = (byte) (0xff & packet.length);
        packet[3]  = 0x00; // Tx packet
        packet[4]  = seqno;

        // destination address
        packet[5]  = _dstb[0];
        packet[6]  = _dstb[1];
        packet[7]  = _dstb[2];
        packet[8]  = _dstb[3];
        packet[9]  = _dstb[4];
        packet[10] = _dstb[5];
        packet[11] = _dstb[6];
        packet[12] = _dstb[7];

        packet[13] = 0x0; // 0x01 is disable ACK and 0x04 is use broadcast, neither interest us
    }
}

