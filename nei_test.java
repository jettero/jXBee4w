
public class nei_test {
    public static void main(String [] args) {
        if( args.length != 3 ) {
            System.out.println("java nei_test <name> <host> <port>");
            System.exit(1);
        }

        (new Thread(

            new NetworkEndpointInterface(args[0], args[1], new Integer(args[2]).intValue())

        )).start();

        System.out.printf(args[0] + " NEI running%n");
    }
}
