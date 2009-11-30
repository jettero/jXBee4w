
public class Message {
    public static byte[][] fragmentMessage(String m, int maxSize) throws PayloadException 
        { return fragmentMessage(m.getBytes(), maxSize); }

    public static byte[][] fragmentMessage(byte input[], int maxSize) throws PayloadException {
        int msmo = maxSize - 1;
        int numberOfMessages = (int) Math.ceil( ((double) input.length) / msmo );

        if( numberOfMessages > 254 )
            throw new PayloadException("input message is longer than can be represented in offset byte");

        byte ret[][] = new byte[numberOfMessages][];

        for(int n=0; n<numberOfMessages; n++) {
            int start = n*msmo;
            int end   = (n+1)*msmo;

            if( end > input.length )
                end = input.length;

            byte b[] = new byte[end - start + 1];

            System.out.printf("TODO(%d/%d, start=%d, end=%d, len=%d)%n", n, numberOfMessages, start, end, b.length);

            for(int i=start; i<end; i++) {
                System.out.printf("\tb[%d] = input[%d]%n", i-start, i);
                b[i-start] = input[i];
            }

            if( n<numberOfMessages-1 ) b[b.length-1] = (byte) n;
            else                       b[b.length-1] = (byte) 0xff;

            ret[n] = b;
        }

        return ret;
    }
}
