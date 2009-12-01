
public class neh_test {
    public static void main(String args[]) {
        NetworkEndpointHandle lhs = NetworkEndpointHandle.configuredEndpoint("LHS", true);
        NetworkEndpointHandle rhs = NetworkEndpointHandle.configuredEndpoint("RHS", true);

        lhs.close();
        rhs.close();
    }
}
