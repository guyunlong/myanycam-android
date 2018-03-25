
package com.spoledge.aacdecoder;

import android.util.Log;

import java.io.IOException;



public interface PlayerCallback {

    
    public void playerStarted();


    
    public void playerPCMFeedBuffer( boolean isPlaying, int audioBufferSizeMs, int audioBufferCapacityMs );


    
    public void playerStopped( int perf );


    
    public void playerException( Throwable t );


    
    public void playerMetadata( String key, String value );

}

