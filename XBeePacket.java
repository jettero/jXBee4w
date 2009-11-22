import java.io.*;
import java.nio.*;

public class XBeePacket {
    public static final byte FRAME_DELIMITER = (byte) 0x7e;
    public static final byte AMT_TX64        = (byte) 0x00;
    public static final byte AMT_TX64_STATUS = (byte) 0x89;
    public static final byte AMT_RX64        = (byte) 0x80;
    public static final byte AMT_AT_COMMAND  = (byte) 0x08;
    public static final byte AMT_AT_RESPONSE = (byte) 0x88;

    public static final int FRAME_DELIMITER_LEN  = 1;
    public static final int FRAME_LENGTH_LEN     = 2;
    public static final int FRAME_CHECKSUM_LEN   = 1;
    public static final int FRAME_HEADER_LEN     = FRAME_DELIMITER_LEN + FRAME_LENGTH_LEN + FRAME_CHECKSUM_LEN;

    public static final int API_MESSAGE_TYPE_LEN = 1;
    public static final int FRAME_ID_LEN         = 1;

    public byte packet[];
    private boolean checked;
    private boolean ok;

    /////////////////////////////////////////////////////////////////////////////////
    // consturcotors and factories
    //

    XBeePacket() {} // there's no way to know what kind of packet = new byte[????], so it has to happen in the
                    // specific packet type builder

    XBeePacket(byte b[]) { packet = b; }

    public XBeePacket adapt() {
        if( !conditionalCheckPacket() )
            return this;

        switch(type()) {
            case AMT_RX64:        return new XBeeRxPacket(packet);
            case AMT_AT_RESPONSE: return new XBeeATResponsePacket(packet);

            /* TODO:
            case AMT_TX64:        return new XBeeTxPacket(packet);
            case AMT_TX64_STATUS: return new XBeeTxStatusPacket(packet);
            case AMT_AT_COMMAND:  return new XBeeATCommandPacket(packet);
            */
        }

        return this;
    }

    // Tx packet factory
    public static XBeeTxPacket tx(byte seqno, Address64 dst, String payload) throws PayloadException {
        return new XBeeTxPacket(seqno, dst, payload.getBytes());
    }

    // AT command factories
    public static XBeeATCommandPacket at(byte seqno, String cmd) throws PayloadException {
        return at(seqno, cmd, "");
    }

    public static XBeeATCommandPacket at(byte seqno, String cmd, String param) throws PayloadException {
        return new XBeeATCommandPacket(seqno, cmd.getBytes(), param.getBytes());
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

    public boolean checkFrameLength() {
        int pktlen = frameLength();

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

        if( checkFrameLength() && checkChecksum() )
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

    public int frameLength() {
        // NOTE: checking conditionalCheckPacket would be a fail because it uses this method
        if( packet == null )    return -1;
        if( packet.length < 3 ) return -1;

        return (packet[1] << 8) + packet[2];
    }

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

