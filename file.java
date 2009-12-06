
public class file {
    private String[] subarr(String a[], int i) { return subarr(a, i, a.length-1); }
    private String[] subarr(String a[], int i, int j) {
        int k = Math.abs(i - j) + 1;

        String ret[] = new String[k];
        for(int w=0; w<k; w++)
            ret[w] = a[w+i];

        return ret;
    }

    private String cat(String s[]) {
        if( s.length < 1 )
            return "";

        String ret = s[0];
        for( String _s : subarr(s, 1) )
                ret += " " + _s;

        return ret;
    }

    public static void main(String args[]) {
        //              0      1        2       3       4      5     6        7
        String s[] = { "hi", "man", "wassup", "dude", "one", "two", "three", "four" };
        String t[] = subarr(s, 1);
        String u[] = subarr(s, 3, 4);

        System.out.println(cat(s));
        System.out.println(cat(t));
        System.out.println(cat(u));
    }
}
