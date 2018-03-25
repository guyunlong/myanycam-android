package com.myanycamm.model;

import java.io.File;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import com.myanycamm.utils.Constants;
import com.myanycamm.utils.ELog;

public class TwitterLogin extends ThirdLogin {
	private static String TAG = "TwitterLogin";
	private static Twitter twitter;
	private static RequestToken requestToken;
	private static  Configuration conf;
	private static SharedPreferences mSharedPreferences;

	public TwitterLogin(Activity _activity) {
		super(_activity);
		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
		configurationBuilder.setOAuthConsumerKey(Constants.CONSUMER_KEY);
		configurationBuilder.setOAuthConsumerSecret(Constants.CONSUMER_SECRET);
		conf = configurationBuilder.build();
		twitter = new TwitterFactory(conf).getInstance();
		 mSharedPreferences = activity
				.getSharedPreferences(Constants.PREFERENCE_NAME,
						((Context) activity).MODE_PRIVATE);
	}

	@Override
	public void login() {
		if(isConnected()){
			sharePhoto();
		}else{	

			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						requestToken = twitter
								.getOAuthRequestToken(Constants.CALLBACK_URL);
					} catch (TwitterException e) {
						ELog.i(TAG, "twitter登录出错:" + e.getErrorMessage());
						e.printStackTrace();
						e.printStackTrace();
					}
					activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri
							.parse(requestToken.getAuthenticationURL())));

				}
			}).start();
			Toast.makeText(activity, "Please authorize this app!",
					Toast.LENGTH_LONG).show();
		}
	

	}

	
	public void restoreToken(Uri uri) {
		final String verifier = uri
				.getQueryParameter(Constants.IEXTRA_OAUTH_VERIFIER);
		ELog.i(TAG, "verifier:" + verifier);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					ELog.i(TAG, "requestToken:" + requestToken);
					AccessToken accessToken = twitter.getOAuthAccessToken(
							requestToken, verifier);					
					Editor e = mSharedPreferences.edit();
					e.putString(Constants.PREF_KEY_TOKEN,
							accessToken.getToken());
					e.putString(Constants.PREF_KEY_SECRET,
							accessToken.getTokenSecret());
					e.commit();
					sharePhoto();
				} catch (TwitterException e) {
					ELog.e(TAG, "保存信息出错:" + e.getErrorMessage());
//					Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG)
//							.show();
				}
			}
		}).start();

	}
	
	public void sharePhoto(){
		String oauthAccessToken = mSharedPreferences.getString(
				Constants.PREF_KEY_TOKEN, "");
		String oAuthAccessTokenSecret = mSharedPreferences.getString(
				Constants.PREF_KEY_SECRET, "");
		ELog.i(TAG, oauthAccessToken);

		ConfigurationBuilder confbuilder = new ConfigurationBuilder();
		conf = confbuilder
				.setOAuthConsumerKey(Constants.CONSUMER_KEY)
				.setOAuthConsumerSecret(Constants.CONSUMER_SECRET)
				.setOAuthAccessToken(oauthAccessToken)
				.setOAuthAccessTokenSecret(oAuthAccessTokenSecret).build();
		twitter = new TwitterFactory(conf).getInstance();
		File file = new File(Environment.getExternalStorageDirectory()
				+ "/img.jpg");		
		ELog.i(TAG, "file:" + file.exists());	

		uploadPic(file, "abiijdlljsdflkd", twitter);

	}
	
	

	public void uploadPic(final File file, final String message, final Twitter twitter) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					ELog.i(TAG, "twitter上传图片");
					ELog.i(TAG,"name:"+twitter.verifyCredentials().getName());
					ELog.i(TAG, "后面...");
					StatusUpdate status = new StatusUpdate(message);					
					status.setMedia(file);			
					twitter.updateStatus(status);
				
				} catch (TwitterException e) {
					ELog.d("TAG", "Pic Upload error" + e.getErrorMessage());
				}				
			}
		}).start();

	}
	
	
	private boolean isConnected() {
		return mSharedPreferences.getString(Constants.PREF_KEY_TOKEN, null) != null;
	}


}
