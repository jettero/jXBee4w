
public class dispatch_test {
    public static void main(String args[]) {
        XBeeDispatcher lhs = XBeeDispatcher.configuredDispatcher("LHS", true);
        XBeeDispatcher rhs = XBeeDispatcher.configuredDispatcher("RHS", true);

        lhs.close();
        rhs.close();
    }
}
