
public class Address64 {
    byte addr[];

    Address64(String s) {
        addr = new byte[8];
        addr[0] = 0x00;
        addr[1] = 0x11;
        addr[2] = 0x22;
        addr[3] = 0x33;
        addr[4] = 0x44;
        addr[5] = 0x55;
        addr[6] = 0x66;
        addr[7] = 0x77;
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

    public String toText() {
        String s = "";
        String d = Integer.toHexString(addr[0]);

        if( d.length() == 1 )
             s = "0" + d;
        else s =  d;

        for(int i=1; i<addr.length; i++) {
            d = Integer.toHexString(addr[0]);
            if( d.length() == 1 )
                 s += ":0" + d;
            else s += ":" + d;
        }

        return s;
    }
}
