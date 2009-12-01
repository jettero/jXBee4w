
public class fragment_test {
    public static void main(String s[]) {
        String longString = "Hello mang, this is my string.";
        System.out.printf("--fragmenting %d bytes:%n", longString.length());
        byte b[][] = Message.fragmentMessage(longString, 7);

        for(int i=0; i<b.length; i++) {
            byte c[] = b[i];

            XBeePacket.bytesToFile( String.format("fragment-%02x%02x.dat", c[c.length-2], c[c.length-1]), c);
        }
    }
}
