package de.onyxbits.droidentify;

import android.os.AsyncTask;

public class ReporterTask extends AsyncTask<Object, Integer, Exception> {

	private MainService serverService;

	public ReporterTask(MainService ss) {
		this.serverService = ss;
	}
	@Override
	protected Exception doInBackground(Object... params) {
		try {
			serverService.createReport();
		}
		catch (Exception e) {
			return e;
		}
		return null;
	}
	
	@Override
	protected void onPostExecute(Exception e) {
		serverService.onReport(e);
	}

}
