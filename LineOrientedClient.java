import java.io.*;
import java.net.*;
import java.util.regex.*;

public class LineOrientedClient {
    // private static class ServerResponse {{{
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

        public boolean ok() {
            if( code >= 200 && code < 400 )
                return true;

            return false;
        }
    }
// }}}
    // private static class ServerReader implements Runnable {{{
    private static class ServerReader implements Runnable {
        BufferedReader     in;
        LineOrientedClient out;

        ServerReader(InputStream _s, LineOrientedClient _o) {
            in  = new BufferedReader(new InputStreamReader(_s));
            out = _o;
        }

        public void run() {
            try {
                String line;

                while( (line = in.readLine()) != null )
                    out.handleServerResponse(line);

            } catch(IOException e) {
                System.err.println("ERROR reading server responses: " + e.getMessage());
            }

            try {
                out.close();
                in.close();

            } catch(IOException e) {
                System.err.println("ERROR closing server connection streams: " + e.getMessage());
            }
        }
    }
    // }}}

    ServerReader in;
    Socket server;
    PrintWriter out;

    LineOrientedClient(String host, int port) {
        try {
            server = new Socket(host, port);
            out    = new PrintWriter(server.getOutputStream(), true);
            in     = new ServerReader(server.getInputStream(), this);

            (new Thread(in)).start();

        } catch (UnknownHostException e) {
            System.err.printf("ERROR connecting to host %s on port %d: %s%n", host, port, e.getMessage());
            System.exit(1);

        } catch (IOException e) {
            System.err.println("ERROR building streams from network connection.");
            e.printStackTrace();
            System.exit(1);
        }   
    }

    public void close() {
        try {
            server.close();
            out.close();

        } catch(IOException e) {
            System.err.println("ERROR closing server connection streams: " + e.getMessage());
        }
    }

    public void handleServerResponse(String line) {
        ServerResponse s = new ServerResponse(line);

        System.out.println( s.ok() ? "Server Says: " + s.msg : String.format("ERROR(%d): %s", s.code, s.msg) );
    }
}
