
public class fragment_test {
    public static void main(String s[]) {
        String longString = "longString0 ";
        int i;

        for(i=1; i<=100; i++)
            longString += "longString" + i + " ";

        byte b[][] = Message.fragmentMessage(longString, 100);
    }
}
