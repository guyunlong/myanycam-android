

package org.videolan.vlc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.videolan.vlc.interfaces.IAudioPlayer;
import org.videolan.vlc.interfaces.IAudioService;
import org.videolan.vlc.interfaces.IAudioServiceCallback;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

public class AudioServiceController {
    public static final String TAG = "AudioServiceContoller";

    private static AudioServiceController mInstance;
    private static boolean mIsBound = false;
    private IAudioService mAudioServiceBinder;
    private ServiceConnection mAudioServiceConnection;
    private final ArrayList<IAudioPlayer> mAudioPlayer;
    private final IAudioServiceCallback mCallback = new IAudioServiceCallback.Stub() {
        @Override
        public void update() throws RemoteException {
            updateAudioPlayer();
        }
    };

    private AudioServiceController() {
        mAudioPlayer = new ArrayList<IAudioPlayer>();
    }

    public static AudioServiceController getInstance() {
        if (mInstance == null) {
            mInstance = new AudioServiceController();
        }
        return mInstance;
    }

    
    public void bindAudioService(Context context) {
        if (context == null) {
            Log.w(TAG, "bindAudioService() with null Context. Ooops" );
            return;
        }
        context = context.getApplicationContext();

        if (!mIsBound) {
            Intent service = new Intent(context, AudioService.class);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final boolean enableHS = prefs.getBoolean("enable_headset_detection", true);

           
            mAudioServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Log.d(TAG, "Service Disconnected");
                    mAudioServiceBinder = null;
                    mIsBound = false;
                }

                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    if (!mIsBound)
                        return;
                    Log.d(TAG, "Service Connected");
                    mAudioServiceBinder = IAudioService.Stub.asInterface(service);

                   
                    try {
                        mAudioServiceBinder.addAudioCallback(mCallback);
                        mAudioServiceBinder.detectHeadset(enableHS);
                    } catch (RemoteException e) {
                        Log.e(TAG, "remote procedure call failed: addAudioCallback()");
                    }
                    updateAudioPlayer();
                }
            };

            mIsBound = context.bindService(service, mAudioServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
           
            try {
                if (mAudioServiceBinder != null)
                    mAudioServiceBinder.addAudioCallback(mCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "remote procedure call failed: addAudioCallback()");
            }
        }
    }

    public void unbindAudioService(Context context) {
        if (context == null) {
            Log.w(TAG, "unbindAudioService() with null Context. Ooops" );
            return;
        }
        context = context.getApplicationContext();

        if (mIsBound) {
            mIsBound = false;
            try {
                if (mAudioServiceBinder != null)
                    mAudioServiceBinder.removeAudioCallback(mCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "remote procedure call failed: removeAudioCallback()");
            }
            context.unbindService(mAudioServiceConnection);
            mAudioServiceBinder = null;
            mAudioServiceConnection = null;
        }
    }

    
    public void addAudioPlayer(IAudioPlayer ap) {
        mAudioPlayer.add(ap);
    }

    
    public void removeAudioPlayer(IAudioPlayer ap) {
        if (mAudioPlayer.contains(ap)) {
            mAudioPlayer.remove(ap);
        }
    }

    
    private void updateAudioPlayer() {
        for (IAudioPlayer player : mAudioPlayer)
            player.update();
    }

    
    private <T> T remoteProcedureCall(IAudioService instance, Class<T> returnType, T defaultValue, String functionName, Class<?> parameterTypes[], Object parameters[]) {
        if(instance == null) {
            return defaultValue;
        }

        try {
            Method m = IAudioService.class.getMethod(functionName, parameterTypes);
            @SuppressWarnings("unchecked")
            T returnVal = (T) m.invoke(instance, parameters);
            return returnVal;
        } catch(NoSuchMethodException e) {
            e.printStackTrace();
            return defaultValue;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return defaultValue;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return defaultValue;
        } catch (InvocationTargetException e) {
            if(e.getTargetException() instanceof RemoteException) {
                Log.e(TAG, "remote procedure call failed: " + functionName + "()");
            }
            return defaultValue;
        }
    }

    public void load(List<String> mediaPathList, int position) {
        load(mediaPathList, position, false, false);
    }

    public void load(String mediaPath, int position, boolean libvlcBacked, boolean noVideo) {
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add(mediaPath);
        load(arrayList, position, libvlcBacked, noVideo);
    }

    public void load(List<String> mediaPathList, int position, boolean libvlcBacked, boolean noVideo) {
        remoteProcedureCall(mAudioServiceBinder, Void.class, (Void)null, "load",
                new Class<?>[] { List.class, int.class, boolean.class, boolean.class },
                new Object[] { mediaPathList, position, libvlcBacked, noVideo } );
    }

    public void append(String mediaPath) {
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add(mediaPath);
        append(arrayList);
    }

    public void append(List<String> mediaPathList) {
        remoteProcedureCall(mAudioServiceBinder, Void.class, (Void)null, "append",
                new Class<?>[] { List.class },
                new Object[] { mediaPathList } );
    }

    @SuppressWarnings("unchecked")
    public List<String> getItems() {
        List<String> def = new ArrayList<String>();
        return remoteProcedureCall(mAudioServiceBinder, List.class, def, "getItems", null, null);
    }

    public String getItem() {
        return remoteProcedureCall(mAudioServiceBinder, String.class, (String)null, "getItem", null, null);
    }

    public void stop() {
        remoteProcedureCall(mAudioServiceBinder, Void.class, (Void)null, "stop", null, null);
        updateAudioPlayer();
    }

    public void showWithoutParse(String u) {
        remoteProcedureCall(mAudioServiceBinder, Void.class, (Void)null, "showWithoutParse",
                new Class<?>[] { String.class },
                new Object[] { u } );
        updateAudioPlayer();
    }


}
