

package org.videolan.vlc.interfaces;

public interface OnPlayerControlListener {
    public abstract void onPlayPause();

    public abstract long onWheelStart();

    public abstract void onSeek(int delta);

    public abstract void onSeekTo(long position);

    public abstract void onShowInfo(String info);
}
