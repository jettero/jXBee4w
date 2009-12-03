import java.io.*;
import java.util.*;
import java.util.regex.*;

public class config_test {
    static boolean debug = true;

    public static void main(String[] args) {
        String port = System.getenv("PORT");

        if( port == null )
            System.exit(0);

        if( args.length > 0 )
            port = args[0];

        if( XBeeConfig.config(port, 115200, true) == XBeeConfig.SPEED_ERR )
            XBeeConfig.config(port, 9600, true);
    }
}
