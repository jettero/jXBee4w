
public class Address64 {
    byte addr[];

    Address64(byte SH[], byte SL[]) throws Address64Exception {
        if( SH.length != 4 ) throw new Address64Exception("SH address must be 4 bytes (not " + SH.length + ")");
        if( SL.length != 4 ) throw new Address64Exception("SL address must be 4 bytes (not " + SL.length + ")");

        addr = new byte[8];
        for(int i=0; i<4; i++) {
            addr[i]   = SH[i];
            addr[i+4] = SL[i];
        }
    }
}
