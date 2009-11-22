
public class XBeeATCommandPacket extends XBeeATPacket {

    XBeeATCommandPacket(byte seqno, byte []cmd, byte []param) throws PayloadException {
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

}
