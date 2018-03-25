

package org.videolan.vlc;

import org.videolan.libvlc.LibVLC;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class PhoneStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        if (state.equals(TelephonyManager.EXTRA_STATE_RINGING) ||
                state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {

            LibVLC libVLC = LibVLC.getExistingInstance();
            if (libVLC != null && libVLC.isPlaying())
                libVLC.pause();
        }
    }

}
