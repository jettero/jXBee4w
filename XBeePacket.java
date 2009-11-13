
public class XBeePacket {
    byte packet[];

    // there is no default packet type, so no constructor

    public String serialize() {
        return new String(packet);
    }

    public void set_tx(byte seqno, Address64 a, String _payload) throws PayloadException {
        byte payload[] = _payload.getBytes();

        if( payload.length > 100 )
            throw new PayloadException("asked to packetize " + payload.length + " bytes, but payloads are restricted to 100 bytes");

        packet = new byte[ payload.length + 9 ];

        // the payload bytes:
        for(int i=0; i<payload.length; i++) {
            packet[i+8] = payload[i];

            /* if( payload[i] == 0x7e )
            /*     throw new PayloadException("invalid character at byte-" + i + " of payload 0x7e characters may not appear in a packet payload");
            */
        }

        // frame header:
        packet[0] = 0x7e;
        packet[1] = (byte) ((0xff00 & packet.length) >> 8);
        packet[2] = (byte) (0xff & packet.length);
        packet[3] = 0x00; // Tx packet
        packet[4] = seqno;
        packet[5] = a.MSB();
        packet[6] = a.LSB();
        packet[7] = 0x0; // 0x01 is disable ACK and 0x04 is use broadcast, neither interest us
    }

    public static XBeePacket tx(char seqno, String payload) throws PayloadException {
        XBeePacket p = new XBeePacket();
        p.set_tx(seqno, payload);
        return p;
    }
}

