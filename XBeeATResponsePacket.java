
public class XBeeATResponsePacket extends XBeeATPacket {
    public static final int AT_RESPONSE_STATUS_BYTE_LEN = 1;
    public static final int AT_RESPONSE_CMD_LEN = 2;
    public static final int AT_HEADER_LEN = API_MESSAGE_TYPE_LEN + FRAME_ID_LEN
        + AT_RESPONSE_STATUS_BYTE_LEN + AT_RESPONSE_CMD_LEN;

    public static final int  AT_RESPONSE_STATUS_BYTE = 7;
    public static final int  AT_RESPONSE_START_BYTE = 8;

    public static final byte AT_RESPONSE_STATUS_OK  = 0x0;
    public static final byte AT_RESPONSE_STATUS_ERR = 0x1;
    public static final byte AT_RESPONSE_STATUS_INVALID_CMD = 0x2;
    public static final byte AT_RESPONSE_STATUS_INVALID_PARAM = 0x2;

    XBeeATResponsePacket(byte b[]) { super(b); }

    public boolean statusOK()             { return packet[AT_RESPONSE_STATUS_BYTE] == AT_RESPONSE_STATUS_OK; }
    public boolean statusError()          { return packet[AT_RESPONSE_STATUS_BYTE] == AT_RESPONSE_STATUS_ERR; }
    public boolean statusInvalidCommand() { return packet[AT_RESPONSE_STATUS_BYTE] == AT_RESPONSE_STATUS_INVALID_CMD; }

    public String response() { return new String(this.responseBytes()); }
    public int responseLength() {
        return frameLength() - AT_HEADER_LEN;
    }
    public byte[] responseBytes() {
        int length = responseLength();

        if( length < 1 )
            return new byte[0];

        byte b[] = new byte[length];
        for(int i=0; i<length; i++)
            b[i] = packet[AT_RESPONSE_START_BYTE+i];

        return b;
    }
}
