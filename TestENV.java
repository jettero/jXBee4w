
public class TestENV {
    public static int numberize(String var) {
        String res = System.getenv(var);

        if( res == null )
            return 0;

        return (new Integer(res)).intValue();
    }

    public static boolean booleanize(String var) {
        int i = numberize(var);

        if( i != 0 ) return true;
        return false;
    }

    public static boolean test(String varname) {
        return booleanize(varname);
    }
}
