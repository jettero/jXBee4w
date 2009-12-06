import java.util.*;
import java.net.*;
import java.io.*;

public class LineOrientedServer implements Runnable {
    public static final int GREETINGS           = 100;
    public static final int QUIT                = 299;
    public static final int UNIMPLEMENTED_ERROR = 599;

    public static class CommandResponse {
        int code = 500;
        String msg = "unknown";

        CommandResponse(int _c, String _m) {
            code = _c;
            msg = _m;
        }

        public String toLine() {
            return String.format("%03d %s", code, msg);
        }
    }

    public static class LineDispatcher implements Runnable {
        private Socket client;
        private PrintWriter out;
        private BufferedReader in;
        private LineOrientedServer cmdHandler;

        LineDispatcher(Socket s, LineOrientedServer _e) throws IOException {
            out = new PrintWriter(s.getOutputStream(), true);
             in = new BufferedReader(new InputStreamReader(s.getInputStream()));

            client = s;
            cmdHandler = _e;
        }

        public void send(CommandResponse r ) {
            out.println( r.toLine() );
        }

        public void run() {
            String line;
            String IP = client.getInetAddress().getHostAddress();

            cmdHandler.learnIP( IP );
            System.out.println("connection from " + IP);

            try {
                out.println( cmdHandler.greeting().toLine() );

                while( (line = in.readLine()) != null ) {
                    cmdHandler.setCurrent(this);
                    CommandResponse r = cmdHandler.handleCommand(line);
                    send( r );

                    if( r.code == QUIT )
                        break;
                }

            } catch(IOException e) {
                System.err.println("ERROR reading client commands: " + e.getMessage());
            }

            System.out.println("lost connection from " + IP);

            try {
                out.close();
                in.close();
                client.close();
                cmdHandler.removeMe(this);

            } catch(IOException e) {
                System.err.println("ERROR closing client command connection streams: " + e.getMessage());
            }
        }
    }

    private ServerSocket serverSocket;
    private HashSet <LineDispatcher> ld = new HashSet <LineDispatcher>();
    private LineDispatcher curld;

    public void learnIP(String ignored) {}

    public void removeMe(LineDispatcher _ld) { ld.remove(_ld); }
    public void setCurrent(LineDispatcher _ld) { curld = _ld; }
    public void send(CommandResponse r) { curld.send(r); }
    public void sendAll(CommandResponse r) {
        Iterator i = ld.iterator();

        while( i.hasNext() )
            ( (LineDispatcher) i.next() ).send(r);
    }

    public CommandResponse handleCommand(String ignored) {
        return new CommandResponse(UNIMPLEMENTED_ERROR, "command handler unimplemented");
    }

    public CommandResponse greeting() {
        return new CommandResponse(GREETINGS, "Hello, this is a geneirc line oriented server.");
    }

    public void run() {
        try {
            Socket clientSocket;

            while( (clientSocket = serverSocket.accept()) != null ) {
                try {
                    LineDispatcher _ld = new LineDispatcher(clientSocket, this);

                    (new Thread(_ld)).start();

                    ld.add(_ld);

                } catch(IOException e) {
                    System.err.println("ERROR opening client connection streams: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.printf("Could not accept connection from client: %s%n", e.getMessage());
            System.exit(1);
        }
    }

    public void listen() { (new Thread(this)).start(); }

    LineOrientedServer(int port) {
        try {
            serverSocket = new ServerSocket(port);

        } catch (IOException e) {
            System.err.printf("Could not listen on port %d: %s%n", port, e.getMessage());
            System.exit(1);
        }
    }

}
