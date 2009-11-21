import java.io.*;
import java.nio.*;

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

    public static final int AT_PAYLOAD_LIMIT   = 100; // no idea what the limit is, if any, but 100 is enough for sure
    public static final int AT_SEQNO_LEN       = 1;
    public static final int AT_HEADER_LEN      = API_MESSAGE_TYPE_LEN + AT_SEQNO_LEN;

    byte packet[];

    /////////////////////////////////////////////////////////////////////////////////
    // consturcotors and factories
    //

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

    // Tx packet facotry
    public static XBeePacket tx(byte seqno, Address64 dst, String payload) throws PayloadException {
        XBeePacket p = new XBeePacket();
        p.set_tx(seqno, dst, payload);
        return p;
    }

    // Tx packet setup from string
    public void set_tx(byte seqno, Address64 dst, String _payload) throws PayloadException {
        this.set_tx(seqno, dst, _payload.getBytes());
    }

    // Tx Packet setup from bytes
    public void set_tx(byte seqno, Address64 dst, byte []payload) throws PayloadException {
        byte _dstb[] = dst.getBytes();

        if( payload.length > TX64_PAYLOAD_LIMIT )
            throw new PayloadException("asked to packetize " + payload.length + " bytes, but payloads are restricted to " + TX64_PAYLOAD_LIMIT + " bytes");

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

        packet[packet.length-1] = this.calculateChecksum();
    }

    // AT command factories
    public static XBeePacket at(byte seqno, String cmd) throws PayloadException {
        XBeePacket p = new XBeePacket();
        p.setAT(seqno, cmd.getBytes(), "".getBytes());
        return p;
    }

    public static XBeePacket at(byte seqno, String cmd, String param) throws PayloadException {
        XBeePacket p = new XBeePacket();
        p.setAT(seqno, cmd.getBytes(), param.getBytes());
        return p;
    }

    // AT command setup
    public void setAT(byte seqno, byte []cmd, byte []param) throws PayloadException {
        if( cmd.length != 2 )
            throw new PayloadException("asked to send XBee " + cmd.length + " byte command, but AT commands are two bytes");

        if( param.length > AT_PAYLOAD_LIMIT )
            throw new PayloadException("asked to packetize " + param.length + " param bytes, but param bytes are restricted to " + AT_PAYLOAD_LIMIT + " bytes");

        int content_length = cmd.length + param.length + AT_HEADER_LEN;
        packet = new byte[ FRAME_HEADER_LEN + content_length ];

        // frame header:
        packet[0]  = 0x7e;
        packet[1]  = (byte) ((0xff00 & content_length) >> 8);
        packet[2]  = (byte) (0xff & content_length);
        packet[3]  = 0x08; // AT cmd packet
        packet[4]  = seqno;
        packet[5]  = cmd[0];
        packet[6]  = cmd[1];

        for(int i=0; i<param.length; i++)
            packet[7+i] = param[i];

        packet[packet.length-1]  = this.calculateChecksum();
    }

    /////////////////////////////////////////////////////////////////////////////////
    // packet helpers
    //
    public byte calculateChecksum() {
        int sum = 0;

        for(int i=3; i < packet.length-1; i++)
            sum += (packet[i] & 0xff);

        return (byte) (0xff - (sum & 0xff));
    }

    public boolean checkChecksum() {
        int sum = 0;

        for(int i=3; i < packet.length; i++)
            sum += (packet[i] & 0xff);

        if( sum == 0xff )
            return true; // oh, goodie don't fall through to the pessimistic assumption

        return false; // :( let's be pessimistic
    }

    public byte[] getBytes() {
        return packet;
    }

    /////////////////////////////////////////////////////////////////////////////////
    // byte helpers
    //
    public static boolean enoughForPacket(ByteBuffer b) {
        int buflen = b.position();

        if( buflen > 3 ) {
            int pktlen = b.get(1) << 8;
                pktlen += b.get(2);

            System.out.println("[debug] enoughForPacket(b)? pktlen: " + pktlen + "; bufferlen: " + buflen);

            if( (pktlen + FRAME_HEADER_LEN) <= buflen )
                return true;
        }

        return false;
    }

    public void fileDump(String fname) {
        if( packet == null )   return;
        if( packet.length < 5) return;

        String mod = String.format(fname, (int) this.getBytes()[4]);
        System.out.println("dumping packet to " + mod);

            try {
                FileOutputStream out = new FileOutputStream(mod);

                out.write(packet);
                out.close();
            }

            catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
    }

}

