import java.io.*;

public class fragment_test {
    public static void main(String s[]) {
        String longString = "Hello mang, this is my string.";

        for(int dupy=0; dupy<1000; dupy++)
            longString += " Hello mang, this is my string.";

        for(int maxSize=3; maxSize<200; maxSize++) {
            byte b[][] = Message.fragmentMessage(longString.getBytes(), maxSize);

            System.out.printf("fragmented %d bytes into %3d peices (maxSize=%3d)",
                longString.length(), b.length, maxSize);

            boolean ok = false;
            Message m = new Message();
            for(int i=0; i<b.length; i++)
                try {
                    m.addBlock(0, b[i]);

                } catch(IOException e) {
                    System.err.println("fatal error adding block: " + e.getMessage());
                    System.exit(1);
                }

            try { 
                if( m.wholeMessage() )
                    if( longString.equals(new String(m.reconstructMessage())) )
                        ok = true;
            }

            catch(IOException e) {
                System.err.println("fatal error rebuilding message: " + e.getMessage());
                System.exit(1);
            }


            System.out.println( ok ? " [ OK ]"
                                   : " [    ]" );

            String fdm = System.getenv("FRAGDUMP_MAXSIZE");
            if( fdm == null )
                fdm = "0";

            if( (new Integer(fdm)).intValue() == maxSize ) {
                for(int i=0; i<b.length; i++)
                    XBeePacket.bytesToFile( String.format("mfrag-%03x.dat", Message.blockOffset(b[i])), b[i] );

                try {
                    XBeePacket.bytesToFile( "message.dat", longString.getBytes() );
                    XBeePacket.bytesToFile( "egassem.dat", m.reconstructMessage() );

                } catch (IOException e) { /* who cares */ }

                System.exit(0);
            }
        }
    }
}
