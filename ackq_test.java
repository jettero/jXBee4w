import java.util.*;

public class ackq_test {
    public static void main(String arg[]) {
        Address64 a = new Address64();
        XBeePacketizer p = new XBeePacketizer();
        String longString = "longString0 ";

        for(int i=1; i<=100; i++)
            longString += "longString" + i + " ";

        Queue q = p.tx( a, longString );
        ACKQueue Q = new ACKQueue(q);

        System.out.println(Q.size());
    }
}
