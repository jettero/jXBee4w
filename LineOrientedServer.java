
public class LineOrientedServer {

    public static interface ClientCommandEvent {
        public CommandResponse handleCommand(String cmd);
    }

    public static class CommandResponse {
        int code = 500;
        String msg = "unknown";

        CommandResponse(int _c, String _m) {
            code = _c;
            msg = _m;
        }
    }

    ClientCommandEvent clientHandler;

    LineOrientedServer(ClientCommandEvent _e, int listenPort) {
        clientHandler = _e;
    }

}
