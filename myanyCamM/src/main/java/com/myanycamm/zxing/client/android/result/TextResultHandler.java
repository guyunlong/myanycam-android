

package com.myanycamm.zxing.client.android.result;

import com.google.zxing.client.result.ParsedResult;
import com.myanycamm.cam.BaseActivity;


public final class TextResultHandler extends ResultHandler {



  public TextResultHandler(BaseActivity activity, ParsedResult result) {
    super(activity, result);
  }

  @Override
  public int getButtonCount() {
	return 0;}

  @Override
  public int getButtonText(int index) {
	return index;
  }

  @Override
  public void handleButtonPress(int index) {
    String text = getResult().getDisplayResult();
    goToEasouSearch(text);
  }

  @Override
  public int getDisplayTitle() {
	return 0;
	  
  }
}
