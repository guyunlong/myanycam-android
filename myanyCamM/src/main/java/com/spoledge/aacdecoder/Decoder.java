
package com.spoledge.aacdecoder;



public class Decoder {

    
    public static final class Info {
        private int sampleRate;
        private int channels;

        private int frameMaxBytesConsumed;
        private int frameSamples;

        private int roundFrames;
        private int roundBytesConsumed;
        private int roundSamples;


       
       
       

        
        public int getSampleRate() {
            return sampleRate;
        }


        
        public int getChannels() {
            return channels;
        }


        
        public int getFrameMaxBytesConsumed() {
            return frameMaxBytesConsumed;
        }


        
        public int getFrameSamples() {
            return frameSamples;
        }


        
        public int getRoundFrames() {
            return roundFrames;
        }


        
        public int getRoundBytesConsumed() {
            return roundBytesConsumed;
        }


        
        public int getRoundSamples() {
            return roundSamples;
        }
    }


    protected static int STATE_IDLE = 0;
    protected static int STATE_RUNNING = 1;

    private static boolean libLoaded = false;


   
   
   

    
    protected int decoder;


    
    protected int aacdw;


    
    protected int state = STATE_IDLE;


    
    protected Info info;


   
   
   

    protected Decoder( int decoder ) {
        this.decoder = decoder;
    }


   
   
   

    
    public static synchronized void loadLibrary() {
        if (!libLoaded) {
            System.loadLibrary( "aacdecoder" );

            libLoaded = true;
        }
    }


    
    public static Decoder create() {
        return create( 0 );
    }


    
    public static Decoder createByName( String name ) {
        loadLibrary();

        int aacdw = nativeDecoderGetByName( name );

        return aacdw != 0 ? create( aacdw ) : null;
    }


    
    public static synchronized Decoder create( int decoder ) {
        loadLibrary();

        return new Decoder( decoder );
    }


    
    public Info start( BufferReader reader ) {
        if (state != STATE_IDLE) throw new IllegalStateException();

        info = new Info();

        aacdw = nativeStart( decoder, reader, info );

        if (aacdw == 0) throw new RuntimeException("Cannot start native decoder");

        state = STATE_RUNNING;

        return info;
    }


    
    public Info decode( short[] samples, int outLen ) {
        if (state != STATE_RUNNING) throw new IllegalStateException();

        nativeDecode( aacdw, samples, outLen );

        return info;
    }


    
    public void stop() {
        if (aacdw != 0) {
            nativeStop( aacdw );
            aacdw = 0;
        }

        state = STATE_IDLE;
    }


   
   
   

    @Override
    protected void finalize() {
        try {
            stop();
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }


   
   
   


    
    protected native int nativeStart( int decoder, BufferReader reader, Info info );


    
    protected native int nativeDecode( int aacdw, short[] samples, int outLen );


    
    protected native void nativeStop( int aacdw );


    
    protected static native int nativeDecoderGetByName( String name );


}

