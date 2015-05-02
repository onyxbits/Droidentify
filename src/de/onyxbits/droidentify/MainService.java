package de.onyxbits.droidentify;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.floreysoft.jmte.Engine;

import android.app.Service;
import android.content.Intent;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class MainService extends Service {

	public static final String TMPLDIR = "tmpl";
	public static final String HTDOCS = "htdocs";
	private static final int BUFFER = 1024 * 8;
	private boolean running = false;

	private IBinder binder;

	@Override
	public IBinder onBind(Intent intent) {
		if (binder == null) {
			binder = new LocalBinder(this);
		}
		return binder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (!running) {
			running = true;
			new ReporterTask(this).execute(new Object());
		}
		else {
			onReport(null);
		}
		return START_STICKY;
	}

	public void onDestroy() {
	}

	public File getDocDir() {
		return new File(getFilesDir(), HTDOCS);
	}

	public void createReport() throws IOException {
		// Target directory.
		File dest = getDocDir();
		dest.mkdirs();

		// Copy build.prop to target
		FileInputStream ins = new FileInputStream(new File("/system/build.prop"));
		FileOutputStream out = new FileOutputStream(new File(dest, "build.prop"));
		copyFile(ins, out);
		ins.close();
		out.close();

		// Create the layout template
		Map<String, Object> model = createModel();
		InputStream li = getAssets().open(TMPLDIR + "/layout.tmpl");
		String layout = readFile(li);
		li.close();
		Map<String, Object> layoutModel = new HashMap<String, Object>();
		try {
			layoutModel.put("APPVERSION", "v"
					+ getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		}
		catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		layoutModel.put("PRODUCT", Devices.getDeviceName());

		// Create the individual pages of the report from the layout template
		String[] files = getAssets().list(TMPLDIR);
		for (String file : files) {
			if (file.equals("layout.tmpl")) {
				continue;
			}
			InputStream inp = getAssets().open(TMPLDIR + "/" + file);
			if (file.endsWith("html")) {
				layoutModel.put("CONTENT", readFile(inp));
				String content = new Engine().transform(layout, layoutModel);
				PrintWriter pw = new PrintWriter(new File(dest, file));
				pw.write(new Engine().transform(content, model));
				pw.close();
			}
			else {
				out = new FileOutputStream(new File(dest, file));
				copyFile(inp, out);
				out.close();
			}
			inp.close();
		}

		// Create an enhanced build.prop
		ins = new FileInputStream(new File("/system/build.prop"));
		StringBuilder bprop = new StringBuilder(readFile(ins));
		ins.close();
		bprop.append("\n\n# DummyDroid properties\n");
		bprop.append("dd.features=${foreach FEATURES FEATURE , }${FEATURE}${end}\n");
		bprop.append("dd.libraries=${foreach LIBRARIES LIBRARY , }${LIBRARY}${end}\n");
		PrintWriter pw = new PrintWriter(new File(dest, "build_extra.prop"));
		pw.write(new Engine().transform(bprop.toString(), model));
		pw.close();

		// Zip the contents of the target directory.
		files = dest.list(new ZippedReportFilter());
		FileOutputStream zdest = new FileOutputStream(new File(dest, ZippedReportFilter.NAME));
		ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(zdest));
		byte data[] = new byte[BUFFER];
		for (int i = 0; i < files.length; i++) {
			FileInputStream fi = new FileInputStream(new File(dest, files[i]));
			BufferedInputStream origin = new BufferedInputStream(fi, BUFFER);
			ZipEntry entry = new ZipEntry("/" + files[i]);
			zout.putNextEntry(entry);
			int count;
			while ((count = origin.read(data, 0, BUFFER)) != -1) {
				zout.write(data, 0, count);
			}
			origin.close();
		}
		zout.close();
		zdest.close();
	}

	private Map<String, Object> createModel() throws IOException {
		Map<String, Object> model = new HashMap<String, Object>();
		InputStream ins = new FileInputStream(new File("/system/build.prop"));
		model.put("BUILDPROP", readFile(ins));
		ins.close();

		Locale[] locales = Locale.getAvailableLocales();
		Vector<String> locs = new Vector<String>();
		for (Locale l : locales) {
			locs.add(l.toString());
		}
		model.put("LOCALES", locs);

		Vector<String> features = new Vector<String>();
		FeatureInfo[] infos = getPackageManager().getSystemAvailableFeatures();
		for (FeatureInfo info : infos) {
			if (info.name == null) {
				// TODO: GL features
			}
			else {
				features.add(info.name);
			}
		}
		model.put("FEATURES", features);

		model.put("LIBRARIES", Arrays.asList(getPackageManager().getSystemSharedLibraryNames()));

		Vector<Tuple> ids = new Vector<Tuple>();

		Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		display.getMetrics(dm);
		ids.add(new Tuple("Display Size", dm.widthPixels + " x " + dm.heightPixels + " px"));
		if (Build.SERIAL != null) {
			ids.add(new Tuple("Serial number", Build.SERIAL));
		}
		ids.add(new Tuple("OS Release", Build.VERSION.RELEASE));
		if (getGsfAndroidId() != null) {
			ids.add(new Tuple("GSF ID", getGsfAndroidId()));
		}

		ids.add(new Tuple("Android ID", Secure.getString(getContentResolver(), Secure.ANDROID_ID)));

		if (tm.getDeviceId() != null) {
			ids.add(new Tuple("IMEI", tm.getDeviceId()));
			ids.add(new Tuple("IMSI", tm.getSubscriberId()));
			ids.add(new Tuple("SIM Operator", tm.getSimOperator() + " (" + tm.getSimOperatorName() + ")"));
			ids.add(new Tuple("SIM Serial", tm.getSimSerialNumber()));
			ids.add(new Tuple("Network Operator", tm.getNetworkOperator() + " ("
					+ tm.getNetworkOperatorName() + ")"));
			ids.add(new Tuple("Phone number", tm.getLine1Number()));

		}

		model.put("IDENTIFIERS", ids);

		Vector<Tuple> paths = new Vector<Tuple>();
		paths.add(new Tuple("External Storage", Environment.getExternalStorageDirectory()
				.getAbsolutePath()));
		paths.add(new Tuple("Music", Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_MUSIC).getAbsolutePath()));
		paths.add(new Tuple("Podcasts", Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PODCASTS).getAbsolutePath()));
		paths.add(new Tuple("Ringtones", Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_RINGTONES).getAbsolutePath()));
		paths.add(new Tuple("Alarms", Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_ALARMS).getAbsolutePath()));
		paths.add(new Tuple("Notifications", Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_NOTIFICATIONS).getAbsolutePath()));
		paths.add(new Tuple("Pictures", Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES).getAbsolutePath()));
		paths.add(new Tuple("Movies", Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_MOVIES).getAbsolutePath()));
		paths.add(new Tuple("Downloads", Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()));
		paths.add(new Tuple("Dcim", Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DCIM).getAbsolutePath()));
		model.put("PATHS", paths);

		return model;
	}

	private void copyFile(InputStream ins, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int length;
		while ((length = ins.read(buffer)) > 0) {
			out.write(buffer, 0, length);
		}
	}

	protected static String readFile(InputStream ins) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(ins, "UTF-8"));
		String line = bufferedReader.readLine();
		while (line != null) {
			sb.append(line);
			sb.append('\n');
			line = bufferedReader.readLine();
		}
		return sb.toString();
	}

	private String getGsfAndroidId() {
		try {
			Uri URI = Uri.parse("content://com.google.android.gsf.gservices");
			String ID_KEY = "android_id";
			String params[] = { ID_KEY };
			Cursor c = getContentResolver().query(URI, null, null, params, null);
			if (!c.moveToFirst() || c.getColumnCount() < 2)
				return Long.toHexString(Long.parseLong(c.getString(1)));
			c.close();
		}
		catch (Exception e) {
			return null;
		}
		return null;
	}

	public void onReport(Exception e) {
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Intent.ACTION_VIEW));
	}

}
