import java.io.*;
import java.util.*;
import java.util.regex.*;

public class config_test {
    public static final int UNKNOWN    = 0;
    public static final int CONFIGURED = 1;
    public static final int CONFIG_ERR = 2;
    public static final int PORT_ERR   = 4;

    static boolean debug = true;

    public static int config(String port, int speed) {
        int result = UNKNOWN;

        try {
            XBeeConfig c = new XBeeConfig(port, speed, true); // the last value is whether to print debugging info

            try {
                String conf[]    = { "ATRE", "ATBD7", "ATAP1" };
                Pattern expect[] = new Pattern[ conf.length ];

                String res[] = c.config(conf, expect);

                for(int i=0; i<conf.length; i++)
                    System.out.println(conf[i] + " result: " + res[i]);

                result = CONFIGURED;

            } catch( XBeeConfigException e ) {
                System.err.println("ERROR configuring modem: " + e.getMessage());
                result = CONFIG_ERR;
            }

            c.close();
        }

        catch(gnu.io.NoSuchPortException e) {
            System.err.println("ERROR opening port: No Such Port Error");
            result = PORT_ERR;
        }

        catch(gnu.io.PortInUseException e) {
            System.err.println("ERROR opening port: port in use");
            result = PORT_ERR;
        }

        catch(gnu.io.UnsupportedCommOperationException e) {
            System.err.println("ERROR opening port: unsupported operation ... " + e.getMessage());
            result = PORT_ERR;
        }

        catch(IOException e) {
            System.err.println("IO ERROR opening port: " + e.getMessage());
            result = PORT_ERR;
        }

        return result;
    }

    public static void main(String[] args) {
        String port = "COM1";

        if( args.length > 0 )
            port = args[0];

        if( config(port, 115200) == PORT_ERR )
            config(port, 9600);
    }
}
