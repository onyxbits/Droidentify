package de.onyxbits.droidentify;

import java.io.File;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.webkit.WebView;

class ProxyReceiver extends BroadcastReceiver {

	private MainActivity mainActivity;

	public ProxyReceiver(MainActivity m) {
		this.mainActivity = m;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		WebView wv = (WebView) mainActivity.findViewById(R.id.webview);
		File target = new File(new File(mainActivity.getFilesDir(), MainService.HTDOCS),
				"index.html");
		wv.loadUrl(target.toURI().toString());
	}
}
