

package com.myanycamm.zxing.client.android.result;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Contacts;

import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import com.myanycamm.cam.BaseActivity;
import com.myanycamm.cam.R;
import com.myanycamm.zxing.client.android.Contents;
import com.myanycamm.zxing.client.android.LocaleManager;


public abstract class ResultHandler {
  private String TAG = "ResultHandler";
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
  private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

  private static final String GOOGLE_SHOPPER_PACKAGE = "com.google.android.apps.shopper";
  private static final String GOOGLE_SHOPPER_ACTIVITY = GOOGLE_SHOPPER_PACKAGE +
      ".results.SearchResultsActivity";

  private static final String MARKET_REFERRER_SUFFIX =
      "&referrer=utm_source%3Dbarcodescanner%26utm_medium%3Dapps%26utm_campaign%3Dscan";

  public static final int MAX_BUTTON_COUNT = 4;

  private final ParsedResult result;
  private final BaseActivity activity;
  private final Result rawResult;
  private final String customProductSearch;



  ResultHandler(BaseActivity activity, ParsedResult result) {
    this(activity, result, null);
  }

  ResultHandler(BaseActivity activity, ParsedResult result, Result rawResult) {
    this.result = result;
    this.activity = activity;
    this.rawResult = rawResult;
    this.customProductSearch = parseCustomSearchURL();
   }

  ParsedResult getResult() {
    return result;
  }

  boolean hasCustomProductSearch() {
    return customProductSearch != null;
  }

  
  public abstract int getButtonCount();

  
  public abstract int getButtonText(int index);


  
  public abstract void handleButtonPress(int index);

  
  public CharSequence getDisplayContents() {
    String contents = result.getDisplayResult();
    return contents.replace("\r", "");
  }

  
  public abstract int getDisplayTitle();

  
  public final ParsedResultType getType() {
    return result.getType();
  }

  
  final void addCalendarEvent(String summary,
                              String start,
                              String end,
                              String location,
                              String description) {
    Intent intent = new Intent(Intent.ACTION_EDIT);
    intent.setType("vnd.android.cursor.item/event");
    intent.putExtra("beginTime", calculateMilliseconds(start));
    if (start.length() == 8) {
      intent.putExtra("allDay", true);
    }
    if (end == null) {
      end = start;
    }
    intent.putExtra("endTime", calculateMilliseconds(end));
    intent.putExtra("title", summary);
    intent.putExtra("eventLocation", location);
    intent.putExtra("description", description);
    launchIntent(intent);
  }

  private static long calculateMilliseconds(String when) {
    if (when.length() == 8) {
     
      Date date;
      synchronized (DATE_FORMAT) {
        date = DATE_FORMAT.parse(when, new ParsePosition(0));
      }
      return date.getTime();
    } else {
     
      Date date;
      synchronized (DATE_TIME_FORMAT) {
       date = DATE_TIME_FORMAT.parse(when.substring(0, 15), new ParsePosition(0));
      }
      long milliseconds = date.getTime();
      if (when.length() == 16 && when.charAt(15) == 'Z') {
        Calendar calendar = new GregorianCalendar();
        int offset = calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);
        milliseconds += offset;
      }
      return milliseconds;
    }
  }

  final void addContact(String[] names, String[] phoneNumbers, String[] emails, String note,
                         String address, String org, String title) {

   
    Intent intent = new Intent(Contacts.Intents.Insert.ACTION, Contacts.People.CONTENT_URI);
    putExtra(intent, Contacts.Intents.Insert.NAME, names != null ? names[0] : null);

    int phoneCount = Math.min((phoneNumbers != null) ? phoneNumbers.length : 0,
        Contents.PHONE_KEYS.length);
    for (int x = 0; x < phoneCount; x++) {
      putExtra(intent, Contents.PHONE_KEYS[x], phoneNumbers[x]);
    }

    int emailCount = Math.min((emails != null) ? emails.length : 0, Contents.EMAIL_KEYS.length);
    for (int x = 0; x < emailCount; x++) {
      putExtra(intent, Contents.EMAIL_KEYS[x], emails[x]);
    }

    putExtra(intent, Contacts.Intents.Insert.NOTES, note);
    putExtra(intent, Contacts.Intents.Insert.POSTAL, address);
    putExtra(intent, Contacts.Intents.Insert.COMPANY, org);
    putExtra(intent, Contacts.Intents.Insert.JOB_TITLE, title);
    launchIntent(intent);
  }



  final void sendEmail(String address, String subject, String body) {
    sendEmailFromUri("mailto:" + address, address, subject, body);
  }

 
  final void sendEmailFromUri(String uri, String email, String subject, String body) {
    Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse(uri));
    if (email != null) {
      intent.putExtra(Intent.EXTRA_EMAIL, new String[] {email});
    }
    putExtra(intent, Intent.EXTRA_SUBJECT, subject);
    putExtra(intent, Intent.EXTRA_TEXT, body);
    intent.setType("text/plain");
    launchIntent(intent);
  }



  final void sendSMS(String phoneNumber, String body) {
    sendSMSFromUri("smsto:" + phoneNumber, body);
  }

  final void sendSMSFromUri(String uri, String body) {
    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uri));
    putExtra(intent, "sms_body", body);
   
    intent.putExtra("compose_mode", true);
    launchIntent(intent);
  }




  final void dialPhone(String phoneNumber) {
    launchIntent(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber)));
  }

  final void dialPhoneFromUri(String uri) {
    launchIntent(new Intent(Intent.ACTION_DIAL, Uri.parse(uri)));
  }

  final void openMap(String geoURI) {
    launchIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(geoURI)));
  }

  
  final void searchMap(String address, String title) {
    String query = address;
    if (title != null && title.length() > 0) {
      query = query + " (" + title + ')';
    }
    launchIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query))));
  }

  final void getDirections(double latitude, double longitude) {
    launchIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://maps.google." +
        LocaleManager.getCountryTLD() + "/maps?f=d&daddr=" + latitude + ',' + longitude)));
  }

 
  final void openProductSearch(String upc) {
	  goToEasouSearch(upc);
//    Uri uri = Uri.parse("http://www.google." + LocaleManager.getProductSearchCountryTLD() +
//        "/m/products?q=" + upc + "&source=zxing");
//    launchIntent(new Intent(Intent.ACTION_VIEW, uri));
    
  }

  final void openBookSearch(String isbn) {
	  goToEasouSearch(isbn);
//    Uri uri = Uri.parse("http://books.google." + LocaleManager.getBookSearchCountryTLD() +
//        "/books?vid=isbn" + isbn);
//    launchIntent(new Intent(Intent.ACTION_VIEW, uri));
  }

  final void searchBookContents(String isbn) {
	  goToEasouSearch(isbn);
//    Intent intent = new Intent(Intents.SearchBookContents.ACTION);
//    putExtra(intent, Intents.SearchBookContents.ISBN, isbn);
//    launchIntent(intent);
  }


  final void openURL(String url) {
//	  url = ((CaptureActivity)activity).getContentTextview();//得到最新修改过后网址
//    launchIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
//	  Intent intent = new Intent(activity, EasouSearchActivity.class);
//	  intent.setAction("com.android.easou.easousearch.favorites");
//	  Bundle bundle = new Bundle();//该类用作携带数据
//	  intent.putExtra("EASOU_WEBBROWSER_URL", url);
//	  intent.putExtras(bundle);//附带上额外的数据
//	  activity.startActivity(intent);
//		intent.setAction("com.android.easou.easousearch.favorites");
//		intent.putExtra("EASOU_WEBBROWSER_TITLE", "百度");
//		intent.putExtra("EASOU_WEBBROWSER_URL",url);
//		this.startActivity(intent);
  }

  final void webSearch(String query) {
    Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
    intent.putExtra("query", query);
    launchIntent(intent);
  }

  final void openGoogleShopper(String query) {
	  goToEasouSearch(query);
//    try {
//      activity.getPackageManager().getPackageInfo(GOOGLE_SHOPPER_PACKAGE, 0);
//     
//      Intent intent = new Intent(Intent.ACTION_SEARCH);
//      intent.setClassName(GOOGLE_SHOPPER_PACKAGE, GOOGLE_SHOPPER_ACTIVITY);
//      intent.putExtra(SearchManager.QUERY, query);
//      activity.startActivity(intent);
//    } catch (PackageManager.NameNotFoundException e) {
//     
//      AlertDialog.Builder builder = new AlertDialog.Builder(activity);
//      builder.setPositiveButton(R.string.button_ok, shopperMarketListener);
//      builder.setNegativeButton(R.string.button_cancel, null);
//      builder.show();
//    }
  }

  
  void goToEasouSearch(String query){	  

//		if(query!=null && query.length()>0){
//			Intent intent = new Intent(SearchActivityMainView.GOTO_SEARCH_INPUT_INTERFACE);
//			intent.setComponent(
//					new ComponentName(activity, EasouSearchActivity.class));
//			Bundle bundle=new Bundle();
//			String finalQuery = ((CaptureActivity)activity).getContentTextview();//得到最终的修改过后的内容
//			bundle.putString(Constant.WIDGET_SPEAK_WORD,finalQuery);
//			intent.putExtras(bundle);
//			activity.startActivity(intent);
//		} else {
////			Toast.makeText(this, "识别有误", Toast.LENGTH_LONG).show();
//		}
  }
  

  
  void launchIntent(Intent intent) {
    if (intent != null) {
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      try {
        activity.startActivity(intent);
      } catch (ActivityNotFoundException e) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.app_name);
        builder.setPositiveButton(R.string.confirm, null);
        builder.show();
      }
    }
  }

  private static void putExtra(Intent intent, String key, String value) {
    if (value != null && value.length() > 0) {
      intent.putExtra(key, value);
    }
  }

  protected void showNotOurResults(int index, AlertDialog.OnClickListener proceedListener) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);

     

      AlertDialog.Builder builder = new AlertDialog.Builder(activity);
      builder.setPositiveButton(R.string.confirm, proceedListener);
      builder.show();
  }

  private String parseCustomSearchURL() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    if (customProductSearch != null && customProductSearch.trim().length() == 0) {
      return null;
    }
    return customProductSearch;
  }

  String fillInCustomSearchURL(String text) {
    String url = customProductSearch.replace("%s", text);
    if (rawResult != null) {
      url = url.replace("%f", rawResult.getBarcodeFormat().toString());
    }
    return url;
  }

}