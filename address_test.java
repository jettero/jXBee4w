import java.io.*;
import java.util.*;

public class address_test {
    public static void main(String[] args) {
        Address64 a = new Address64("00:11:22:33:44:55:66:77");
        Address64 b = new Address64("88:99:aa:bb:cc:dd:ee:ff");
        Address64 c = new Address64("11:05:19:73:11:05:19:73");

        byte sl[] = { (byte) 0xaa };
        byte sh[] = { (byte) 0xbb };
        Address64 d = new Address64(sl, sh);

        System.out.println( a.toText() );
        System.out.println( b.toText() );
        System.out.println( c.toText() );
        System.out.println( d.toText() );

        try {
            FileOutputStream out = new FileOutputStream("dump.txt");

            out.write( a.getBytes() );
            out.write( b.getBytes() );
            out.write( c.getBytes() );
            out.write( d.getBytes() );

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
