import java.util.regex.*;

public class file {

    private static class ServerResponse {
        private static Pattern cmdre = Pattern.compile("^([12345]\\d{2})\\s+(.+)$");
        String orig;
        int code = 500;
        String msg = "server response was not parsable";

        ServerResponse(String line) {
            orig = line;
            Matcher m = cmdre.matcher(line);

            if( m.find() ) {
                code = (new Integer(m.group(1))).intValue();
                msg  = m.group(2);
            }
        }
    }

    public static void main(String args[]) {
        ServerResponse s = new ServerResponse("300 blarg");

        System.out.printf("%d %s%n", s.code, s.msg);
    }
}
