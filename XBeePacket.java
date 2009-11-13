
public class XBeePacket {
    byte packet[];

    // there is no default packet type, so no constructor

    public String serialize() {
        return new String(packet);
    }

    public void set_tx(char seqno, String payload) {
        System.out.println("seqno: " + seqno + "; payload len: " + payload.length());

        packet = new byte[ payload.length() + 9 ];
        packet[0] = 0x7e;
    }
}

