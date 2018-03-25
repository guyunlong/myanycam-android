

package org.videolan.vlc;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.vlc.interfaces.IAudioService;
import org.videolan.vlc.interfaces.IAudioServiceCallback;

import com.myanycam.net.SocketFunction;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteControlClient.MetadataEditor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class AudioService extends Service {

	private static final String TAG = "AudioService";

	private static final int SHOW_PROGRESS = 0;
	private static final int SHOW_TOAST = 1;

	public static final String ACTION_REMOTE_GENERIC = "org.videolan.vlc.remote.";
	public static final String ACTION_REMOTE_PLAY = "org.videolan.vlc.remote.Play";
	public static final String ACTION_REMOTE_PLAYPAUSE = "org.videolan.vlc.remote.PlayPause";
	public static final String ACTION_REMOTE_PAUSE = "org.videolan.vlc.remote.Pause";
	public static final String ACTION_REMOTE_STOP = "org.videolan.vlc.remote.Stop";

	private LibVLC mLibVLC;
	private ArrayList<Media> mMediaList;
	private Stack<Media> mPrevious;
	private Media mCurrentMedia;
	private HashMap<IAudioServiceCallback, Integer> mCallback;
	private EventHandler mEventHandler;
	private boolean mShuffling = false;
	private RepeatType mRepeating = RepeatType.None;
	private OnAudioFocusChangeListener audioFocusListener;
	private ComponentName mRemoteControlClientReceiverComponent;
	private PowerManager.WakeLock mWakeLock;

	
	private RemoteControlClient mRemoteControlClient = null;
	private RemoteControlClientReceiver mRemoteControlClientReceiver = null;

	
	private boolean mLibVLCPlaylistActive = false;

	
	private long mWidgetPositionTimestamp = Calendar.getInstance()
			.getTimeInMillis();

	@Override
	public void onCreate() {
		super.onCreate();

		// Get libVLC instance
		try {
			mLibVLC = Util.getLibVlcInstance();
		} catch (LibVlcException e) {
			e.printStackTrace();
		}

		mCallback = new HashMap<IAudioServiceCallback, Integer>();
		mMediaList = new ArrayList<Media>();
		mPrevious = new Stack<Media>();
		mEventHandler = EventHandler.getInstance();
		mRemoteControlClientReceiverComponent = new ComponentName(
				getPackageName(), RemoteControlClientReceiver.class.getName());

		// Make sure the audio player will acquire a wake-lock while playing. If
		// we don't do
		// that, the CPU might go to sleep while the song is playing, causing
		// playback to stop.
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

		IntentFilter filter = new IntentFilter();
		filter.setPriority(Integer.MAX_VALUE);
		filter.addAction(ACTION_REMOTE_PLAYPAUSE);
		filter.addAction(ACTION_REMOTE_PLAY);
		filter.addAction(ACTION_REMOTE_PAUSE);
		filter.addAction(ACTION_REMOTE_STOP);
		filter.addAction(Intent.ACTION_HEADSET_PLUG);
		filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
		filter.addAction(SocketFunction.SLEEP_INTENT);
		registerReceiver(serviceReceiver, filter);

		boolean stealRemoteControl = false;

		if (!Util.isFroyoOrLater() || stealRemoteControl) {
			
			filter = new IntentFilter();
			if (stealRemoteControl)
				filter.setPriority(Integer.MAX_VALUE);
			filter.addAction(Intent.ACTION_MEDIA_BUTTON);
			mRemoteControlClientReceiver = new RemoteControlClientReceiver();
			registerReceiver(mRemoteControlClientReceiver, filter);
		}

	}

	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public void setUpRemoteControlClient() {
		Context context = SocketFunction.getAppContext();
		AudioManager audioManager = (AudioManager) context
				.getSystemService(AUDIO_SERVICE);

		if (Util.isICSOrLater()) {
			audioManager
					.registerMediaButtonEventReceiver(mRemoteControlClientReceiverComponent);

			if (mRemoteControlClient == null) {
				Intent mediaButtonIntent = new Intent(
						Intent.ACTION_MEDIA_BUTTON);
				mediaButtonIntent
						.setComponent(mRemoteControlClientReceiverComponent);
				PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(
						context, 0, mediaButtonIntent, 0);

				// create and register the remote control client
				mRemoteControlClient = new RemoteControlClient(
						mediaPendingIntent);
				audioManager.registerRemoteControlClient(mRemoteControlClient);
			}

			mRemoteControlClient
					.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY
							| RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
							| RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
							| RemoteControlClient.FLAG_KEY_MEDIA_NEXT
							| RemoteControlClient.FLAG_KEY_MEDIA_STOP);
		} else if (Util.isFroyoOrLater()) {
			audioManager
					.registerMediaButtonEventReceiver(mRemoteControlClientReceiverComponent);
		}
	}

	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void setRemoteControlClientPlaybackState(int state) {
		if (!Util.isICSOrLater() || mRemoteControlClient == null)
			return;

		switch (state) {
		case EventHandler.MediaPlayerPlaying:
			mRemoteControlClient
					.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
			break;
		case EventHandler.MediaPlayerPaused:
			mRemoteControlClient
					.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
			break;
		case EventHandler.MediaPlayerStopped:

			mRemoteControlClient
					.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
			break;
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stop();
		if (mWakeLock.isHeld())
			mWakeLock.release();
		unregisterReceiver(serviceReceiver);
		if (mRemoteControlClientReceiver != null) {
			unregisterReceiver(mRemoteControlClientReceiver);
			mRemoteControlClientReceiver = null;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mInterface;
	}

	@TargetApi(Build.VERSION_CODES.FROYO)
	private void changeAudioFocus(boolean gain) {
		if (!Util.isFroyoOrLater())
			return;

		audioFocusListener = new OnAudioFocusChangeListener() {
			@Override
			public void onAudioFocusChange(int focusChange) {
				if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
						|| focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
					
					LibVLC.getExistingInstance().setVolume(36);
				} else {
					LibVLC.getExistingInstance().setVolume(100);
				}
			}
		};

		AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
		if (gain)
			am.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC,
					AudioManager.AUDIOFOCUS_GAIN);
		else
			am.abandonAudioFocus(audioFocusListener);

	}

	private final BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			int state = intent.getIntExtra("state", 0);
			if (mLibVLC == null) {
				Log.w(TAG, "Intent received, but VLC is not loaded, skipping.");
				return;
			}

			if (action.equalsIgnoreCase(ACTION_REMOTE_PLAYPAUSE)) {
				if (mLibVLC.isPlaying() && mCurrentMedia != null)
					pause();
				else if (!mLibVLC.isPlaying() && mCurrentMedia != null)
					play();
			} else if (action.equalsIgnoreCase(ACTION_REMOTE_PLAY)) {
				if (!mLibVLC.isPlaying() && mCurrentMedia != null)
					play();
			} else if (action.equalsIgnoreCase(ACTION_REMOTE_PAUSE)) {
				if (mLibVLC.isPlaying() && mCurrentMedia != null)
					pause();
			} else if (action.equalsIgnoreCase(ACTION_REMOTE_STOP)) {
				stop();
			}

			
			if (action.equalsIgnoreCase(SocketFunction.SLEEP_INTENT)) {
				stop();
			}
		}
	};

	
	private final Handler mVlcEventHandler = new AudioServiceEventHandler(this);

	private static class AudioServiceEventHandler extends
			WeakHandler<AudioService> {
		public AudioServiceEventHandler(AudioService fragment) {
			super(fragment);
		}

		@Override
		public void handleMessage(Message msg) {
			AudioService service = getOwner();
			if (service == null)
				return;

			switch (msg.getData().getInt("event")) {
			case EventHandler.MediaPlayerPlaying:
				Log.i(TAG, "MediaPlayerPlaying");
				service.executeUpdate();

				if (service.mCurrentMedia == null)
					return;
				String location = service.mCurrentMedia.getLocation();
				long length = service.mLibVLC.getLength();
				MediaDatabase dbManager = MediaDatabase
						.getInstance(SocketFunction.getAppContext());
				Media m = dbManager.getMedia(SocketFunction.getAppContext(),
						location);
				
				if (m != null && m.getLength() == 0 && length > 0) {
					Log.d(TAG, "Updating audio file length");
					dbManager.updateMedia(location,
							MediaDatabase.mediaColumn.MEDIA_LENGTH, length);
				}

				service.changeAudioFocus(true);
				service.setRemoteControlClientPlaybackState(EventHandler.MediaPlayerPlaying);
				if (!service.mWakeLock.isHeld())
					service.mWakeLock.acquire();
				break;
			case EventHandler.MediaPlayerPaused:
				Log.i(TAG, "MediaPlayerPaused");
				service.executeUpdate();
				service.setRemoteControlClientPlaybackState(EventHandler.MediaPlayerPaused);
				if (service.mWakeLock.isHeld())
					service.mWakeLock.release();
				break;
			case EventHandler.MediaPlayerStopped:
				// 停止调用
				Log.i(TAG, "MediaPlayerStopped");
				service.executeUpdate();
				service.setRemoteControlClientPlaybackState(EventHandler.MediaPlayerStopped);
				if (service.mWakeLock.isHeld())
					service.mWakeLock.release();
				break;
			case EventHandler.MediaPlayerEndReached:
				Log.i(TAG, "MediaPlayerEndReached");
				service.executeUpdate();
				if (service.mWakeLock.isHeld())
					service.mWakeLock.release();
				break;
			case EventHandler.MediaPlayerVout:
				if (msg.getData().getInt("data") > 0) {
					service.handleVout();
				}
				break;
			case EventHandler.MediaPlayerPositionChanged:
				float pos = msg.getData().getFloat("data");
				break;
			case EventHandler.MediaPlayerEncounteredError:
				if (service.mCurrentMedia != null) {

				}
				service.executeUpdate();
				if (service.mWakeLock.isHeld())
					service.mWakeLock.release();
				break;
			default:
				Log.e(TAG, "Event not handled");
				break;
			}
		}
	};

	private void handleVout() {		
		Log.i(TAG, "Obtained video track");
		mMediaList.clear();
		hideNotification();

		// Don't crash if user stopped the media
		if (mCurrentMedia == null)
			return;

		// Switch to the video player & don't lose the currently playing stream
		// VideoPlayerActivity.start(SocketFunction.getAppContext(),
		// mCurrentMedia.getLocation(), mCurrentMedia.getTitle(), true);
	}

	private void executeUpdate() {
		executeUpdate(true);
	}

	private void executeUpdate(Boolean updateWidget) {
		for (IAudioServiceCallback callback : mCallback.keySet()) {
			try {
				callback.update();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	private final Handler mHandler = new AudioServiceHandler(this);

	private static class AudioServiceHandler extends WeakHandler<AudioService> {
		public AudioServiceHandler(AudioService fragment) {
			super(fragment);
		}

		@Override
		public void handleMessage(Message msg) {
			AudioService service = getOwner();
			if (service == null)
				return;

			switch (msg.what) {
			case SHOW_PROGRESS:
				if (service.mCallback.size() > 0) {
					removeMessages(SHOW_PROGRESS);
					service.executeUpdate(false);
					sendEmptyMessageDelayed(SHOW_PROGRESS, 1000);
				}
				break;
			case SHOW_TOAST:
				final Bundle bundle = msg.getData();
				final String text = bundle.getString("text");
				final int duration = bundle.getInt("duration");
				Toast.makeText(SocketFunction.getAppContext(), text, duration)
						.show();
				break;
			}
		}
	};

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void hideNotification() {
		stopForeground(true);
		stopSelf();
	}

	private void pause() {
		setUpRemoteControlClient();
		mHandler.removeMessages(SHOW_PROGRESS);
		// hideNotification(); <-- see event handler
		mLibVLC.pause();
	}

	private void play() {
		if (mCurrentMedia != null) {
			setUpRemoteControlClient();
			mLibVLC.play();
			mHandler.sendEmptyMessage(SHOW_PROGRESS);
		}
	}

	private void stop() {
		mLibVLC.stop();
		mEventHandler.removeHandler(mVlcEventHandler);
		setRemoteControlClientPlaybackState(EventHandler.MediaPlayerStopped);
		mCurrentMedia = null;
		mMediaList.clear();
		mPrevious.clear();
		mHandler.removeMessages(SHOW_PROGRESS);
		hideNotification();
		executeUpdate();
		changeAudioFocus(false);
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void updateRemoteControlClientMetadata() {
		if (!Util.isICSOrLater())
			return;

		if (mRemoteControlClient != null) {
			MetadataEditor editor = mRemoteControlClient.editMetadata(true);
			editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE,
					mCurrentMedia.getTitle());
			editor.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
					mCurrentMedia.getLength());
			editor.apply();
		}
	}

	private void shuffle() {
		if (mShuffling)
			mPrevious.clear();
		mShuffling = !mShuffling;
	}

	private void setRepeatType(int t) {
		mRepeating = RepeatType.values()[t];
	}

	private final IAudioService.Stub mInterface = new IAudioService.Stub() {

		@Override
		public String getCurrentMediaLocation() throws RemoteException {
			return mCurrentMedia.getLocation();
		}

		@Override
		public void pause() throws RemoteException {
			AudioService.this.pause();
		}

		@Override
		public void play() throws RemoteException {
			AudioService.this.play();
		}

		@Override
		public void stop() throws RemoteException {
			AudioService.this.stop();
		}

		@Override
		public boolean isPlaying() throws RemoteException {
			return mLibVLC.isPlaying();
		}

		@Override
		public boolean isShuffling() {
			return mShuffling;
		}

		@Override
		public int getRepeatType() {
			return mRepeating.ordinal();
		}

		@Override
		public boolean hasMedia() throws RemoteException {
			return mMediaList.size() != 0;
		}

		@Override
		public String getTitle() throws RemoteException {
			if (mCurrentMedia != null)
				return mCurrentMedia.getTitle();
			else
				return null;
		}

		@Override
		public synchronized void addAudioCallback(IAudioServiceCallback cb)
				throws RemoteException {
			Integer count = mCallback.get(cb);
			if (count == null)
				count = 0;
			mCallback.put(cb, count + 1);
			mHandler.sendEmptyMessage(SHOW_PROGRESS);
		}

		@Override
		public synchronized void removeAudioCallback(IAudioServiceCallback cb)
				throws RemoteException {
			Integer count = mCallback.get(cb);
			if (count == null)
				count = 0;
			if (count > 1)
				mCallback.put(cb, count - 1);
			else
				mCallback.remove(cb);
		}

		@Override
		public int getTime() throws RemoteException {
			return (int) mLibVLC.getTime();
		}

		@Override
		public int getLength() throws RemoteException {
			return (int) mLibVLC.getLength();
		}

		@Override
		public void load(List<String> mediaPathList, int position,
				boolean libvlcBacked, boolean noVideo) throws RemoteException {
			mLibVLCPlaylistActive = libvlcBacked;

			Log.v(TAG, "Loading position " + ((Integer) position).toString()
					+ " in " + mediaPathList.toString());
			mEventHandler.addHandler(mVlcEventHandler);

			mMediaList.clear();
			mPrevious.clear();

			if (mLibVLCPlaylistActive) {
				for (int i = 0; i < mediaPathList.size(); i++)
					mMediaList.add(new Media(mediaPathList.get(i), i));
			} else {
				MediaDatabase db = MediaDatabase.getInstance(AudioService.this);
				for (int i = 0; i < mediaPathList.size(); i++) {
					String location = mediaPathList.get(i);
					Media media = db.getMedia(AudioService.this, location);
					if (media == null) {

						Log.v(TAG, "Creating on-the-fly Media object for "
								+ location);
						media = new Media(location, false);
					}
					mMediaList.add(media);
				}
			}

			if (mMediaList.size() > position) {
				mCurrentMedia = mMediaList.get(position);
			}

			if (mCurrentMedia != null) {
				if (mLibVLCPlaylistActive) {
					mLibVLC.playIndex(position);
				} else {
					mLibVLC.readMedia(mCurrentMedia.getLocation(), noVideo);
				}
				setUpRemoteControlClient();
				updateRemoteControlClientMetadata();
			}

		}

		@Override
		public void append(List<String> mediaLocationList)
				throws RemoteException {
			if (mMediaList.size() == 0) {
				load(mediaLocationList, 0, false, false);
				return;
			}

			if (mLibVLCPlaylistActive) {
				return;
			}
			MediaDatabase db = MediaDatabase.getInstance(AudioService.this);
			for (int i = 0; i < mediaLocationList.size(); i++) {
				String location = mediaLocationList.get(i);
				Media media = db.getMedia(AudioService.this, location);
				if (media == null) {

					Log.v(TAG, "Creating on-the-fly Media object for "
							+ location);
					media = new Media(location, false);
				}
				mMediaList.add(media);
			}
		}

		@Override
		public List<String> getItems() {
			ArrayList<String> medias = new ArrayList<String>();
			for (int i = 0; i < mMediaList.size(); i++) {
				Media item = mMediaList.get(i);
				medias.add(item.getLocation());
			}
			return medias;
		}

		@Override
		public String getItem() {
			return mCurrentMedia != null ? mCurrentMedia.getLocation() : null;
		}

		@Override
		public void next() throws RemoteException {
		}

		@Override
		public void previous() throws RemoteException {
		}

		@Override
		public void shuffle() throws RemoteException {
			AudioService.this.shuffle();
		}

		@Override
		public void setRepeatType(int t) throws RemoteException {
			AudioService.this.setRepeatType(t);
		}

		@Override
		public void setTime(long time) throws RemoteException {
			mLibVLC.setTime(time);
		}

		@Override
		public boolean hasNext() throws RemoteException {
			if (mRepeating == RepeatType.Once)
				return false;
			int index = mMediaList.indexOf(mCurrentMedia);
			if (mShuffling && mPrevious.size() < mMediaList.size() - 1
					|| !mShuffling && index < mMediaList.size() - 1)
				return true;
			else
				return false;
		}

		@Override
		public boolean hasPrevious() throws RemoteException {
			if (mRepeating == RepeatType.Once)
				return false;
			int index = mMediaList.indexOf(mCurrentMedia);
			if (mPrevious.size() > 0 || index > 0)
				return true;
			else
				return false;
		}

		@Override
		public void detectHeadset(boolean enable) throws RemoteException {
		}

		@Override
		public float getRate() throws RemoteException {
			return mLibVLC.getRate();
		}

		@Override
		public void showWithoutParse(String URI) throws RemoteException {
			// TODO Auto-generated method stub

		}

		@Override
		public String getArtist() throws RemoteException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getAlbum() throws RemoteException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Bitmap getCover() throws RemoteException {
			// TODO Auto-generated method stub
			return null;
		}
	};

	private void showToast(String text, int duration) {
		Message msg = new Message();
		Bundle bundle = new Bundle();
		bundle.putString("text", text);
		bundle.putInt("duration", duration);
		msg.setData(bundle);
		msg.what = SHOW_TOAST;
		mHandler.sendMessage(msg);
	}
}
