import java.io.*;
import java.util.*;
import java.util.regex.*;

public class config_test {
    public static void main(String[] args) {
        String port = "COM1";

        if( args.length > 0 )
            port = args[0];

        try {
            XBeeConfig c = new XBeeConfig(port, 115200);

            c.debug = true;

            try {
                String conf[]    = { "ATRE", "ATBD7", "ATAP1" };
                Pattern expect[] = new Pattern[ conf.length ];

                String res[] = c.config(conf,expect);

                for(int i=0; i<conf.length; i++)
                    System.out.println(conf[i] + " result: " + res[i]);

            } catch( XBeeConfigException e ) {
                System.err.println("ERROR configuring modem: " + e.getMessage());
            }

            c.close();
        }

        catch(gnu.io.NoSuchPortException e) {
            System.err.println("ERROR opening port: No Such Port Error");
        }

        catch(gnu.io.PortInUseException e) {
            System.err.println("ERROR opening port: port in use");
        }

        catch(gnu.io.UnsupportedCommOperationException e) {
            System.err.println("ERROR opening port: unsupported operation ... " + e.getMessage());
        }

        catch(IOException e) {
            System.err.println("IO ERROR opening port: " + e.getMessage());
        }
    }
}
