
public class XBeePacket {
    byte packet[];

    // there is no default packet type, so no constructor

    public String serialize() {
        return new String(packet);
    }

    public void set_tx(char seqno, String _payload) throws PayloadException {
        byte payload[] = _payload.getBytes();

        if( payload.length > 100 )
            throw new PayloadException("asked to packetize " + payload.length + " bytes, but payloads are restricted to 100 bytes");

        packet = new byte[ payload.length + 8 ];

        // the payload bytes:
        for(int i=0; i<payload.length; i++) {
            packet[i+7] = payload[i];

            if( payload[i] == 0x7e )
                throw new PayloadException("invalid character at byte-" + i + " of payload 0x7e characters may not appear in a packet payload");
        }

        // frame header:
        packet[0] = 0x7e;
        packet[1] = (byte) ((0xff00 & packet.length) >> 8);
        packet[2] = (byte) (0xff & packet.length);
    }

    public static XBeePacket tx(char seqno, String payload) throws PayloadException {
        XBeePacket p = new XBeePacket();
        p.set_tx(seqno, payload);
        return p;
    }
}

