

package org.videolan.vlc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.StringTokenizer;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;

import com.myanycam.net.SocketFunction;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

public class Util {
	public final static String TAG = "Util";
	private final static boolean hasNavBar;
	public static Context context;
	

	static {
		HashSet<String> devicesWithoutNavBar = new HashSet<String>();
		devicesWithoutNavBar.add("HTC One V");
		devicesWithoutNavBar.add("HTC One S");
		devicesWithoutNavBar.add("HTC One X");
		devicesWithoutNavBar.add("HTC One XL");
		hasNavBar = isICSOrLater()
				&& !devicesWithoutNavBar.contains(android.os.Build.MODEL);
	}

	public static LibVLC getLibVlcInstance() throws LibVlcException {
		LibVLC instance = LibVLC.getExistingInstance();
		if (instance == null) {
			Thread.setDefaultUncaughtExceptionHandler(new VlcCrashHandler());

			instance = LibVLC.getInstance();
			// Context context = SocketFunction.getAppContext();
//			SharedPreferences pref = PreferenceManager
//					.getDefaultSharedPreferences(context);
//			updateLibVlcSettings(pref);
			initVlcSetting();
			instance.init(context);		
		}
		return instance;
	}

	// abo加
	public static void initVlcSetting() {
		LibVLC instance = LibVLC.getExistingInstance();
		if (instance == null)
			return;		
		instance.setIomx(false);
		instance.setSubtitlesEncoding("");
		instance.setChroma("");
		instance.setVerboseMode(false);
		instance.setAout(0);// 0 audioTrack java
							// opensl es
		instance.setDeblocking(-1);
		Log.i(TAG, "设置完成..");
	}

	public static void updateLibVlcSettings(SharedPreferences pref) {
		LibVLC instance = LibVLC.getExistingInstance();
		if (instance == null)
			return;
		instance.setIomx(pref.getBoolean("enable_iomx", false));
		instance.setSubtitlesEncoding(pref.getString("subtitles_text_encoding",
				""));
		instance.setTimeStretching(pref.getBoolean(
				"enable_time_stretching_audio", false));
		instance.setChroma(pref.getString("chroma_format", ""));
		instance.setVerboseMode(pref.getBoolean("enable_verbose_mode", true));

		int aout;
		try {
			aout = Integer.parseInt(pref.getString("aout", "-1"));
		} catch (NumberFormatException nfe) {
			aout = -1;
		}
		Log.i(TAG, "aout:" + aout);
		int deblocking;
		try {
			deblocking = Integer.parseInt(pref.getString("deblocking", "-1"));
		} catch (NumberFormatException nfe) {
			deblocking = -1;
		}
		instance.setAout(aout);
		instance.setDeblocking(deblocking);
	}

	
	public static void toaster(Context context, int stringId, int duration) {
		Toast.makeText(context, stringId, duration).show();
	}

	public static void toaster(Context context, int stringId) {
		toaster(context, stringId, Toast.LENGTH_SHORT);
	}

	public static File URItoFile(String URI) {
		return new File(Uri.decode(URI).replace("file://", ""));
	}

	public static String URItoFileName(String URI) {
		return URItoFile(URI).getName();
	}

	public static String stripTrailingSlash(String s) {
		if (s.endsWith("/") && s.length() > 1)
			return s.substring(0, s.length() - 1);
		return s;
	}

	public static String readAsset(String assetName, String defaultS) {
		try {
			InputStream is = SocketFunction.getAppResources().getAssets()
					.open(assetName);
			BufferedReader r = new BufferedReader(new InputStreamReader(is,
					"UTF8"));
			StringBuilder sb = new StringBuilder();
			String line = r.readLine();
			if (line != null) {
				sb.append(line);
				line = r.readLine();
				while (line != null) {
					sb.append('\n');
					sb.append(line);
					line = r.readLine();
				}
			}
			return sb.toString();
		} catch (IOException e) {
			return defaultS;
		}
	}

	
	public static String millisToString(long millis) {
		boolean negative = millis < 0;
		millis = java.lang.Math.abs(millis);

		millis /= 1000;
		int sec = (int) (millis % 60);
		millis /= 60;
		int min = (int) (millis % 60);
		millis /= 60;
		int hours = (int) millis;

		String time;
		DecimalFormat format = (DecimalFormat) NumberFormat
				.getInstance(Locale.US);
		format.applyPattern("00");
		if (millis > 0) {
			time = (negative ? "-" : "") + hours + ":" + format.format(min)
					+ ":" + format.format(sec);
		} else {
			time = (negative ? "-" : "") + min + ":" + format.format(sec);
		}
		return time;
	}

	public static Bitmap scaleDownBitmap(Context context, Bitmap bitmap,
			int width) {
		
		if (bitmap != null) {
			final float densityMultiplier = context.getResources()
					.getDisplayMetrics().density;
			int w = (int) (width * densityMultiplier);
			int h = (int) (w * bitmap.getHeight() / ((double) bitmap.getWidth()));
			bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
		}
		return bitmap;
	}

	public static Bitmap cropBorders(Bitmap bitmap, int width, int height) {
		int top = 0;
		for (int i = 0; i < height / 2; i++) {
			int pixel1 = bitmap.getPixel(width / 2, i);
			int pixel2 = bitmap.getPixel(width / 2, height - i - 1);
			if ((pixel1 == 0 || pixel1 == -16777216)
					&& (pixel2 == 0 || pixel2 == -16777216)) {
				top = i;
			} else {
				break;
			}
		}

		int left = 0;
		for (int i = 0; i < width / 2; i++) {
			int pixel1 = bitmap.getPixel(i, height / 2);
			int pixel2 = bitmap.getPixel(width - i - 1, height / 2);
			if ((pixel1 == 0 || pixel1 == -16777216)
					&& (pixel2 == 0 || pixel2 == -16777216)) {
				left = i;
			} else {
				break;
			}
		}

		if (left >= width / 2 - 10 || top >= height / 2 - 10)
			return bitmap;

		// Cut off the transparency on the borders
		return Bitmap.createBitmap(bitmap, left, top, (width - (2 * left)),
				(height - (2 * top)));
	}

	public static String getValue(String string, int defaultId) {
		return (string != null && string.length() > 0) ? string
				: SocketFunction.getAppContext().getString(defaultId);
	}

	public static int convertPxToDp(int px) {
		WindowManager wm = (WindowManager) SocketFunction.getAppContext()
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		float logicalDensity = metrics.density;
		int dp = Math.round(px / logicalDensity);
		return dp;
	}

	public static int convertDpToPx(int dp) {
		return Math.round(TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, dp, SocketFunction
						.getAppResources().getDisplayMetrics()));
	}

	public static boolean isFroyoOrLater() {
		return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO;
	}

	public static boolean isGingerbreadOrLater() {
		return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD;
	}

	public static boolean isHoneycombOrLater() {
		return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB;
	}

	public static boolean isICSOrLater() {
		return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	}

	public static boolean isJellyBeanOrLater() {
		return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN;
	}

	public static boolean hasExternalStorage() {
		return Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);
	}

	public static boolean hasNavBar() {
		return hasNavBar;
	}

	
	public static boolean hasCombBar() {
		return (!isPhone() && ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) && (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.JELLY_BEAN)));
	}

	public static boolean isPhone() {
		TelephonyManager manager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		if (manager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE) {
			return false;
		} else {
			return true;
		}
	}

	public static String[] getStorageDirectories() {
		String[] dirs = null;
		BufferedReader bufReader = null;
		try {
			bufReader = new BufferedReader(new FileReader("/proc/mounts"));
			ArrayList<String> list = new ArrayList<String>();
			list.add(Environment.getExternalStorageDirectory().getPath());
			String line;
			while ((line = bufReader.readLine()) != null) {
				if (line.contains("vfat") || line.contains("exfat")
						|| line.contains("/mnt") || line.contains("/Removable")) {
					StringTokenizer tokens = new StringTokenizer(line, " ");
					String s = tokens.nextToken();
					s = tokens.nextToken();
											// point

					if (list.contains(s))
						continue;

					if (line.contains("/dev/block/vold")) {
						if (!line.startsWith("tmpfs")
								&& !line.startsWith("/dev/mapper")
								&& !s.startsWith("/mnt/secure")
								&& !s.startsWith("/mnt/shell")
								&& !s.startsWith("/mnt/asec")
								&& !s.startsWith("/mnt/obb")) {
							list.add(s);
						}
					}
				}
			}

			dirs = new String[list.size()];
			for (int i = 0; i < list.size(); i++) {
				dirs[i] = list.get(i);
			}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		} finally {
			if (bufReader != null) {
				try {
					bufReader.close();
				} catch (IOException e) {
				}
			}
		}
		return dirs;
	}

	public static String[] getCustomDirectories() {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(SocketFunction.getAppContext());
		final String custom_paths = preferences.getString("custom_paths", "");
		if (custom_paths.equals(""))
			return new String[0];
		else
			return custom_paths.split(":");
	}

	public static String[] getMediaDirectories() {
		ArrayList<String> list = new ArrayList<String>();
		list.addAll(Arrays.asList(Util.getStorageDirectories()));
		list.addAll(Arrays.asList(Util.getCustomDirectories()));
		return list.toArray(new String[list.size()]);
	}

	public static void addCustomDirectory(String path) {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(SocketFunction.getAppContext());

		ArrayList<String> dirs = new ArrayList<String>(
				Arrays.asList(getCustomDirectories()));
		dirs.add(path);
		StringBuilder builder = new StringBuilder();
		builder.append(dirs.remove(0));
		for (String s : dirs) {
			builder.append(":");
			builder.append(s);
		}
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("custom_paths", builder.toString());
		editor.commit();
	}


	
	public static String formatRateString(float rate) {
		return String.format(java.util.Locale.US, "%.2fx", rate);
	}

	
	public static boolean nullEquals(String s1, String s2) {
		return (s1 == null ? s2 == null : s1.equals(s2));
	}
}
