
package com.spoledge.aacdecoder;

import android.util.Log;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;



public class IcyInputStream extends FilterInputStream {

    private static final String LOG = "IcyInputStream";


   
   
   


    
    protected int period;


    
    protected int remaining;


    
    protected byte[] mbuffer;


    
    protected PlayerCallback playerCallback;


   
   
   

    
    public IcyInputStream( InputStream in, int period ) {
        this( in, period, null );
    }


    
    public IcyInputStream( InputStream in, int period, PlayerCallback playerCallback ) {
        super( in );
        this.period = period;
        this.playerCallback = playerCallback;

        remaining = period;
        mbuffer = new byte[128];
    }


   
   
   

    @Override
    public int read() throws IOException {
        int ret = super.read();

        if (--remaining == 0) fetchMetadata();

        return ret;
    }


    @Override
    public int read( byte[] buffer, int offset, int len ) throws IOException {
        int ret = in.read( buffer, offset, remaining < len ? remaining : len );

        if (remaining == ret) fetchMetadata();
        else remaining -= ret;

        return ret;
    }


   
   
   

    
    protected void fetchMetadata() throws IOException {
        remaining = period;

        int size = in.read();

       
        if (size < 1) return;

       
        size <<= 4;

        if (mbuffer.length < size) {
            mbuffer = null;
            mbuffer = new byte[ size ];
            Log.d( LOG, "Enlarged metadata buffer to " + size + " bytes");
        }

        size = readFully( mbuffer, 0, size );

       
        for (int i=0; i < size; i++) {
            if (mbuffer[i] == 0) {
                size = i;
                break;
            }
        }

        String s;

        try {
            s = new String( mbuffer, 0, size, "UTF-8" );
        }
        catch (Exception e) {
            Log.e( LOG, "Cannot convert bytes to String" );
            return;
        }

        Log.d( LOG, "Metadata string: " + s );

        parseMetadata( s );
    }


    
    protected void parseMetadata( String s ) {
        String[] kvs = s.split( ";" );

        for (String kv : kvs) {
            int n = kv.indexOf( '=' );
            if (n < 1) continue;

            boolean isString = n + 1 < kv.length()
                                && kv.charAt( kv.length() - 1) == '\''
                                && kv.charAt( n + 1 ) == '\'';

            String key = kv.substring( 0, n );
            String val = isString ?
                            kv.substring( n+2, kv.length()-1) :
                            n + 1 < kv.length() ?
                                kv.substring( n+1 ) : "";

           
            if (playerCallback != null) playerCallback.playerMetadata( key, val );
        }
    }


    
    protected final int readFully( byte[] buffer, int offset, int size ) throws IOException {
        int n;
        int oo = offset;

        while (size > 0 && (n = in.read( buffer, offset, size )) != -1) {
            offset += n;
            size -= n;
        }

        return offset - oo;
    }

}

