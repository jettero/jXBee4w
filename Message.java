import java.util.*;

public class Message {
    public static int FLAGS_MASK = 0xc000; // 0b1100_0000_0000_0000
    public static int OFSET_MASK = 0x3fff; // 0b0011_1111_1111_1111

    public static int FRAGMENTED = 0x8000; // 0b1000_0000_0000_0000
    public static int MORE_FRAGS = 0x4000; // 0b0100_0000_0000_0000

    boolean checked, wholeMessage;
    TreeMap message;

    private static class Block {
        public byte block[];
        int offset;
        boolean fragmented, moreFrags;

        Block(byte b[]) {
            int f1 = (b[b.length-2] & 0xff) << 8;
            int f2 = (b[b.length-1] & 0xff);

            fragmented = (f1 & FRAGMENTED)>0 ? true : false;
            moreFrags  = (f1 & MORE_FRAGS)>0 ? true : false;

            offset  = f1 + f2;
            offset &= Message.OFSET_MASK;

            block = b;
        }
    }

    Message() {
        message = new TreeMap<Integer,Block>();
        checked = false;
    }

    public void addBlock(byte b[]) throws PayloadException {
        Block   B = new Block(b);
        Integer I = new Integer(b.offset);

        message.put(I,B);
        checked = false;
    }

    public boolean wholeMessage() {
        if( checked )
            return wholeMessage;

        checked = true;

        Block v[] = message.values().toArray(new Block v[message.size()]);

        if( v.length < 1 )
            return (wholeMessage=false);

        if( v[v.length-1].moreFrags )
            return (wholeMessage=false);

        if( !v[0].fragmented && v.length == 1 )
            return (wholeMessage=true);

        for(int i=0; i<v.length; i++)
            if( v.offset != i )
                return false;

        for(int i=1; i<v.length-1; i++)
            if( !v.moreFrags )
                return false;

        // TODO: this is probably pretty close, but we need some way to detect invalid messages,
        // so we can discard and start from scratch... DDO

        return (wholeMessage=true);
    }

    public static int blockOffset(byte b[]) { Block B = new Block(b); return B.offset; }

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
