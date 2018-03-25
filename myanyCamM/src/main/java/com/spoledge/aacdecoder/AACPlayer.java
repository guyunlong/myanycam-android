
package com.spoledge.aacdecoder;

import android.util.Log;

import gyl.cam.SoundPlay;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.net.URL;
import java.net.URLConnection;



public class AACPlayer {

    
    public static final int DEFAULT_EXPECTED_KBITSEC_RATE = 64;


    
    public static final int DEFAULT_AUDIO_BUFFER_CAPACITY_MS = 1500;


    
    public static final int DEFAULT_DECODE_BUFFER_CAPACITY_MS = 700;


    private static final String LOG = "AACPlayer";


   
   
   

    protected boolean stopped;
    protected boolean metadataEnabled = true;

    protected int audioBufferCapacityMs;
    protected int decodeBufferCapacityMs;
    protected PlayerCallback playerCallback;

    protected Decoder decoder;

   
    private int sumKBitSecRate = 0;
    private int countKBitSecRate = 0;
    private int avgKBitSecRate = 0;


   
   
   

    
    public AACPlayer() {
        this( null );
    }


    
    public AACPlayer( PlayerCallback playerCallback ) {
        this( playerCallback, DEFAULT_AUDIO_BUFFER_CAPACITY_MS, DEFAULT_DECODE_BUFFER_CAPACITY_MS );
    }


    
    public AACPlayer( PlayerCallback playerCallback, int audioBufferCapacityMs, int decodeBufferCapacityMs ) {
        setPlayerCallback( playerCallback );
        setAudioBufferCapacityMs( audioBufferCapacityMs );
        setDecodeBufferCapacityMs( decodeBufferCapacityMs );

        decoder = createDecoder();
    }


   
   
   

    
    public Decoder getDecoder() {
        return decoder;
    }


    
    public void setDecoder( Decoder decoder ) {
        this.decoder = decoder;
    }
    

    
    public void setAudioBufferCapacityMs( int audioBufferCapacityMs ) {
        this.audioBufferCapacityMs = audioBufferCapacityMs;
    }


    
    public int getAudioBufferCapacityMs() {
        return audioBufferCapacityMs;
    }


    
    public void setDecodeBufferCapacityMs( int decodeBufferCapacityMs ) {
        this.decodeBufferCapacityMs = decodeBufferCapacityMs;
    }


    
    public int getDecodeBufferCapacityMs() {
        return decodeBufferCapacityMs;
    }


    
    public void setPlayerCallback( PlayerCallback playerCallback ) {
        this.playerCallback = playerCallback;
    }


    
    public PlayerCallback getPlayerCallback() {
        return playerCallback;
    }


    
    public boolean getMetadataEnabled() {
        return metadataEnabled;
    }


    
    public void setMetadataEnabled( boolean metadataEnabled ) {
        this.metadataEnabled = metadataEnabled;
    }

    public void playMyanyCam(){
        new Thread(new Runnable() {
            public void run() {
                try {
                  play(SoundPlay.cbb.getInputStream());
                }
                catch (Exception e) {
                    Log.e( LOG, "playAsync():", e);

                    if (playerCallback != null) playerCallback.playerException( e );
                }
            }
        }).start();
    }

    
    public void playAsync( final String url ) {
        playAsync( url, -1 );
    }


    
    public void playAsync( final String url, final int expectedKBitSecRate ) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    play( url, expectedKBitSecRate );
                }
                catch (Exception e) {
                    Log.e( LOG, "playAsync():", e);

                    if (playerCallback != null) playerCallback.playerException( e );
                }
            }
        }).start();
    }


    
    public void play( String url ) throws Exception {
        play( url, -1 );
    }


    
    public void play( String url, int expectedKBitSecRate ) throws Exception {
        if (url.indexOf( ':' ) > 0) {
            URLConnection cn = new URL( url ).openConnection();

            prepareConnection( cn );
            cn.connect();
            processHeaders( cn );

           
            play( getInputStream( cn ), expectedKBitSecRate);
        }
        else play( new FileInputStream( url ), expectedKBitSecRate );
    }


    
    public void play( InputStream is ) throws Exception {
        play( is, -1 );
    }


    
    public final void play( InputStream is, int expectedKBitSecRate ) throws Exception {
        stopped = false;

        if (playerCallback != null) playerCallback.playerStarted();

        if (expectedKBitSecRate <= 0) expectedKBitSecRate = DEFAULT_EXPECTED_KBITSEC_RATE;

        sumKBitSecRate = 0;
        countKBitSecRate = 0;

        playImpl( is, expectedKBitSecRate );
    }


    
    public void stop() {
        stopped = true;
    }


   
   
   

    
    protected void playImpl( InputStream is, int expectedKBitSecRate ) throws Exception {
        BufferReader reader = new BufferReader(
                                        computeInputBufferSize( expectedKBitSecRate, decodeBufferCapacityMs ),
                                        is );
        new Thread( reader ).start();

        PCMFeed pcmfeed = null;
        Thread pcmfeedThread = null;

       
        long profMs = 0;
        long profSamples = 0;
        long profSampleRate = 0;
        int profCount = 0;

        try {
            Decoder.Info info = decoder.start( reader );

            Log.d( LOG, "play(): samplerate=" + info.getSampleRate() + ", channels=" + info.getChannels());

            profSampleRate = info.getSampleRate() * info.getChannels();

            if (info.getChannels() > 2) {
                throw new RuntimeException("Too many channels detected: " + info.getChannels());
            }

           
           
           
           
            short[][] decodeBuffers = createDecodeBuffers( 3, info );
            short[] decodeBuffer = decodeBuffers[0]; 
            int decodeBufferIndex = 0;

            pcmfeed = createPCMFeed( info );
            pcmfeedThread = new Thread( pcmfeed );
            pcmfeedThread.start();

            do {
                long tsStart = System.currentTimeMillis();

                info = decoder.decode( decodeBuffer, decodeBuffer.length );
                int nsamp = info.getRoundSamples();

                profMs += System.currentTimeMillis() - tsStart;
                profSamples += nsamp;
                profCount++;

                Log.d( LOG, "play(): decoded " + nsamp + " samples" );

                if (nsamp == 0 || stopped) break;
                if (!pcmfeed.feed( decodeBuffer, nsamp ) || stopped) break;

                int kBitSecRate = computeAvgKBitSecRate( info );
                if (Math.abs(expectedKBitSecRate - kBitSecRate) > 1) {
                    Log.i( LOG, "play(): changing kBitSecRate: " + expectedKBitSecRate + " -> " + kBitSecRate );
                    reader.setCapacity( computeInputBufferSize( kBitSecRate, decodeBufferCapacityMs ));
                    expectedKBitSecRate = kBitSecRate;
                }

                decodeBuffer = decodeBuffers[ ++decodeBufferIndex % 3 ];
            } while (!stopped);
        }
        finally {
            stopped = true;

            if (pcmfeed != null) pcmfeed.stop();
            decoder.stop();
            reader.stop();

            int perf = 0;

            if (profCount > 0) Log.i( LOG, "play(): average decoding time: " + profMs / profCount + " ms");

            if (profMs > 0) {
                perf = (int)((1000*profSamples / profMs - profSampleRate) * 100 / profSampleRate);

                Log.i( LOG, "play(): average rate (samples/sec): audio=" + profSampleRate
                    + ", decoding=" + (1000*profSamples / profMs)
                    + ", audio/decoding= " + perf
                    + " %  (the higher, the better; negative means that decoding is slower than needed by audio)");
            }

            if (pcmfeedThread != null) pcmfeedThread.join();

            if (playerCallback != null) playerCallback.playerStopped( perf );
        }
    }


    protected Decoder createDecoder() {
        return Decoder.create();
    }


    protected short[][] createDecodeBuffers( int count, Decoder.Info info ) {
        int size = PCMFeed.msToSamples( decodeBufferCapacityMs, info.getSampleRate(), info.getChannels());

        short[][] ret = new short[ count ][];

        for (int i=0; i < ret.length; i++) {
            ret[i] = new short[ size ];
        }

        return ret;
    }


    protected PCMFeed createPCMFeed( Decoder.Info info ) {
        int size = PCMFeed.msToBytes( audioBufferCapacityMs, info.getSampleRate(), info.getChannels());

        return new PCMFeed( info.getSampleRate(), info.getChannels(), size, playerCallback );
    }


    
    protected void prepareConnection( URLConnection conn ) {
       
        if (metadataEnabled) conn.setRequestProperty("Icy-MetaData", "1");
    }


    
    protected InputStream getInputStream( URLConnection conn ) throws Exception {
        String smetaint = conn.getHeaderField( "icy-metaint" );
        InputStream ret = conn.getInputStream();

        if (!metadataEnabled) {
            Log.i( LOG, "Metadata not enabled" );
        }
        else if (smetaint != null) {
            int period = -1;
            try {
                period = Integer.parseInt( smetaint );
            }
            catch (Exception e) {
                Log.e( LOG, "The icy-metaint '" + smetaint + "' cannot be parsed: '" + e );
            }

            if (period > 0) {
                Log.i( LOG, "The dynamic metainfo is sent every " + period + " bytes" );

                ret = new IcyInputStream( ret, period, playerCallback );
            }
        }
        else Log.i( LOG, "This stream does not provide dynamic metainfo" );

        return ret;
    }


    
    protected void processHeaders( URLConnection cn ) {
        dumpHeaders( cn );

        if (playerCallback != null) {
            for (java.util.Map.Entry<String, java.util.List<String>> me : cn.getHeaderFields().entrySet()) {
                for (String s : me.getValue()) {
                    playerCallback.playerMetadata( me.getKey(), s );
                }
            }
        }
    }


    protected void dumpHeaders( URLConnection cn ) {
        for (java.util.Map.Entry<String, java.util.List<String>> me : cn.getHeaderFields().entrySet()) {
            for (String s : me.getValue()) {
                Log.d( LOG, "header: key=" + me.getKey() + ", val=" + s);
            }
        }
    }


    protected int computeAvgKBitSecRate( Decoder.Info info ) {
       
        if (countKBitSecRate < 64) {
            int kBitSecRate = computeKBitSecRate( info );
            int frames = info.getRoundFrames();

            sumKBitSecRate += kBitSecRate * frames;
            countKBitSecRate += frames;
            avgKBitSecRate = sumKBitSecRate / countKBitSecRate;
        }

        return avgKBitSecRate;
    }


    protected static int computeKBitSecRate( Decoder.Info info ) {
        if (info.getRoundSamples() <= 0) return -1;

        return computeKBitSecRate( info.getRoundBytesConsumed(), info.getRoundSamples(),
                                   info.getSampleRate(), info.getChannels());
    }


    protected static int computeKBitSecRate( int bytesconsumed, int samples, int sampleRate, int channels ) {
        long ret = 8L * bytesconsumed * channels * sampleRate / samples;

        return (((int)ret) + 500) / 1000;
    }


    protected static int computeInputBufferSize( int kbitSec, int durationMs ) {
        return kbitSec * durationMs / 8;
    }


    protected static int computeInputBufferSize( Decoder.Info info, int durationMs ) {

        return computeInputBufferSize( info.getRoundBytesConsumed(), info.getRoundSamples(),
                                        info.getSampleRate(), info.getChannels(), durationMs );
    }


    protected static int computeInputBufferSize( int bytesconsumed, int samples,
                                                 int sampleRate, int channels, int durationMs ) {

        return (int)(((long) bytesconsumed) * channels * sampleRate * durationMs  / (1000L * samples));
    }

}
