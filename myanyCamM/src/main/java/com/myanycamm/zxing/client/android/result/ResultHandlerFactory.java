

package com.myanycamm.zxing.client.android.result;

import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import com.google.zxing.client.result.ResultParser;
import com.myanycamm.cam.BaseActivity;


public final class ResultHandlerFactory {
	private ResultHandlerFactory() {
	}

	public static ResultHandler makeResultHandler(BaseActivity activity,
			Result rawResult) {
		ParsedResult result = parseResult(rawResult);
		ParsedResultType type = result.getType();
		// The TextResultHandler is the fallthrough for unsupported formats.
		return new TextResultHandler(activity, result);
	}

	private static ParsedResult parseResult(Result rawResult) {
		return ResultParser.parseResult(rawResult);
	}
}
