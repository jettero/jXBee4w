
public class nci_test {
    public static void main(String [] args) {
        int port;
        try { port = (new Integer(args[0])).intValue(); }
        catch(Exception e) { port = 4000; }

        NetworkControlInterface NCI = new NetworkControlInterface(port);

        System.out.printf("NCI running on port %d.%n", port);

        try { Thread.sleep(5000); }
        catch(Exception e) {}
    }
}
