import java.io.*;
import java.util.*;

public class address_test {
    public static void main(String[] args) {
        Checksum c = new Checksum();

        System.out.println( c.toText() );

        try {
            FileOutputStream out = new FileOutputStream("dump.txt");

            out.write( c.toBytes() );
            out.close();
        }

        catch (Exception e) {
            System.err.println("file output stream exception: " + e.getMessage());
        }

        try {
            String cmd[]   = {"xxd", "dump.txt"};
            Process prcs   = Runtime.getRuntime().exec(cmd);
            InputStream in = prcs.getInputStream();

            int ch;
            StringBuffer sb = new StringBuffer(512);
            while((ch=in.read())!=-1)
                sb.append((char)ch);

            System.out.print(sb.toString());
        }

        catch (Exception e) {
            System.err.println("exec() exception: " + e.getMessage());
        }
    }
}
