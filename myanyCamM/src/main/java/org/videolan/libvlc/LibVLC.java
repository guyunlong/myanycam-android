

package org.videolan.libvlc;

import java.util.ArrayList;
import java.util.Map;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

public class LibVLC {
    private static final String TAG = "VLC/LibVLC";
    public static final int AOUT_AUDIOTRACK_JAVA = 0;
    public static final int AOUT_AUDIOTRACK = 1;
    public static final int AOUT_OPENSLES = 2;

    private static LibVLC sInstance;

    
    private long mLibVlcInstance = 0;
    
    private long mMediaListPlayerInstance = 0;
    private long mInternalMediaPlayerInstance = 0;
    
    private long mMediaListInstance = 0;

    
    private StringBuffer mDebugLogBuffer;
    private boolean mIsBufferingLog = false;

    private Aout mAout;

    
   

    
    private boolean iomx = false;
    private String subtitlesEncoding = "";
    private int aout = LibVlcUtil.isGingerbreadOrLater() ? AOUT_OPENSLES : AOUT_AUDIOTRACK_JAVA;
    private boolean timeStretching = false;
    private int deblocking = -1;
    private String chroma = "";
    private boolean verboseMode = true;

    
    private boolean mIsInitialized = false;
    public native void attachSurface(Surface surface, IVideoPlayer player, int width, int height);

    public native void detachSurface();

    
    static {
        try {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1)
                System.loadLibrary("iomx-gingerbread");
            else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB_MR2)
                System.loadLibrary("iomx-hc");
            else
                System.loadLibrary("iomx-ics");
        } catch (Throwable t) {
            Log.w(TAG, "Unable to load the iomx library: " + t);
        }
        try {
            System.loadLibrary("vlcjni");
        } catch (UnsatisfiedLinkError ule) {
            Log.e(TAG, "Can't load vlcjni library: " + ule);
           
            System.exit(1);
        } catch (SecurityException se) {
            Log.e(TAG, "Encountered a security issue when loading vlcjni library: " + se);
           
            System.exit(1);
        }
    }

    
    public static LibVLC getInstance() throws LibVlcException {
        synchronized (LibVLC.class) {
            if (sInstance == null) {
                
                sInstance = new LibVLC();
            }
        }

        return sInstance;
    }

    
    public static LibVLC getExistingInstance() {
        synchronized (LibVLC.class) {
            return sInstance;
        }
    }

    
    private LibVLC() {
        mAout = new Aout();
    }

    
    @Override
    public void finalize() {
        if (mLibVlcInstance != 0) {
            Log.d(TAG, "LibVLC is was destroyed yet before finalize()");
            destroy();
        }
    }

    
    public native void setSurface(Surface f);

    public static synchronized void restart(Context context) {
        if (sInstance != null) {
            try {
                sInstance.destroy();
                sInstance.init(context);
            } catch (LibVlcException lve) {
                Log.e(TAG, "Unable to reinit libvlc: " + lve);
            }
        }
    }

    

    public boolean useIOMX() {
        return iomx;
    }

    public void setIomx(boolean iomx) {
        this.iomx = iomx;
    }

    public String getSubtitlesEncoding() {
        return subtitlesEncoding;
    }

    public void setSubtitlesEncoding(String subtitlesEncoding) {
        this.subtitlesEncoding = subtitlesEncoding;
    }

    public int getAout() {
        return aout;
    }

    public void setAout(int aout) {
        if (aout < 0)
            this.aout = LibVlcUtil.isGingerbreadOrLater() ? AOUT_OPENSLES : AOUT_AUDIOTRACK_JAVA;
        else
            this.aout = aout;
    }

    public boolean timeStretchingEnabled() {
        return timeStretching;
    }

    public void setTimeStretching(boolean timeStretching) {
        this.timeStretching = timeStretching;
    }

    public int getDeblocking() {
        int ret = deblocking;
        if(deblocking < 0) {
            
            LibVlcUtil.MachineSpecs m = LibVlcUtil.getMachineSpecs();
            if( (m.hasArmV6 && !(m.hasArmV7)) || m.hasMips )
                ret = 4;
            else if(m.bogoMIPS > 1200 && m.processors > 2)
                ret = 1;
            else
                ret = 3;
        } else if(deblocking > 4) {
            ret = 3;
        }
        return ret;
    }

    public void setDeblocking(int deblocking) {
        this.deblocking = deblocking;
    }

    public String getChroma() {
        return chroma;
    }

    public void setChroma(String chroma) {
        this.chroma = chroma.equals("YV12") && !LibVlcUtil.isGingerbreadOrLater() ? "" : chroma;
    }

    public boolean isVerboseMode() {
        return verboseMode;
    }

    public void setVerboseMode(boolean verboseMode) {
        this.verboseMode = verboseMode;
    }

    
    public void init(Context context) throws LibVlcException {
        Log.v(TAG, "Initializing LibVLC");
        mDebugLogBuffer = new StringBuffer();
        if (!mIsInitialized) {
            if(!LibVlcUtil.hasCompatibleCPU(context)) {
                Log.e(TAG, LibVlcUtil.getErrorMsg());
                throw new LibVlcException();
            }
            nativeInit();
            setEventHandler(EventHandler.getInstance());
            mIsInitialized = true;
        }
    }

    
    public void destroy() {
        Log.v(TAG, "Destroying LibVLC instance");
        nativeDestroy();
        detachEventHandler();
        mIsInitialized = false;
    }

    
    public void initAout(int sampleRateInHz, int channels, int samples) {
        Log.d(TAG, "Opening the java audio output");
        mAout.init(sampleRateInHz, channels, samples);
    }

    
    public void playAudio(byte[] audioData, int bufferSize) {
        mAout.playBuffer(audioData, bufferSize);
    }

    
    public void pauseAout() {
        Log.d(TAG, "Pausing the java audio output");
        mAout.pause();
    }

    
    public void closeAout() {
        Log.d(TAG, "Closing the java audio output");
        mAout.release();
    }

    public void readMedia(String mrl) {
        readMedia(mLibVlcInstance, mrl, false);
    }

    
    public int readMedia(String mrl, boolean novideo) {
        Log.v(TAG, "Reading " + mrl);
        return readMedia(mLibVlcInstance, mrl, novideo);
    }

    
    public void playIndex(int position) {
        playIndex(mLibVlcInstance, position);
    }

    public String[] readMediaMeta(String mrl) {
        return readMediaMeta(mLibVlcInstance, mrl);
    }

    public TrackInfo[] readTracksInfo(String mrl) {
        return readTracksInfo(mLibVlcInstance, mrl);
    }

    
    public byte[] getThumbnail(String mrl, int i_width, int i_height) {
        return getThumbnail(mLibVlcInstance, mrl, i_width, i_height);
    }

    
    public boolean hasVideoTrack(String mrl) throws java.io.IOException {
        return hasVideoTrack(mLibVlcInstance, mrl);
    }

    
    public long getLengthFromLocation(String mrl) {
        return getLengthFromLocation(mLibVlcInstance, mrl);
    }

    
    public native void setRate(float rate);

    
    public native float getRate();

    
    private native void nativeInit() throws LibVlcException;

    
    private native void nativeDestroy();

    
    public native void startDebugBuffer();
    public native void stopDebugBuffer();
    public String getBufferContent() {
        return mDebugLogBuffer.toString();
    }

    public void clearBuffer() {
        mDebugLogBuffer.setLength(0);
    }

    public boolean isDebugBuffering() {
        return mIsBufferingLog;
    }

    
    public void removeIndex(int position) {
        removeIndex(mMediaListInstance, position);
    }
    private native void removeIndex(long media_list_instance, int position);

    
    private native int readMedia(long instance, String mrl, boolean novideo);

    
    private native void playIndex(long instance, int position);

    
    public native boolean hasMediaPlayer();

    
    public native boolean isPlaying();

    
    public native boolean isSeekable();

    
    public native void play();

    
    public native void pause();

    
    public native void stop();

    
    public native void previous();

    
    public native void next();

    
    public native int getVolume();

    
    public native int setVolume(int volume);

    
    public native long getTime();

    
    public native long setTime(long time);

    
    public native float getPosition();

    
    public native void setPosition(float pos);

    
    public native long getLength();

    
    public native String version();

    
    public native String compiler();

    
    public native String changeset();

    
    private native byte[] getThumbnail(long instance, String mrl, int i_width, int i_height);

    
    private native boolean hasVideoTrack(long instance, String mrl);

    private native String[] readMediaMeta(long instance, String mrl);

    private native TrackInfo[] readTracksInfo(long instance, String mrl);

    public native TrackInfo[] readTracksInfoPosition(int position);

    public native int getAudioTracksCount();

    public native Map<Integer,String> getAudioTrackDescription();

    public native int getAudioTrack();

    public native int setAudioTrack(int index);

    public native int getVideoTracksCount();

    public native int addSubtitleTrack(String path);

    public native Map<Integer,String> getSpuTrackDescription();

    public native int getSpuTrack();

    public native int setSpuTrack(int index);

    public native int getSpuTracksCount();

    public static native String nativeToURI(String path);

    public static String PathToURI(String path) {
        if(path == null) {
            throw new NullPointerException("Cannot convert null path!");
        }
        return LibVLC.nativeToURI(path);
    }

    public static native void nativeReadDirectory(String path, ArrayList<String> res);

    public native static boolean nativeIsPathDirectory(String path);

    
    public native void getMediaListItems(ArrayList<String> arl);

    
    private native long getLengthFromLocation(long instance, String mrl);

    private native void setEventHandler(EventHandler eventHandler);

    private native void detachEventHandler();
}
