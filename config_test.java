
public class config_test {
    public static void main(String[] args) {
        try {
            XBeeConfig c = new XBeeConfig(args[0], 115200);
        }

        catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}
