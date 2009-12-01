
public class fragment_test {
    public static void main(String s[]) {
        String longString = "Hello mang, this is my string.";
        System.out.printf("--fragmenting %d bytes:%n", longString.length());

        byte b[][] = Message.fragmentMessage(longString, 8);

        for(int i=0; i<b.length; i++)
            XBeePacket.bytesToFile( String.format("fragment-%03x.dat", Message.blockOffset(b[i])), b[i] );
    }
}
