import java.io.*;
import java.util.*;
import java.util.regex.*;

public class config_test {
    public static final int UNKNOWN    = 0;
    public static final int CONFIGURED = 1;
    public static final int CONFIG_ERR = 2;
    public static final int SPEED_ERR  = 4;
    public static final int PORT_ERR   = 8;

    static boolean debug = true;

    public static int config(String port, int speed) {
        int result = UNKNOWN;

        try {
            XBeeConfig c = XBeeConfig.newFromPortName(port, speed, true); // the last value is whether to print debugging info

            try {
                String conf[]    = { "ATRE", "ATBD7", "ATAP1", "ATHV", "ATVR" };
                Pattern expect[] = new Pattern[ conf.length ];

                Pattern _OK = Pattern.compile("^OK$");
                expect[0] = expect[1] = expect[2] = _OK;
                expect[conf.length-1] = Pattern.compile("^10CD$");

                String res[] = c.config(conf, expect);
                for(int i=0; i<conf.length; i++)
                    System.out.println(conf[i] + " result: " + res[i]);

                result = CONFIGURED; // used by the linespeed retry loop

                // not a debug message
                System.out.println("XBee version " + conf[conf.length-2]
                    + " running firmware revision " + conf[conf.length-1] + " configured successfully");

            } catch( XBeeConfigException e ) {
                System.err.println("ERROR configuring modem: " + e.getMessage());
                result = CONFIG_ERR;
                if( e.probably_linespeed )
                    result = SPEED_ERR;
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

        if( config(port, 115200) == SPEED_ERR )
            config(port, 9600);
    }
}
