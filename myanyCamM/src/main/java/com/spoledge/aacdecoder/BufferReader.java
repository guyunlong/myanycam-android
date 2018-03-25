
package com.spoledge.aacdecoder;

import android.util.Log;

import java.io.InputStream;
import java.io.IOException;



public class BufferReader implements Runnable {

    public static class Buffer {
        private byte[] data;
        private int size;

        Buffer( int capacity ) {
            data = new byte[ capacity ];
        }

        public final byte[] getData() {
            return data;
        }

        public final int getSize() {
            return size;
        }
    }

    private static String LOG = "BufferReader";

    int capacity;

    private Buffer[] buffers;

    
    private int indexMine;

    
    private int indexBlocked;

    private boolean stopped;

    private InputStream is;


   
   
   

    
    public BufferReader( int capacity, InputStream is ) {
        this.capacity = capacity;
        this.is = is;

        Log.d( LOG, "init(): capacity=" + capacity );

        buffers = new Buffer[3];

        for (int i=0; i < buffers.length; i++) {
            buffers[i] = new Buffer( capacity );
        }

        indexMine = 0;
        indexBlocked = buffers.length-1;
    }


   
   
   

    
    public synchronized void setCapacity( int capacity ) {
        Log.d( LOG, "setCapacity(): " + capacity );
        this.capacity = capacity;
    }


    
    public void run() {
        Log.d( LOG, "run() started...." );

        int cap = capacity;
        int total = 0;

        while (!stopped) {
            Buffer buffer = buffers[ indexMine ];
            total = 0;

            if (cap != buffer.data.length) {
                Log.d( LOG, "run() capacity changed: " + buffer.data.length + " -> " + cap);
                buffers[ indexMine ] = buffer = null;
                buffers[ indexMine ] = buffer = new Buffer( cap );
            }

            while (!stopped && total < cap) {
                try {
                    int n = is.read( buffer.data, total, cap - total );

                    if (n == -1) stopped = true;
                    else total += n;
                }
                catch (IOException e) {
                    Log.e( LOG, "Exception when reading: " + e );
                    stopped = true;
                }
            }

            buffer.size = total;

            synchronized (this) {
                notify();
                int indexNew = (indexMine + 1) % buffers.length;

                while (!stopped && indexNew == indexBlocked) {
                    Log.d( LOG, "run() waiting...." );
                    try { wait(); } catch (InterruptedException e) {}
                    Log.d( LOG, "run() awaken" );
                }

                indexMine = indexNew;
                cap = capacity;
            }
        }

        Log.d( LOG, "run() stopped." );
    }


    
    public synchronized void stop() {
        stopped = true;
        notify();
    }


    
    public boolean isStopped() {
        return stopped;
    }


    
    public synchronized Buffer next() {
        int indexNew = (indexBlocked + 1) % buffers.length;

        while (!stopped && indexNew == indexMine) {
            Log.d( LOG, "next() waiting...." );
            try { wait(); } catch (InterruptedException e) {}
            Log.d( LOG, "next() awaken" );
        }

        if (indexNew == indexMine) return null;

        indexBlocked = indexNew;

        notify();

        return buffers[ indexBlocked ]; 
    }

}

