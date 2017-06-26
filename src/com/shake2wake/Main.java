/*
 * Copyright 2014 VOVLab
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shake2wake;

import com.shake2wake.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class Main extends Activity implements OnClickListener {

	/** Messenger for communicating with the service. */
	Messenger mService = null;

	/** Flag indicating whether we have called bind on the service. */
	boolean mBound;

	private Options opts;

	ToggleButton btToggleOn;

	SeekBar bar;

	TextView tvSensitive;

	CheckBox btCheckOn, btCheckSound;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		btCheckOn = (CheckBox) findViewById(R.id.btCheckOn);
		btCheckOn.setTextSize(21);
		btCheckOn.setOnClickListener(this);

		btCheckSound = (CheckBox) findViewById(R.id.btCheckSound);
		btCheckSound.setTextSize(21);
		btCheckSound.setOnClickListener(this);

		bar = (SeekBar) findViewById(R.id.seekBarSensitive);
		bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				opts.sensitive = Math.round(seekBar.getProgress() * 210 / 100);
				bindOptions();
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				tvSensitive.setText("Sensitive Value: " + progress);
				
			}
		});

		// make text label for progress value
		tvSensitive = (TextView) findViewById(R.id.tvSensitive);

		opts = Options.getInstance();

		// Restore preferences
		getOptions();

		if (isMyServiceRunning(BackgroundShake.class))
			opts.isStart = true;
		else if (opts.isStart) {
			_startJob();
		} else
			opts.isStart = false;

		btCheckOn.setChecked(opts.isStart);

		btCheckSound.setChecked(opts.notifSound);

		bar.setProgress(Math.round(opts.sensitive * 100 / 210));

		if (Options._debug)
			Log.d("Background_shake", "MainActivity Started.");
	}

	public void getOptions() {
		// Restore preferences
		SharedPreferences settings = getSharedPreferences(opts.PREFS_NAME, 0);
		opts.isStart = settings.getBoolean("isStart", false);
		opts.notifSound = settings.getBoolean("notifSound", false);
		opts.sensitive = settings.getInt("sensitivePercent", opts.sensitive);
	}

	public void saveOptions() {
		SharedPreferences settings = getSharedPreferences(opts.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("isStart", opts.isStart);
		editor.putBoolean("notifSound", opts.notifSound);
		editor.putInt("sensitive", opts.sensitive);

		// Commit the edits!
		editor.commit();
	}

	private boolean isMyServiceRunning(Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	protected void onStop() {
		super.onStop();

		saveOptions();

		// Unbind from the service
		unbindSvc();
	}

	protected void unbindSvc() {
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
	}

	protected void onResume() {
		super.onResume();
		if (Options._debug)
			Log.d("Background_shake", "on Resume");
	}

	protected void onPause() {
		saveOptions();
		super.onPause();
		if (Options._debug)
			Log.d("Background_shake", "on Pause");
	}

	private void _startJob() {
		if (!isMyServiceRunning(BackgroundShake.class)) {
			// Bind to the service
			bindService(new Intent(this, BackgroundShake.class), mConnection,
					Context.BIND_AUTO_CREATE);
			startService(new Intent(this, BackgroundShake.class));
		}
	}

	private void _stopJob() {
		if (Options._debug)
			Log.d("Background_shake", "Try to Stop service");

		if (isMyServiceRunning(BackgroundShake.class)) {
			// Unbind from the service
			unbindSvc();
			stopService(new Intent(this, BackgroundShake.class));
		}
	}

	@Override
	public void onClick(View src) {
		switch (src.getId()) {
		case R.id.btCheckOn:
			if (btCheckOn.isChecked()) {
				if (Options._debug)
					Log.d("Background_shake", "Call start");
				opts.isStart = true;
				_startJob();
			} else {
				if (Options._debug)
					Log.d("Background_shake", "Call stop");
				opts.isStart = false;
				_stopJob();
			}
			break;
		case R.id.btCheckSound:
			opts.notifSound = btCheckSound.isChecked();
			bindOptions();
			break;
		}
	}

	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the object we can use to
			// interact with the service. We are communicating with the
			// service using a Messenger, so here we get a client-side
			// representation of that from the raw IBinder object.
			mService = new Messenger(service);
			mBound = true;
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;
			mBound = false;
		}
	};

	public void bindOptions() {
		if (!mBound)
			return;
		
		if (Options._debug)
			Log.d("Background_shake", "Bindservice- send Msg");
		
		Message msg = Message.obtain(null, 1, opts);
		try {
			mService.send(msg);
		} catch (RemoteException e) {
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (Options._debug)
			Log.d("Background_shake", "on Start");
	}

}