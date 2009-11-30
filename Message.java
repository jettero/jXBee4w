
public class Message {
    public static byte[][] fragmentMessage(String m, int maxSize) { return fragmentMessage(m.getBytes(), maxSize); }

    public static byte[][] fragmentMessage(byte input[], int maxSize) {
        int msmo = maxSize - 1;
        int numberOfMessages = (int) Math.ceil(input.length / msmo);

        byte ret[][] = new byte[numberOfMessages][];

        for(int i=0; i<numberOfMessages; i++) {
            int start = i*msmo;
            int end   = (i+1)*msmo-1;

            byte b[] = new byte[end - start + 1];

            System.out.printf("%d/%d - TODO(start=%d, end=%d, len=%d)%n", input.length, numberOfMessages, start, end, b.length);
        }

        return ret;
    }
}
