import java.io.*;
import java.util.*;
import java.util.regex.*;

public class config_test {

    static boolean debug = true;

    public static void main(String[] args) {
        String port = "COM1";

        if( args.length > 0 )
            port = args[0];

        if( config(port, 115200) == XBeeConfig.SPEED_ERR )
            config(port, 9600);
    }
}
