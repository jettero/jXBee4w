
public class Address64 {
    byte addr[];

    Address64() { this(new byte[0], new byte[0]); }

    Address64(String s) throws Address64Exception {
        String a[] = s.split(":");

        if( a.length != 8 )
            throw new Address64Exception("addresses should be 8 hex octets in xx:xx:xx:xx:xx:xx:xx:xx form");

        addr = new byte[8];

        for(int i=0; i<a.length; i++) {
            if( !a[i].matches("^[0-9a-fA-F]{1,2}$") )
                throw new Address64Exception("octet("+i+") is not the right length or contains invalid characters (hex only)");

            addr[i] = (byte) Integer.parseInt(a[i], 16);
        }
    }

    public boolean equals(Object o) { // to override the Object.equals(Object o), it has to be Object, not Address64
        if( o == null )
            return false;

        if( !( o instanceof Address64 ) )
            return false;

        byte b[] = ( (Address64) o ).getBytes();
        for(int i=0; i<addr.length; i++)
            if( b[i] != addr[i] )
                return false;

        return true;
    }

    public int hashCode() {
        int ret = 0;

        for(int i=0; i<4; i++)
            ret += (addr[i] << i) + (addr[i+4] << i);

        return ret;
    }

    private byte[] padaddr(byte b[], int l) {
        if(b.length>=l)
            return b;

        byte n[] = new byte[l];
        int diff = n.length - b.length;

        for(int i=0; i<diff; i++)
            n[i] = 0;

        for(int i=0; i<b.length; i++)
            n[diff+i] = b[i];

        return n;
    }

    Address64(byte _addr[]) throws Address64Exception {
        addr = padaddr(_addr, 8);
    }

    Address64(byte SH[], byte SL[]) throws Address64Exception {
        if( SL.length < 4 ) SL = padaddr(SL, 4);
        if( SH.length < 4 ) SH = padaddr(SH, 4);

        if( SH.length != 4 ) throw new Address64Exception("SH address must be 4 bytes (not " + SH.length + ")");
        if( SL.length != 4 ) throw new Address64Exception("SL address must be 4 bytes (not " + SL.length + ")");

        addr = new byte[8];
        for(int i=0; i<4; i++) {
            addr[i]   = SH[i];
            addr[i+4] = SL[i];
        }
    }

    public byte[] getBytes() {
        return addr;
    }

    public String toText() {
        String s = "";
        String d = Integer.toHexString(addr[0] & 0xff); // java has no unsigned type, so squash the "negative" bits for printing

        if( d.length() == 1 )
             s = "0" + d;
        else s =  d;

        for(int i=1; i<addr.length; i++) {
            d = Integer.toHexString(addr[i] & 0xff); // java has no unsigned type, so squash the "negative" bits for printing
            if( d.length() == 1 )
                 s += ":0" + d;
            else s += ":" + d;
        }

        return s;
    }
}
