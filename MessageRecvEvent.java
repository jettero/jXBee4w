
public interface MessageRecvEvent {
    public void recvMessage(XBeeDispatcher dst, Address64 src, byte message[]);
}

