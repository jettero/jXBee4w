
public class Address64 {
    byte addr[];

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

    Address64(byte SH[], byte SL[]) throws Address64Exception {
        if( SH.length != 4 ) throw new Address64Exception("SH address must be 4 bytes (not " + SH.length + ")");
        if( SL.length != 4 ) throw new Address64Exception("SL address must be 4 bytes (not " + SL.length + ")");

        addr = new byte[8];
        for(int i=0; i<4; i++) {
            addr[i]   = SH[i];
            addr[i+4] = SL[i];
        }
    }

    public byte[] serialize() {
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
