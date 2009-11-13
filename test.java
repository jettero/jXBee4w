//import XBeeAPIPacket;

public class test {
    public static void main(String[] args) {
        XBeeAPIPacketizer p = new XBeeAPIPacketizer();

        p.set_seqno(65533);

        System.out.println("seqno: " + (int)p.seqno());
        System.out.println("seqno: " + (int)p.seqno());
        System.out.println("seqno: " + (int)p.seqno());
        System.out.println("seqno: " + (int)p.seqno());
        System.out.println("seqno: " + (int)p.seqno());
    }
}
