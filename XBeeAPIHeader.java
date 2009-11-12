

public class XBeeAPIHeader {
    static int seqno = 0;

    public static void seqno() {
        System.out.println("seqno: " + seqno++);
    }
}
