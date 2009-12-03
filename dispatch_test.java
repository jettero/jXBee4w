
public class dispatch_test {
    public static void main(String args[]) {
        XBeeDispatcher lhs = XBeeDispatcher.configuredDispatcher("LHS", true);
        XBeeDispatcher rhs = XBeeDispatcher.configuredDispatcher("RHS", true);

        try {

        lhs.setChannel( 0x0e );
        rhs.setChannel( 0x0e );
        Thread.sleep(2000);

        } catch(Exception e) { System.err.println("pfft... " + e.getMessage()); }

        lhs.close();
        rhs.close();
    }
}
