
public class TestENV {
    public static boolean test(String varname) {
        String _dump = System.getenv(varname);

        if( _dump != null )
            if( !_dump.isEmpty() )
                if( !_dump.equals("0") )
                    return true;

        return false;
    }
}
