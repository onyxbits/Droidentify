package de.onyxbits.droidentify;

import android.os.Binder;

public class LocalBinder extends Binder {

	private MainService serverService;

	public LocalBinder(MainService ss) {
		this.serverService = ss;
	}
	
	public MainService getService() {
		return serverService;
	}

}
