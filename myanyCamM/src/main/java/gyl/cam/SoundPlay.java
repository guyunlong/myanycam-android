package gyl.cam;

import java.io.File;
import java.io.FileOutputStream;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.morlunk.mumbleclient.jni.Native;
import com.myanycam.bean.VideoData;
import com.myanycamm.process.AdPcm;
import com.myanycamm.utils.ELog;
import com.spoledge.aacdecoder.AACPlayer;

public class SoundPlay extends Thread {
	private static String TAG = "SoundPlay";
	protected AudioTrack m_out_trk;
	protected int m_out_buf_size;
	protected byte[] m_out_bytes;
	public static boolean is_keep_running;
	public static boolean isAdpcm;
	private AACPlayer aacPlayer;
	public static short[] outShort;
	public static CircularByteBuffer cbb = new CircularByteBuffer();
	public SoundPlay() {
		init();
	}

	public void init() {
		// s = new Socket("192.168.1.100", 4331);
		// din = new DataInputStream(s.getInputStream());

		is_keep_running = true;

		// m_out_bytes = new byte[m_out_buf_size];

		// new Thread(R1).start();

	}

	public void free() {
		is_keep_running = false;
		ELog.i(TAG, "停止了音频解析...");
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
			e.printStackTrace();
			// Log.d(“sleep exceptions…\n”,”")
		}
	}

	public void run() {
		while (VideoData.audioArraryList.size()==0) {
			while (VideoData.audioArraryList.size()<6) {//每次到0时就积累6包
				
			}
		}
		//不为空才解析
		if (isAdpcm) {
			 adpcmPlay();	
		}else{
			aacPlay();
		}	
		
	}

	private void adpcmPlay() {
		
		m_out_buf_size = AudioTrack.getMinBufferSize(8000,
				AudioFormat.CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_16BIT);

		m_out_trk = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
				AudioFormat.CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_16BIT, m_out_buf_size,
				AudioTrack.MODE_STREAM);
		byte[] bytes_pkg = null;
		m_out_trk.play();
		 Native.WebRtcAecm_Create();
         Native.WebRtcAecm_Init(8000);  
	
		while (is_keep_running) {
			while (VideoData.audioArraryList.size()==0) {
				while (VideoData.audioArraryList.size()<6) {//每次到0时就积累6包
					
				}
			}
			if (!VideoData.audioArraryList.isEmpty()) {
				if ( VideoData.audioArraryList.get(0) != null) {
					m_out_bytes = VideoData.audioArraryList.get(0);
					bytes_pkg = new byte[m_out_bytes.length - 7];
					System.arraycopy(m_out_bytes, 7, bytes_pkg, 0, bytes_pkg.length);
					//
					// int outp = bytes_pkg.length* 4;
					// byte[] output = new byte[outp];
					ELog.i(TAG, "音频长度:" + VideoData.audioArraryList.size());
					outShort = new short[bytes_pkg.length * 2];
					ELog.i(TAG, "outshort.."+outShort.length);
					outShort = AdPcm.decodeMy(bytes_pkg, bytes_pkg.length,
							outShort, 0);

//					for (int i = 0; i < outShort.length; i++) {
//						outShort[i] = (short) (outShort[i] - CloudLivingView.tmpBuf[i]);
//					}
//					if (CloudLivingView.isRecording) {
//					
////						outShort = mSpeex.process(CloudLivingView.tmpBuf, outShort);
//						ELog.i(TAG, "出来的声音:"+outShort.length);
//					}
				
					// short[] pcmByte = AdPcm.decode(bytes_pkg);
					//
					// "解压之前:"+bytes_pkg.length+"   解压adpcm:"+outShort.length);
					// bytes_pkg = m_out_bytes.clone();
					m_out_trk.write(outShort, 0, outShort.length);
					m_out_trk.flush();
				}			
				try {
					VideoData.audioArraryList.remove(0);
					// if (VideoData.audioArraryList.size()>4) {
					// VideoData.audioArraryList.clear();
					// }
				} catch (Exception e) {
					ELog.i(TAG, "移除出错...");
					e.printStackTrace();
				}

				//
			}
			

		}
		ELog.i(TAG, "出了循环..");

		m_out_trk.stop();
		m_out_trk = null;

	}

	private void aacPlay() {
		WriteAudio t1 = new WriteAudio();	

		ELog.i(TAG, "aac在解析" + is_keep_running);
		if (aacPlayer != null) {
			aacPlayer.stop();
			aacPlayer = null;
		}
		aacPlayer = new AACPlayer();
	
		// InputStream stream =
		// ScreenManager.getScreenManager().currentActivity()
		// .getResources().openRawResource(R.raw.aac235);
		// aacPlayer = new AACPlayer();
		// aacPlayer.playAsync(stream, AACPlayer.Quality.LOW_32);
//			out = new PipedOutputStream(aacPlayer.mPipedInputStream);
			t1.start();

		aacPlayer.playMyanyCam();
	}

	public class WriteAudio extends Thread {
		byte[] bytes_pkg = null;
		// 这是一个线程类，所以它应该覆盖Thread的run方法,run方法在线程类启动时自动运行
		public void run() {
			File temp;
			FileOutputStream outF = null;
//			try {
//				temp = File.createTempFile("aac", "temp");
//				outF = new FileOutputStream(temp);
//			} catch (IOException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}

			while (is_keep_running) {
				if (!VideoData.audioArraryList.isEmpty()
						&& (VideoData.audioArraryList.get(0) != null)) {
					m_out_bytes = VideoData.audioArraryList.get(0);
					bytes_pkg = new byte[m_out_bytes.length - 7];
					System.arraycopy(m_out_bytes, 7, bytes_pkg, 0,
							bytes_pkg.length);
					// aacPlayer.mArrayList.add(new
					// ByteArrayInputStream(bytes_pkg));
					try {
//						outF.write(bytes_pkg);
						// 空余足够多,才写
						ELog.i(TAG, "audioArraryList:"+VideoData.audioArraryList.size());
						cbb.getOutputStream().write(bytes_pkg);
//						bOut.write(bytes_pkg);
//						out.flush();
						VideoData.audioArraryList.remove(0);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			// try {
			// out.close();
			// aacPlayer.mPipedInputStream.close();
			// } catch (IOException e) {
			//
			// e.printStackTrace();
			// }

		}

	}

}
