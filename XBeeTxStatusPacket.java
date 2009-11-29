
public class XBeeTxStatusPacket extends XBeePacket {
    public static final int TXS_HEADER_LEN    = API_MESSAGE_TYPE_LEN + FRAME_ID_LEN;
    public static final int TXS_FRAME_ID_BYTE = FRAME_HEADER_LEN + API_MESSAGE_TYPE_LEN;
    public static final int TXS_STATUS_BYTE   = TXS_FRAME_ID_BYTE + FRAME_ID_LEN;

    public static final byte TXS_STATUS_SUCCESS   = 0x0;
    public static final byte TXS_STATUS_NO_ACK    = 0x1;
    public static final byte TXS_STATUS_CCA_ERROR = 0x2; // cyclic sleep ode cyclic-sleep
    public static final byte TXS_STATUS_PURGED    = 0x3; // coordinator timeout

    XBeeTxStatusPacket(byte b[]) { super(b); }

    public boolean statusOK()       { return packet[TXS_STATUS_BYTE] == TXS_STATUS_SUCCESS;   }
    public boolean statusNoACK()    { return packet[TXS_STATUS_BYTE] == TXS_STATUS_NO_ACK;    }
    public boolean statusCCAError() { return packet[TXS_STATUS_BYTE] == TXS_STATUS_CCA_ERROR; }
    public boolean statusPURGED()   { return packet[TXS_STATUS_BYTE] == TXS_STATUS_PURGED;    }
}
