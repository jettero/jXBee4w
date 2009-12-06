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
    }

    public static class LineDispatcher {
    }

    ServerSocket serverSocket;

    LineOrientedServer(int listenPort) {
        try {
            serverSocket = new ServerSocket(listenPort);
            Socket clientSocket;

            while( (clientSocket = serverSocket.accept()) != null ) {
                System.out.println("do stuff here");
            }

        } catch (IOException e) {
            System.err.printf("Could not listen on port %d: %s%n", listenPort, e.getMessage());
            System.exit(1);
        }
    }

}
