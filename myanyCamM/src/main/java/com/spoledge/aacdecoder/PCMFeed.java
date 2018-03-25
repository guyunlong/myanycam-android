
package com.spoledge.aacdecoder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import android.util.Log;



public class PCMFeed implements Runnable, AudioTrack.OnPlaybackPositionUpdateListener {

    private static final String LOG = "PCMFeed";


   
   
   

    protected int sampleRate;
    protected int channels;
    protected int bufferSizeInMs;
    protected int bufferSizeInBytes;


    
    protected PlayerCallback playerCallback;


    
    protected boolean isPlaying;

    protected boolean stopped;


    
    protected short[] lsamples;


    
    protected short[] samples;


    
    protected int samplesCount;


    
    protected int writtenTotal = 0;



   
   
   

    
    protected PCMFeed( int sampleRate, int channels, int bufferSizeInBytes, PlayerCallback playerCallback ) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.bufferSizeInBytes = bufferSizeInBytes;
        this.bufferSizeInMs = bytesToMs( bufferSizeInBytes, sampleRate, channels );
        this.playerCallback = playerCallback;
    }


   
   
   

    
    public final int getSampleRate() {
        return sampleRate;
    }


    
    public final int getChannels() {
        return channels;
    }


    
    public final int getBufferSizeInBytes() {
        return bufferSizeInBytes;
    }


    
    public final int getBufferSizeInMs() {
        return bufferSizeInMs;
    }


    
    public synchronized boolean feed( short[] samples, int n ) {
        while (this.samples != null && !stopped) {
            try { wait(); } catch (InterruptedException e) {}
        }

        this.samples = samples;
        this.samplesCount = n;

        notify();

        return !stopped;
    }


    
    public synchronized void stop() {
        stopped = true;
        notify();
    }


    
    public static int msToBytes( int ms, int sampleRate, int channels ) {
        return (int)(((long) ms) * sampleRate * channels / 500);
    }


    
    public static int msToSamples( int ms, int sampleRate, int channels ) {
        return (int)(((long) ms) * sampleRate * channels / 1000);
    }


    
    public static int bytesToMs( int bytes, int sampleRate, int channels ) {
        return (int)(500L * bytes / (sampleRate * channels));
    }


    
    public static int samplesToMs( int samples, int sampleRate, int channels ) {
        return (int)(1000L * samples / (sampleRate * channels));
    }


   
   
   

    
    public void onMarkerReached( AudioTrack track ) {
    }


    
    public void onPeriodicNotification( AudioTrack track ) {
        if (playerCallback != null) {
            int buffered = 0;

            try {
                buffered = writtenTotal - track.getPlaybackHeadPosition()*channels;
            }
            catch (IllegalStateException e) {
                Log.e( LOG, "onPeriodicNotification(): illegal state=" + track.getPlayState());
                return;
            }

            int ms = samplesToMs( buffered, sampleRate, channels );

            playerCallback.playerPCMFeedBuffer( isPlaying, ms, bufferSizeInMs );
        }
    }


   
   
   

    
    public void run() {
        Log.d( LOG, "run(): sampleRate=" + sampleRate + ", channels=" + channels
            + ", bufferSizeInBytes=" + bufferSizeInBytes
            + " (" + bufferSizeInMs + " ms)");

        AudioTrack atrack = new AudioTrack(
                                AudioManager.STREAM_MUSIC,
                                sampleRate,
                                channels == 1 ?
                                    AudioFormat.CHANNEL_CONFIGURATION_MONO :
                                    AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                bufferSizeInBytes,
                                AudioTrack.MODE_STREAM );

        atrack.setPlaybackPositionUpdateListener( this );
        atrack.setPositionNotificationPeriod( msToSamples( 200, sampleRate, channels ));

        isPlaying = false;

        while (!stopped) {
           
            int ln = acquireSamples();

            if (stopped) {
                releaseSamples();
                break;
            }

           
            int writtenNow = 0;

            do {
                if (writtenNow != 0) {
                    Log.d( LOG, "too fast for playback, sleeping...");
                    try { Thread.sleep( 50 ); } catch (InterruptedException e) {}
                }

                int written = atrack.write( lsamples, writtenNow, ln );

                if (written < 0) {
                    Log.e( LOG, "error in playback feed: " + written );
                    stopped = true;
                    break;
                }

                writtenTotal += written;
                int buffered = writtenTotal - atrack.getPlaybackHeadPosition()*channels;

               

                if (!isPlaying) {
                    if (buffered*2 >= bufferSizeInBytes) {
                        Log.d( LOG, "start of AudioTrack - buffered " + buffered + " samples");
                        atrack.play();
                        isPlaying = true;
                    }
                    else {
                        Log.d( LOG, "start buffer not filled enough - AudioTrack not started yet");
                    }
                }

                writtenNow += written;
                ln -= written;
            } while (ln > 0);

            releaseSamples();
        }

        if (isPlaying) atrack.stop();
        atrack.flush();
        atrack.release();

        Log.d( LOG, "run() stopped." );
    }



   
   
   

    
    protected synchronized int acquireSamples() {
        while (samplesCount == 0 && !stopped) {
            try { wait(); } catch (InterruptedException e) {}
        }

       
        lsamples = samples;
        int ln = samplesCount;

       
        samples = null;
        samplesCount = 0;

        notify();

        return ln;
    }


    
    protected void releaseSamples() {
       
    }

}
