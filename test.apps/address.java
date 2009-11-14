import java.io.*;
import java.util.*;

public class test {
    public static void main(String[] args) {
        Address64 a = new Address64("00:11:22:33:44:55:66:77");

        System.out.println( a.toText() );
    }
}
