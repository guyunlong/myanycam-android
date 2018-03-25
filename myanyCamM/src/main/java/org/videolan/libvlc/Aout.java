

package org.videolan.libvlc;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class Aout {
    

    public Aout() {
    }

    private AudioTrack mAudioTrack;
    private static final String TAG = "LibVLC/aout";

    public void init(int sampleRateInHz, int channels, int samples) {
        Log.d(TAG, sampleRateInHz + ", " + channels + ", " + samples + "=>" + channels * samples);
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRateInHz,
                                                        AudioFormat.CHANNEL_OUT_STEREO,
                                                        AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                                     sampleRateInHz,
                                     AudioFormat.CHANNEL_OUT_STEREO,
                                     AudioFormat.ENCODING_PCM_16BIT,
                                     Math.max(minBufferSize, channels * samples * 2),
                                     AudioTrack.MODE_STREAM);
    }

    public void release() {
        if (mAudioTrack != null) {
            mAudioTrack.release();
        }
        mAudioTrack = null;
    }

    public void playBuffer(byte[] audioData, int bufferSize) {
        if (mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED)
            return;
        if (mAudioTrack.write(audioData, 0, bufferSize) != bufferSize) {
            Log.w(TAG, "Could not write all the samples to the audio device");
        }
        mAudioTrack.play();
    }

    public void pause() {
        mAudioTrack.pause();
    }
}
