package gyl.cam;

import android.os.Handler;
import android.util.Log;

import com.myanycam.bean.VideoData;
import com.myanycamm.cam.AppServer;
import com.myanycamm.utils.ELog;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static com.thSDK.lib.jvideo_decode_frame;
import static com.thSDK.lib.jvideo_decode_init;

public class recThread extends Thread {
	private static String TAG = "recThread";
	String ip;//
	int port;
	// CloudLivingView root;
	Socket socket = null;
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	DataOutputStream outbuf;
	Boolean _quit;

	int ptr;

	int recType = -1;

	private Handler mHandler;

	int noDataCnt;//

	boolean _isFir = true;
	int width;
	int height;
	int type;
	int parseTime;// 接一帧视频的时间

	byte[] rec;//
	int total;



	public recThread(Handler mHandler) {
		// root = parent;
		this.mHandler = mHandler;
	}

	public void init() {

		ELog.i("gyl", "解析初始化");
		_isFir = true;

		ptr = 0;
		_quit = false;
		noDataCnt = 0;
		// conToSer(ip, port);

	}

	@Override
	public void run() {

		int i = 0;
		AppServer.isDisplayVideo = true;
		while (AppServer.isDisplayVideo) {
			if (null == VideoData.Videolist || VideoData.Videolist.size() == 0) {
				//
				if (VideoData.audioArraryList.isEmpty()) {

				}

			} else {
				try {

					byte[] videoData = null;
					synchronized ( VideoData.Videolist)
					{
						videoData = VideoData.Videolist.get(0).getVideoData();
						VideoData.Videolist.remove(0);
						if (VideoData.Videolist.size() > 100) {
							VideoData.Videolist.clear();
						}
					}
					if (videoData != null){
						parse(videoData,videoData.length);
					}


				} catch (NullPointerException e) {
					ELog.i(TAG, "解码时空指针异常");
					//VideoData.Videolist.remove(0);
					continue;
				} catch (IndexOutOfBoundsException e) {
					//
				}

			}

		}
	}



	void parse(byte []buf,int total) {
		// Log.e("gyl", "parse");

		//

		if (buf[6] != recType) {
			recType = buf[6];
			ELog.i("gyl", "_isFir" + _isFir);
			_isFir = false;
			if (buf[1] == 0x00) {
				Log.e("gyl", "mpeg code");
				type = 0;
			}
			if (buf[1] == 0x01) {
				Log.e("gyl", "h264 code");
				type = 1;
			}
			if (buf[6] == 0x00) {

				width = 160;
				height = 120;
			}
			if (buf[6] == 0x01) {
				width = 176;
				height = 144;
			}
			if (buf[6] == 0x02) {
				width = 320;
				height = 240;
			}
			if (buf[6] == 0x03) {
				width = 352;
				height = 288;
			}
			if (buf[6] == 0x04) {
				width = 640;
				height = 480;
			}
			if (buf[6] == 0x05) {
				width = 720;
				height = 480;
			}
			if (buf[6] == 0x06) {
				width = 1280;
				height = 720;
			}
			if (buf[6] == 0x07) {
				width = 1920;
				height = 1080;
			}
			if (buf[6] == 0x08) {
				width = 640;
				height = 360;
			}
			if (buf[6] == 0x09) {
				width = 320;
				height = 180;
			}
			ELog.i(TAG, "width:" + width + "height:" + height);
			jvideo_decode_init(type, width, height);
		}

		byte[] bmp = new byte[total - 7];
		for (int i = 0; i < total - 7; i++) {
			bmp[i] = buf[i + 7];
		}
		//ByteBuffer subbuf = ByteBuffer.wrap(buf,7,total-7);
		long parseTimeBefore = System.currentTimeMillis();
		ELog.i(TAG, "解码---------0" );

			int n = jvideo_decode_frame( bmp, total - 7);
			ELog.e(TAG, "解码n:" + n+"thead id is "+Thread.currentThread().getId());


		//
		// FileUtils.saveByteToFile(yuv,
		// Environment.getExternalStorageDirectory().getPath()+"/my.yuv");
		// VideoData.yuvArrayList.add(yuv);
		long parseTimeAfter = System.currentTimeMillis();
		parseTime = (int) (parseTimeAfter - parseTimeBefore);
		//


	}


	int getSize() {
		int availbytes = 0;

		try {
			availbytes = socket.getInputStream().available();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return availbytes;
	}

	void req() throws IOException {
		try {
			outbuf = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// header
		byte[] req = new byte[8];

		req[0] = 0x00;
		req[1] = 0x00;
		req[2] = 0x00;
		req[3] = 0x00;
		req[4] = 0x00;
		req[5] = 0x00;
		req[6] = 0x00;
		req[7] = 0x00;
		outbuf.write(req);
	}

	public void parseByteData(byte[] videoData) {
		//
		if (videoData == null) {
			return;
		}
		this.total = videoData.length;
		this.rec = new byte[total];
		this.rec = videoData;


		parse(videoData,videoData.length);
	}



	public int Byte2Int(byte[] b) {
		int bb;
		bb = (b[0] & 0x000000FF) << 24 | (b[1] & 0x000000FF) << 16
				| (b[2] & 0x000000FF) << 8 | (b[3] & 0x000000FF);
		return bb;
	}

}
