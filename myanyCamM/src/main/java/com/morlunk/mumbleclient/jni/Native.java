package com.morlunk.mumbleclient.jni;




public class Native {
	static {
		System.loadLibrary("NativeAudio");
	}


	public static class JitterBufferPacket {
		public byte[] data;
		public int len;
		public int timestamp;
		public int span;
		public short sequence;
		public int user_data;
	}



	public final static native void WebRtcAecm_Create();

	public final static native void WebRtcAecm_Free();

	public final static native void WebRtcAecm_Process(short[] nearendNoisy,
			short[] nearendClean, short[] out, int nrOfSamples,
			int msInSndCardBuf);

	public final static native void WebRtcAecm_Init(int sampFreq);

	public final static native void WebRtcAecm_BufferFarend(short[] farend,
			int nrOfSamples);
}
