
public class fragment_test {
    public static void main(String s[]) {
        String longString = "Hello mang, this is my string.";
        System.out.printf("--fragmenting %d bytes:%n", longString.length());
        byte b[][] = Message.fragmentMessage(longString, 7);

        for(int i=0; i<b.length; i++)
            XBeePacket.bytesToFile( String.format("fragment-%02x.dat", b[i][b[i].length-1]), b[i] );
    }
}
