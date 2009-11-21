import java.io.*;
import java.nio.*;

public class XBeePacket {
    public static final byte FRAME_DELIMITER = (byte) 0x7e;
    public static final byte AMT_TX          = (byte) 0x00;
    public static final byte AMT_AT_COMMAND  = (byte) 0x08;
    public static final byte AMT_AT_RESPONSE = (byte) 0x88;

    public static final int FRAME_DELIMITER_LEN  = 1;
    public static final int FRAME_LENGTH_LEN     = 2;
    public static final int FRAME_CHECKSUM_LEN   = 1;
    public static final int FRAME_HEADER_LEN     = FRAME_DELIMITER_LEN + FRAME_LENGTH_LEN + FRAME_CHECKSUM_LEN;

    public static final int API_MESSAGE_TYPE_LEN = 1;
    public static final int FRAME_ID_LEN         = 1;

    public static final int TX64_PAYLOAD_LIMIT   = 100;
    public static final int TX64_DST_ADDR_LEN    = 8;
    public static final int TX64_OPTIONS_LEN     = 1;
    public static final int TX64_HEADER_LEN      = API_MESSAGE_TYPE_LEN + FRAME_ID_LEN + TX64_DST_ADDR_LEN + TX64_OPTIONS_LEN;

    public static final int AT_PAYLOAD_LIMIT = 100; // no idea what the limit is, if any, but 100 is enough for sure
    public static final int AT_HEADER_LEN    = API_MESSAGE_TYPE_LEN + FRAME_ID_LEN;

    byte packet[];
    boolean checked;
    boolean ok;

    /////////////////////////////////////////////////////////////////////////////////
    // consturcotors and factories
    //

    XBeePacket() {} // there's no way to know what kind of packet = new byte[????], so it has to happen in the
                    // specific packet type builder

    XBeePacket(byte b[]) { packet = b; }

    // Tx packet factory
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
        packet[0]  = FRAME_DELIMITER;
        packet[1]  = (byte) ((0xff00 & content_length) >> 8);
        packet[2]  = (byte)  (0x00ff & content_length);
        packet[3]  = AMT_TX;
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
        packet[0]  = FRAME_DELIMITER;
        packet[1]  = (byte) ((0xff00 & content_length) >> 8);
        packet[2]  = (byte)  (0x00ff & content_length);
        packet[3]  = AMT_AT_COMMAND;
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

    public byte[] getBytes() {
        return packet;
    }

    public boolean checkChecksum() {
        int sum = 0;

        for(int i=3; i < packet.length; i++)
            sum += packet[i];

        if( (sum & 0xff) == 0xff )
            return true; // oh, goodie don't fall through to the pessimistic assumption

        System.err.printf("ERROR: packet checksum error (worked out to %02x)", sum);
        return false; // :( let's be pessimistic
    }

    public boolean checkFrame() {
        if( packet == null ) {
            System.err.println("ERROR: invalid packet, no contents");
            return (ok = false);
        }

        if( packet.length < FRAME_HEADER_LEN ) {
            System.err.println("ERROR: invalid packet, fewer bytes than a frame header");
            return (ok = false);
        }
        return true;
    }

    public boolean checkFrameLen() {
        int pktlen = (packet[1] << 8) + packet[2];

        if( pktlen+FRAME_HEADER_LEN != packet.length ) {
            System.err.printf("ERROR: invalid packet, packet length differs from what is specified in frame header (%d+%d vs %d)%n", 
                pktlen, FRAME_HEADER_LEN, packet.length);
            return (ok = false);
        }
        return true;
    }

    public boolean checkPacket() {
        checked = true;

        if( !checkFrame() )
            return (ok = false);

        if( checkFrameLen() && checkChecksum() )
            return (ok = true);

        return (ok = false);
    }

    public boolean conditionalCheckPacket() {
        if( checked )
            return ok;

        return checkPacket();
    }

    /////////////////////////////////////////////////////////////////////////////////
    // accessor helpers
    //

    public int length() {
        if( !conditionalCheckPacket() )
            return -1;

        return packet.length;
    }

    public byte type() {
        if( !conditionalCheckPacket() )
            return -1;

        return packet[3];
    }

    public int seqno() {
        if( !conditionalCheckPacket() )
            return -1;

        return packet[4] & 0xff;
    }

    public int frameID() { return seqno(); } // technically more accurately named

    public int payloadLength() {
        if( !conditionalCheckPacket() )
            return -1;

        int pktlen = (packet[1] << 8) + packet[2];

        switch(type()) {
            case AMT_TX:         pktlen -= TX64_HEADER_LEN; break;
            case AMT_AT_COMMAND: pktlen -= AT_HEADER_LEN;   break;

            default:
                System.err.println("warning: making educated guess on payload length");
                pktlen -= API_MESSAGE_TYPE_LEN;
                pktlen -= FRAME_ID_LEN;
        }

        return pktlen;
    }

    /////////////////////////////////////////////////////////////////////////////////
    // byte helpers
    //
    public static boolean enoughForPacket(ByteBuffer b) {
        int buflen = b.position();

        if( buflen > 3 ) {
            int pktlen = b.get(1) << 8;
                pktlen += b.get(2);
                pktlen += FRAME_HEADER_LEN;

            // in a static method, we can't really check some global flag, so make this commentable
            // System.out.println("[debug] enoughForPacket(b)? pktlen+FHL: " + pktlen + "; bufferlen: " + buflen);

            if( pktlen <= buflen )
                return true;
        }

        return false;
    }


    /////////////////////////////////////////////////////////////////////////////////
    // debugging helpers
    //
    public static void bytesToFile(String fname, byte b[]) {
        // this whole function is for debugging purposes, no sense checking some global flag
        System.out.println("[debug] dumping packet bytes to " + fname);

        try {
            FileOutputStream out = new FileOutputStream(fname);

            out.write(b);
            out.close();
        }

        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void fileDump(String fname) {
        if( packet == null || packet.length < 5) {
            System.err.println("nonsense packet, refusing to dump");
            return;
        }

        String mod = String.format(fname, (int) this.getBytes()[4]);
        bytesToFile(mod, packet);
    }

}

