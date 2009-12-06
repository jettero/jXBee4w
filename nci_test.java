
public class nci_test {
    public static void main(String [] args) {
        int port;
        try { port = (new Integer(args[0])).intValue(); }
        catch(Exception e) { port = 4000; }

        NetworkControlInterface NCI = new NetworkControlInterface(port);

        System.out.println("NCI running.");
    }
}
