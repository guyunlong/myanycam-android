

package org.videolan.libvlc;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class EventHandler {

    

   
   
   
   
   
   

   
   
   
   
    public static final int MediaPlayerPlaying                = 0x104;
    public static final int MediaPlayerPaused                 = 0x105;
    public static final int MediaPlayerStopped                = 0x106;
   
   
    public static final int MediaPlayerEndReached             = 0x109;
    public static final int MediaPlayerEncounteredError       = 0x10a;
   
    public static final int MediaPlayerPositionChanged        = 0x10c;
   
   
   
   
   
    public static final int MediaPlayerVout                   = 0x112;

    public static final int MediaListItemAdded                = 0x200;
   
    public static final int MediaListItemDeleted              = 0x202;
   

   
   
   
   

   
   
   

   
   

   
   
   
   
   
   
   
   
   
   
   

    private ArrayList<Handler> mEventHandler;
    private static EventHandler mInstance;

    private EventHandler() {
        mEventHandler = new ArrayList<Handler>();
    }

    public static EventHandler getInstance() {
        if (mInstance == null) {
            mInstance = new EventHandler();
        }
        return mInstance;
    }

    public void addHandler(Handler handler) {
        if (!mEventHandler.contains(handler))
            mEventHandler.add(handler);
    }

    public void removeHandler(Handler handler) {
        mEventHandler.remove(handler);
    }

    
    public void callback(int event, Bundle b) {
        b.putInt("event", event);
        for (int i = 0; i < mEventHandler.size(); i++) {
            Message msg = Message.obtain();
            msg.setData(b);
            mEventHandler.get(i).sendMessage(msg);
        }
    }
}
