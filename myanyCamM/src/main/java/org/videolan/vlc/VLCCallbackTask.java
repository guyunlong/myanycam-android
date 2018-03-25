
package org.videolan.vlc;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;


public abstract class VLCCallbackTask extends AsyncTask<Void, Void, Void> {

    private Context context;
    private ProgressDialog dialog;

    
    public VLCCallbackTask() {
    }

    
    public VLCCallbackTask(Context context) {
        this.context = context;
    }

    @Override
    
    protected void onPreExecute() {

        super.onPreExecute();
    }

    public abstract void run();

    @Override
    
    protected Void doInBackground(Void... params) {
        run();
        return null;
    }

    @Override
    
    protected void onPostExecute(Void result) {
        if (dialog != null)
            dialog.dismiss();
        dialog = null;
        context = null;
        super.onPostExecute(result);
    }
}
