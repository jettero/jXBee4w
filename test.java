//import XBeeAPIPacket;

public class test {
    public static void main(String[] args) {
        XBeePacketizer p = new XBeePacketizer();

        p.set_seqno(65533);

        System.out.println("seqno: " + (int)p.seqno());
        System.out.println("seqno: " + (int)p.seqno());
        System.out.println("seqno: " + (int)p.seqno());
        System.out.println("seqno: " + (int)p.seqno());
        System.out.println("seqno: " + (int)p.seqno());
    }
}
