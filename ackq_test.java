import java.util.*;

public class ackq_test {
    public static void main(String arg[]) {
        Address64 a = new Address64();
        XBeePacketizer p = new XBeePacketizer();
        String longString = "longString0 ";
        int i;

        for(i=1; i<=100; i++)
            longString += "longString" + i + " ";

        Queue q = p.tx( a, longString );
        ACKQueue Q = new ACKQueue(q);

        XBeePacket packets[] = Q.packets();
        for(i=0; i<packets.length; i++)
            System.out.printf("wave #1: packet-%d%n", packets[i].frameID());

        Q.ACK(2);
        Q.ACK(5);
        Q.ACK(9);

        System.out.println("");

        packets = Q.packets();
        for(i=0; i<packets.length; i++)
            System.out.printf("wave #2: packet-%d%n", packets[i].frameID());
    }
}
