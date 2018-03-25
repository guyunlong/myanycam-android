
package org.videolan.vlc;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.KeyEvent;


public class RemoteControlClientReceiver extends BroadcastReceiver {
    @SuppressWarnings("unused")
    private static final String TAG = "VLC/RemoteControlClientReceiver";

    
    private static long mHeadsetDownTime = 0;
    private static long mHeadsetUpTime = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LibVLC mLibVLC;
        try {
            mLibVLC = Util.getLibVlcInstance();
        } catch (LibVlcException e) {
            return;
        }
        if(mLibVLC == null)
            return;

        if(action.equalsIgnoreCase(Intent.ACTION_MEDIA_BUTTON)) {

            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null)
                return;

            if (event.getKeyCode() != KeyEvent.KEYCODE_HEADSETHOOK &&
                event.getKeyCode() != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE &&
                event.getAction() != KeyEvent.ACTION_DOWN)
                return;

            Intent i = null;
            switch (event.getKeyCode())
            {
            
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    long time = SystemClock.uptimeMillis();
                    switch (event.getAction())
                    {
                        case KeyEvent.ACTION_DOWN:
                            if (event.getRepeatCount() > 0)
                                break;
                            mHeadsetDownTime = time;
                            break;
                        case KeyEvent.ACTION_UP:
                           
                            if (time - mHeadsetDownTime >= 1000) {
                                time = 0;
                               
                            } else if (time - mHeadsetUpTime <= 500) {
                            }
                           
                            else {
                                if (mLibVLC.isPlaying())
                                    i = new Intent(AudioService.ACTION_REMOTE_PAUSE);
                                else
                                    i = new Intent(AudioService.ACTION_REMOTE_PLAY);
                            }
                            mHeadsetUpTime = time;
                            break;
                    }
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    i = new Intent(AudioService.ACTION_REMOTE_PLAY);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    i = new Intent(AudioService.ACTION_REMOTE_PAUSE);
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    i = new Intent(AudioService.ACTION_REMOTE_STOP);
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    break;
            }

            if (isOrderedBroadcast())
                abortBroadcast();
            if(i != null)
                context.sendBroadcast(i);
        }
    }
}
