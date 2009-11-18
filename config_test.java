
public class config_test {
    public static void main(String[] args) {
        String port = "COM1";

        if( args.length > 0 )
            port = args[0];

        try {
            XBeeConfig c = new XBeeConfig(port, 115200);

            String conf[] = { "ATDB" };
            String responses[] = c.config(conf);

            System.exit(0); // why do we have to explicitly exit?  I suspect RXTX has some internal threads
        }

        catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}
