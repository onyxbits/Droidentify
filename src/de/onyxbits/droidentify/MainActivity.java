package de.onyxbits.droidentify;


import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

public class MainActivity extends ActionBarActivity {

	private ProxyReceiver proxyReceiver;

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		WebView wv = (WebView) findViewById(R.id.webview);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.setWebViewClient(new Interceptor());
		IntentFilter filter = new IntentFilter(Intent.ACTION_VIEW);
		proxyReceiver = new ProxyReceiver(this);
		LocalBroadcastManager.getInstance(this).registerReceiver(proxyReceiver, filter);
		startService(new Intent(this, MainService.class));
	}

	@Override
	protected void onDestroy() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(proxyReceiver);
		if (isFinishing()) {
			stopService(new Intent(this, MainService.class));
		}
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.share) {
			Interceptor.shareReport(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
