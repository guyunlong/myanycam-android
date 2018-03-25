

package org.videolan.vlc.interfaces;


public interface IPlayerControl {
    void setState(boolean isPlaying);

    void setSeekable(boolean isSeekable);

    void setOnPlayerControlListener(OnPlayerControlListener listener);
}
