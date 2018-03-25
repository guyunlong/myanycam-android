package com.android.webrtc.audio;


public class MobileAEC {
    static {
        System.loadLibrary("webrtc_aecm");
    }

   
   

    
    public static final short AECM_UNABLE = 0;

    
    public static final short AECM_ENABLE = 1;

   
   

    
    public static final class SamplingFrequency {
        public long getFS() {
            return mSamplingFrequency;
        }

        
        public static final SamplingFrequency FS_8000Hz  = new SamplingFrequency(
                                                                 8000);

        
        public static final SamplingFrequency FS_16000Hz = new SamplingFrequency(
                                                                 16000);

        private final long                    mSamplingFrequency;

        private SamplingFrequency(long fs) {
            this.mSamplingFrequency = fs;
        }
    }

    
    public static final class AggressiveMode {
        public int getMode() {
            return mMode;
        }

        
        public static final AggressiveMode MILD            = new AggressiveMode(
                                                                   0);

        
        public static final AggressiveMode MEDIUM          = new AggressiveMode(
                                                                   1);

        
        public static final AggressiveMode HIGH            = new AggressiveMode(
                                                                   2);

        
        public static final AggressiveMode AGGRESSIVE      = new AggressiveMode(
                                                                   3);

        
        public static final AggressiveMode MOST_AGGRESSIVE = new AggressiveMode(
                                                                   4);

        private final int                  mMode;

        private AggressiveMode(int mode) {
            mMode = mode;
        }
    }

   
   

    private int               mAecmHandler = -1;  
    private AecmConfig        mAecmConfig  = null;
    private SamplingFrequency mSampFreq    = null;
    private boolean           mIsInit      = false;

   
   

    
    public MobileAEC(SamplingFrequency sampFreqOfData) {
        setSampFreq(sampFreqOfData);
        mAecmConfig = new AecmConfig();

       
        mAecmHandler = nativeCreateAecmInstance();
    }

   
   

    
    public void setSampFreq(SamplingFrequency fs) {
        if (fs == null)
            mSampFreq = SamplingFrequency.FS_16000Hz;
        else
            mSampFreq = fs;
    }

    
    public MobileAEC farendBuffer(short[] farendBuf, int numOfSamples)
            throws Exception {
       
        if (!mIsInit) {
           
            throw new Exception(
                    "setFarendBuffer() called on an unprepared AECM instance.");
        }

        if (nativeBufferFarend(mAecmHandler, farendBuf, numOfSamples) == -1)
           
            throw new Exception(
                    "setFarendBuffer() failed due to invalid arguments.");

        return this;
    }

    
    public void echoCancellation(short[] nearendNoisy, short[] nearendClean,
            short[] out, short numOfSamples, short delay) throws Exception {
       
        if (!mIsInit) {
           
            throw new Exception(
                    "echoCancelling() called on an unprepared AECM instance.");
        }

        if (nativeAecmProcess(mAecmHandler, nearendNoisy, nearendClean, out,
                numOfSamples, delay) == -1)
           
            throw new Exception(
                    "echoCancellation() failed due to invalid arguments.");
    }

    
    public MobileAEC setAecmMode(AggressiveMode mode)
            throws NullPointerException {
       
        if (mode == null)
            throw new NullPointerException(
                    "setAecMode() failed due to null argument.");

        mAecmConfig.mAecmMode = (short) mode.getMode();
        return this;
    }

    
    public MobileAEC prepare() {
        if (mIsInit) {
            close();
            mAecmHandler = nativeCreateAecmInstance();
        }

        mInitAecmInstance((int) mSampFreq.getFS());
        mIsInit = true;

       
        nativeSetConfig(mAecmHandler, mAecmConfig);
        return this;
    }

    
    public void close() {
        if (mIsInit) {
            nativeFreeAecmInstance(mAecmHandler);
            mAecmHandler = -1;
            mIsInit = false;
        }
    }

   
   

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
       
        if (mIsInit) {
            close();
        }
    }

   
   

    
    private void mInitAecmInstance(int SampFreq) {
        if (!mIsInit) {
            nativeInitializeAecmInstance(mAecmHandler, SampFreq);

           
            mAecmConfig = new AecmConfig();

           
            nativeSetConfig(mAecmHandler, mAecmConfig);

            mIsInit = true;
        }
    }

   
   

    
    @SuppressWarnings("unused")
    public class AecmConfig {
        private short mAecmMode = (short) AggressiveMode.AGGRESSIVE.getMode();
        private short mCngMode  = AECM_ENABLE;                               
    }

   
   

    
    private static native int nativeCreateAecmInstance();

    
    private static native int nativeFreeAecmInstance(int aecmHandler);

    
    private static native int nativeInitializeAecmInstance(int aecmHandler,
            int samplingFrequency);

    
    private static native int nativeBufferFarend(int aecmHandler,
            short[] farend, int nrOfSamples);

    
    private static native int nativeAecmProcess(int aecmHandler,
            short[] nearendNoisy, short[] nearendClean, short[] out,
            short nrOfSamples, short msInSndCardBuf);

    
    private static native int nativeSetConfig(int aecmHandler,
            AecmConfig aecmConfig);
}
