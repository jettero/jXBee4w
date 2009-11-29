
public interface MessageRecvEvent {
    public void recvMessage(NetworkEndpointHandle dst, Address64 src, byte message[]);
}

