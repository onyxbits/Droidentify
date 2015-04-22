package de.onyxbits.droidentify;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class Interceptor extends WebViewClient {

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		if (url.startsWith("http")) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			view.getContext().startActivity(intent);
			return true;
		}
		if (url.endsWith("prop")) {
			try {
				FileInputStream fis = new FileInputStream(new File(new URI(url)));
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("text/html");
				intent.putExtra(Intent.EXTRA_TEXT, MainService.readFile(fis));
				view.getContext().startActivity(intent);
			}
			catch (Exception e) {
				Log.d(getClass().getName(), e.getMessage());
			}
			return true;
		}
		if (url.endsWith("zip")) {
			shareReport(view.getContext());
			return true;
		}
		return false;
	}

	public static void shareReport(Context ctx) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/html");
		intent.putExtra(Intent.EXTRA_SUBJECT, ctx.getString(R.string.subjectline));
		intent.putExtra(
				Intent.EXTRA_STREAM,
				FileProvider.getUriForFile(ctx, "de.onyxbits.fileprovider",
						new File(new File(ctx.getFilesDir(), MainService.HTDOCS), ZippedReportFilter.NAME)));
		ctx.startActivity(Intent.createChooser(intent, ctx.getString(R.string.sharevia)));
	}
}
