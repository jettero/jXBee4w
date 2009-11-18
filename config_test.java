
public class config_test {
    public static void main(String[] args) {
        String port = "COM1";

        if( args.length > 0 )
            port = args[0];

        try {
            XBeeConfig c = new XBeeConfig(port, 115200);

            c.debug = true;

            String conf[] = { "ATRE", "ATBD7", "ATAP1" };
            String res[]  = c.config(conf);

            for(int i=0; i<conf.length; i++)
                System.out.println(conf[i] + " result: " + res[i]);

            System.exit(0); // why do we have to explicitly exit?  I suspect RXTX has some internal threads
        }

        catch ( Exception e ) {
            System.err.println("ERROR configuring modem: " + e.getMessage());
        }
    }
}
