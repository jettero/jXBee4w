
public class config_test {
    public static void main(String[] args) {
        try {
            XBeeConfig c = (new XBeeConfig()).connect(args[0], 115200);

            c.config("", "", "");
        }

        catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}
