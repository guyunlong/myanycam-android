

package org.videolan.vlc;

import java.util.HashSet;
import java.util.Locale;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.TrackInfo;

import com.myanycam.net.SocketFunction;

import android.content.Context;
import android.database.sqlite.SQLiteFullException;
import android.graphics.Bitmap;
import android.util.Log;

public class Media implements Comparable<Media> {

    public final static String TAG = "VLC/MediaItem";

    public final static HashSet<String> VIDEO_EXTENSIONS;
    public final static HashSet<String> AUDIO_EXTENSIONS;
    public final static String EXTENSIONS_REGEX;
    public final static HashSet<String> FOLDER_BLACKLIST;

    static {
        String[] video_extensions = {
                ".3g2", ".3gp", ".3gp2", ".3gpp", ".amv", ".asf", ".avi", ".divx", "drc", ".dv",
                ".f4v", ".flv", ".gvi", ".gxf", ".iso", ".m1v", ".m2v", ".m2t", ".m2ts", ".m4v",
                ".mkv", ".mov", ".mp2", ".mp2v", ".mp4", ".mp4v", ".mpe", ".mpeg",
                ".mpeg1", ".mpeg2", ".mpeg4", ".mpg", ".mpv2", ".mts", ".mtv", ".mxf", ".mxg",
                ".nsv", ".nuv", ".ogm", ".ogv", ".ogx", ".ps", ".rec", ".rm", ".rmvb", ".tod",
                ".ts", ".tts", ".vob", ".vro", ".webm", ".wm", ".wmv", ".wtv", ".xesc" };

        String[] audio_extensions = {
                ".3ga", ".a52", ".aac", ".ac3", ".adt", ".adts", ".aif", ".aifc", ".aiff", ".amr",
                ".aob", ".ape", ".awb", ".caf", ".dts", ".flac", ".it", ".m4a", ".m4p",
                ".mid", ".mka", ".mlp", ".mod", ".mpa", ".mp1", ".mp2", ".mp3", ".mpc", ".mpga",
                ".oga", ".ogg", ".oma", ".opus", ".ra", ".ram", ".rmi", ".s3m", ".spx", ".tta",
                ".voc", ".vqf", ".w64", ".wav", ".wma", ".wv", ".xa", ".xm" };

        String[] folder_blacklist = {
                "/alarms",
                "/notifications",
                "/ringtones",
                "/media/alarms",
                "/media/notifications",
                "/media/ringtones",
                "/media/audio/alarms",
                "/media/audio/notifications",
                "/media/audio/ringtones",
                "/Android/data/" };

        VIDEO_EXTENSIONS = new HashSet<String>();
        for (String item : video_extensions)
            VIDEO_EXTENSIONS.add(item);
        AUDIO_EXTENSIONS = new HashSet<String>();
        for (String item : audio_extensions)
            AUDIO_EXTENSIONS.add(item);

        StringBuilder sb = new StringBuilder(115);
        sb.append(".+(\\.)((?i)(");
        sb.append(video_extensions[0].substring(1));
        for(int i = 1; i < video_extensions.length; i++) {
            sb.append('|');
            sb.append(video_extensions[i].substring(1));
        }
        for(int i = 0; i < audio_extensions.length; i++) {
            sb.append('|');
            sb.append(audio_extensions[i].substring(1));
        }
        sb.append("))");
        EXTENSIONS_REGEX = sb.toString();
        FOLDER_BLACKLIST = new HashSet<String>();
        for (String item : folder_blacklist)
            FOLDER_BLACKLIST.add(android.os.Environment.getExternalStorageDirectory().getPath() + item);
    }

    public final static int TYPE_ALL = -1;
    public final static int TYPE_VIDEO = 0;
    public final static int TYPE_AUDIO = 1;

    
    private String mTitle;
    private String mCopyright;
    private String mTrackNumber;
    private String mDescription;
    private String mRating;
    private String mDate;
    private String mSettings;
    private String mNowPlaying;
    private String mPublisher;
    private String mEncodedBy;
    private String mTrackID;
    private String mArtworkURL;

    private final String mLocation;
    private String mFilename;
    private long mTime = 0;
    private int mAudioTrack = -1;
    private int mSpuTrack = -2;
    private long mLength = 0;
    private int mType;
    private int mWidth = 0;
    private int mHeight = 0;
    private Bitmap mPicture;
    private boolean mIsPictureParsed;

    
    public Media(String URI, Boolean addToDb) {
        mLocation = URI;

        LibVLC mLibVlc = null;
        try {
            mLibVlc = Util.getLibVlcInstance();
            mType = TYPE_ALL;

            TrackInfo[] tracks = mLibVlc.readTracksInfo(mLocation);

            extractTrackInfo(tracks);
        } catch (LibVlcException e) {
            e.printStackTrace();
        }

        if (addToDb) {
           
            MediaDatabase db = MediaDatabase.getInstance(SocketFunction.getAppContext());
            db.addMedia(this);
        }
    }

    public Media(String URI, int position) {
        mLocation = URI;

        LibVLC mLibVlc = null;
        try {
            mLibVlc = Util.getLibVlcInstance();
            mType = TYPE_ALL;

            TrackInfo[] tracks = mLibVlc.readTracksInfoPosition(position);

            extractTrackInfo(tracks);
        } catch (LibVlcException e) {
            e.printStackTrace();
        }
    }

    private void extractTrackInfo(TrackInfo[] tracks) {
        if (tracks == null)
            return;

        for (TrackInfo track : tracks) {
            if (track.Type == TrackInfo.TYPE_VIDEO) {
                mType = TYPE_VIDEO;
                mWidth = track.Width;
                mHeight = track.Height;
            } else if (mType == TYPE_ALL && track.Type == TrackInfo.TYPE_AUDIO){
                mType = TYPE_AUDIO;
            } else if (track.Type == TrackInfo.TYPE_META) {
                mLength = track.Length;
                mTitle = track.Title;
                mArtworkURL = track.ArtworkURL;
                Log.d(TAG, "Title " + mTitle);
            }
        }

        
        if (mType == TYPE_ALL) {
            int dotIndex = mLocation.lastIndexOf(".");
            if (dotIndex != -1) {
                String fileExt = mLocation.substring(dotIndex);
                if( Media.VIDEO_EXTENSIONS.contains(fileExt) ) {
                    mType = TYPE_VIDEO;
                } else if (Media.AUDIO_EXTENSIONS.contains(fileExt)) {
                    mType = TYPE_AUDIO;
                }
            }
        }
    }

    public Media(Context context, String location, long time, long length, int type,
            Bitmap picture, String title, String artist, String genre, String album,
            int width, int height, String artworkURL, int audio, int spu) {
        mLocation = location;
        mFilename = null;
        mTime = time;
        mAudioTrack = audio;
        mSpuTrack = spu;
        mLength = length;
        mType = type;
        mPicture = picture;
        mWidth = width;
        mHeight = height;

        mTitle = title;
        mArtworkURL = artworkURL;
    }

    
    @Override
    public int compareTo(Media another) {
        return mTitle.toUpperCase(Locale.getDefault()).compareTo(
                another.getTitle().toUpperCase(Locale.getDefault()));
    }

    public String getLocation() {
        return mLocation;
    }

    public void updateMeta() {

    }

    public String getFileName() {
        if (mFilename == null) {
            mFilename = Util.URItoFileName(mLocation);
        }
        return mFilename;
    }

    public long getTime() {
        return mTime;
    }

    public void setTime(long time) {
        mTime = time;
    }

    public int getAudioTrack() {
        return mAudioTrack;
    }

    public void setAudioTrack(int track) {
        mAudioTrack = track;
    }

    public int getSpuTrack() {
        return mSpuTrack;
    }

    public void setSpuTrack(int track) {
        mSpuTrack = track;
    }

    public long getLength() {
        return mLength;
    }

    public int getType() {
        return mType;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public Bitmap getPicture() {
       
       
        if (mPicture == null) {
        	
        } else {
            return mPicture;
        }
		return mPicture;
    }

    public void setPicture(Context context, Bitmap p) {
        Log.d(TAG, "Set new picture for " + getTitle());
        try {
            MediaDatabase.getInstance(context).updateMedia(
                mLocation,
                MediaDatabase.mediaColumn.MEDIA_PICTURE,
                p);
        } catch (SQLiteFullException e) {
           
            e.printStackTrace();
        }
        mIsPictureParsed = true;
    }

    public boolean isPictureParsed() {
        return mIsPictureParsed;
    }

    public void setPictureParsed(boolean isParsed) {
        mIsPictureParsed = isParsed;
    }

    public String getTitle() {
        if (mTitle != null && mType != TYPE_VIDEO)
            return mTitle;
        else {
            int end = getFileName().lastIndexOf(".");
            if (end <= 0)
                return getFileName();
            return getFileName().substring(0, end);
        }
    }



    public String getCopyright() {
        return mCopyright;
    }


    public String getTrackNumber() {
        return mTrackNumber;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getRating() {
        return mRating;
    }

    public String getDate() {
        return mDate;
    }

    public String getSettings() {
        return mSettings;
    }

    public String getNowPlaying() {
        return mNowPlaying;
    }

    public String getPublisher() {
        return mPublisher;
    }

    public String getEncodedBy() {
        return mEncodedBy;
    }

    public String getTrackID() {
        return mTrackID;
    }

    public String getArtworkURL() {
        return mArtworkURL;
    }
}
