

package org.videolan.vlc.widget;

import org.videolan.vlc.interfaces.IPlayerControl;
import org.videolan.vlc.interfaces.OnPlayerControlListener;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.myanycamm.cam.R;

public class PlayerControlClassic extends LinearLayout implements IPlayerControl {
    public final static String TAG = "VLC/PlayerControlClassic";

    private ImageButton mPlayPause;
    private OnPlayerControlListener listener = null;

    public PlayerControlClassic(Context context) {
        super(context);

        LayoutInflater.from(context).inflate(R.layout.player_contol_classic, this, true);

        mPlayPause = (ImageButton) findViewById(R.id.player_overlay_play);
        mPlayPause.setOnClickListener(mPlayPauseListener);
    }

    private OnClickListener mBackwardListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (listener != null)
                listener.onSeek(-10000);
        }
    };
    private OnClickListener mPlayPauseListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (listener != null)
                listener.onPlayPause();
        }
    };
    private OnClickListener mForwardListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (listener != null)
                listener.onSeek(10000);
        }
    };

    @Override
    public void setState(boolean isPlaying) {
        if (isPlaying) {
            mPlayPause.setBackgroundResource(R.drawable.ic_pause);
        } else {
            mPlayPause.setBackgroundResource(R.drawable.ic_play);
        }
    }

    @Override
    public void setOnPlayerControlListener(OnPlayerControlListener listener) {
        this.listener = listener;
    }

    @Override
    public void setSeekable(boolean isSeekable) {
    }
}
