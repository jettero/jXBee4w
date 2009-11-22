
public class XBeeATPacket extends XBeePacket {
    public static final int AT_PAYLOAD_LIMIT = 100; // no idea what the limit is, if any, but 100 is enough for sure
    public static final int AT_HEADER_LEN    = API_MESSAGE_TYPE_LEN + FRAME_ID_LEN;
}
