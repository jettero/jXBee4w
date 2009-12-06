import java.net.*;
import java.io.*;

public class LineOrientedServer {

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

    public static interface HandleLineEvent {
        public CommandResponse handleCommand(String cmd);
    }

    public static class LineDispatcher implements Runnable {
        private Socket client;
        private PrintWriter out;
        private BufferedReader in;
        private HandleLineEvent cmdHandler;

        LineDispatcher(Socket s, HandleLineEvent _e) {
            out = new PrintWriter(s.getOutputStream(), true);
             in = new BufferedReader(new InputStreamReader(s.getInputStream()));

            client = s;
            cmdHandler = _e;
        }

        public void run() {
            String line;

            while( (line = in.readLine()) != null )
                out.println( cmdHandler.handleCommand(line).toLine() );

            out.close();
            in.close();
            client.close();
        }
    }


    ServerSocket serverSocket;

    public CommandResponse handleCommand(String ignored) {
        return new CommandResponse(UNIMPLEMENTED_ERROR, "command handler unimplemented");
    }

    LineOrientedServer(int listenPort) {
        try {
            serverSocket = new ServerSocket(listenPort);
            Socket clientSocket;

            while( (clientSocket = serverSocket.accept()) != null ) {
                (new Thread( new LineDispatcher(clientSocket, this) )).start();
            }

        } catch (IOException e) {
            System.err.printf("Could not listen on port %d: %s%n", listenPort, e.getMessage());
            System.exit(1);
        }
    }

}
