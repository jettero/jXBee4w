
public class Message {
    public static int FLAGS_MASK = 0xc000; // 0b1100_0000_0000_0000
    public static int OFSET_MASK = 0x3fff; // 0b0011_1111_1111_1111

    public static int FRAGMENTED = 0x8000; // 0b1000_0000_0000_0000
    public static int MORE_FRAGS = 0x4000; // 0b0100_0000_0000_0000

    public static byte[] reconstructMessage(byte b[][]) {
        return new byte[0];
    }

    public static int blockOffset(byte block[]) {
        int fo  = (block[block.length-2] & 0xff) << 8;
            fo += (block[block.length-1] & 0xff);
            fo &= Message.OFSET_MASK;

        return fo;
    }

    public static byte[][] fragmentMessage(byte input[], int maxSize) throws PayloadException {
        int msmo = maxSize - 2;
        int numberOfMessages = (int) Math.ceil( ((double) input.length) / msmo );

        if( msmo < 1 )
            throw new PayloadException("maxSize must be at least 3");

        if( numberOfMessages > OFSET_MASK )
            throw new PayloadException("input message is longer than can be represented in byte footers");

        byte ret[][] = new byte[numberOfMessages][];

        for(int n=0; n<numberOfMessages; n++) {
            int start = n*msmo;
            int end   = (n+1)*msmo;

            if( end > input.length )
                end = input.length;

            byte b[] = new byte[end - start + 2];

            for(int i=start; i<end; i++)
                b[i-start] = input[i];

            int footer = n;

            if( numberOfMessages > 1 )   footer += FRAGMENTED;
            if( n < numberOfMessages-1 ) footer += MORE_FRAGS;

            b[b.length-2] = (byte) ( (footer & 0xff00) >> 8 );
            b[b.length-1] = (byte) ( (footer & 0xff)        );

            ret[n] = b;
        }

        return ret;
    }
}
