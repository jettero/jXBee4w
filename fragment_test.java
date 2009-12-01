
public class fragment_test {
    public static void main(String s[]) {
        String longString = "Hello mang, this is my string.";

        for(int maxSize=3; maxSize<200; maxSize++) {
            byte b[][] = Message.fragmentMessage(longString.getBytes(), maxSize);

            System.out.printf("fragmented %d bytes into %d peices (maxSize=%d)",
                longString.length(), b.length, maxSize);

            if( !longString.equals(Message.reconstruct(b)) System.out.println(" [ OK ]");
            else                                           System.out.println(" [    ]");

            if( false ) {
                for(int i=0; i<b.length; i++)
                    XBeePacket.bytesToFile( String.format("fragment-%03x.dat", Message.blockOffset(b[i])), b[i] );

                System.exit(0);
            }
        }
    }
}
