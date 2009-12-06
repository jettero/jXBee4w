
public class nei_test {
    public static void main(String [] args) {
        if( args.length != 1 ) {
            System.out.println("java nei_test <name>");
            System.exit(1);
        }

        NetworkEndpointInterface NEI = new NetworkEndpointInterface(args[0], "ranger", 4000);

        System.out.printf(args[0] + " NEI running%n");
    }
}
