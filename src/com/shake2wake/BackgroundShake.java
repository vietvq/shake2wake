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

import java.io.IOException;
import java.util.ArrayList;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class BackgroundShake extends Service implements SensorEventListener {

	final Messenger mMessenger = new Messenger(new IncomingHandler());

	public static final int SCREEN_OFF_RECEIVER_DELAY = 1500;

	// private float mLastX, mLastY, mLastZ;
	// private boolean mInitialized;
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	// private final float NOISE = (float) 0.5;
	private PowerManager pm;
	private WakeLock wakeLock;
	private Options opts;
	private Context mContext;

	private MediaPlayer mPlayer;

	final Handler handler = new Handler();
	private static Runnable runnable;

	// private long lastUpdate;

	private ArrayList<MovePoint> movePoints = new ArrayList<MovePoint>();

	public int deltaTime = 105, sensiPercent = 70, x_left_min = 59,
			x_left_max = 76, x_right_min = 99, x_right_max = 116, z_low = 25,
			z_high = 155, max_steps = 5, shake_rate = 1;

	static final boolean _debug = false;

	// BroadcastReceiver for handling ACTION_SCREEN_OFF.
	public BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				if (Options._debug)
					Log.d("Background_shake", "Screen Off");
				
				runnable = new Runnable() {
					public void run() {
						if (Options._debug)
							Log.d("Background_shake", "Runnable is running");
		
						mSensorManager.unregisterListener(BackgroundShake.this);
						mSensorManager.registerListener(BackgroundShake.this,
								mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		
					}
				};
		
				handler.postDelayed(runnable, SCREEN_OFF_RECEIVER_DELAY);

			} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				if (Options._debug)
					Log.d("Background_shake", "Screen On");
			}
		}
	};

	/**
	 * When binding to the service, we return an interface to our messenger for
	 * sending messages to the service.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		// Toast.makeText(getApplicationContext(), "binding",
		// Toast.LENGTH_SHORT).show();
		return mMessenger.getBinder();
	}

	// @Override
	// public IBinder onBind(Intent intent) {
	//
	// return null;
	// }

	public void onCreate() {
		if (Options._debug)
			Log.d("Background_shake", "onCreate");

		// Toast.makeText(getApplicationContext(), "Service Started",
		// Toast.LENGTH_SHORT).show();

		super.onCreate();

		mContext = getApplicationContext();
		// Obtain a reference to system-wide sensor event manager.
		mSensorManager = (SensorManager) mContext
				.getSystemService(Context.SENSOR_SERVICE);

		// Get the default sensor for accel
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		// Register for events.
		mSensorManager.registerListener(this, mAccelerometer,
				SensorManager.SENSOR_DELAY_NORMAL);

		// Register our receiver for the ACTION_SCREEN_OFF action. This will
		// make our receiver
		// code be called whenever the phone enters standby mode.
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		registerReceiver(mReceiver, filter);

	}

	public void onDestroy() {
		if (Options._debug)
			Log.d("Background_shake", "onDestroy");
		super.onDestroy();

		mSensorManager.unregisterListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY));

		mSensorManager.unregisterListener(this);

		unregisterReceiver(mReceiver);
		
		handler.removeCallbacks(runnable);
	}

	public void getOptions() {

		// Restore preferences
		SharedPreferences settings = getSharedPreferences(opts.PREFS_NAME, 0);
		opts.isStart = settings.getBoolean("isStart", false);
		opts.notifSound = settings.getBoolean("notifSound", false);
		opts.sensitive = settings.getInt("sensitive", opts.sensitive);
		calcRate();
	}

	public void calcRate() {
		deltaTime = opts.sensitive + 120;
		sensiPercent = opts.sensitive * 100 / 210;
		max_steps = (int) (sensiPercent / 10) + 3;
		x_left_min = 69 - max_steps;
		x_left_max = 69 + max_steps;
		x_right_min = 108 - max_steps;
		x_right_max = 108 + max_steps;
		z_low = 25 - (int) (sensiPercent / 10);
		z_high = 155 + (int) (sensiPercent / 10);
		shake_rate = 10 - (int) (sensiPercent / 10);
	}

	@SuppressWarnings("deprecation")
	public void onStart(Intent intent, int startId) {
		// mInitialized = false;
		// mSensorManager = (SensorManager)
		// getSystemService(Context.SENSOR_SERVICE);
		// mAccelerometer = mSensorManager
		// .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		// mSensorManager.registerListener(this, mAccelerometer,
		// SensorManager.SENSOR_DELAY_NORMAL);
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm
				.newWakeLock(
						(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
								| PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP),
						"Wakeshake");

		mPlayer = MediaPlayer.create(BackgroundShake.this, R.raw.unlock);

		opts = Options.getInstance();

		getOptions();

		// Test
//		runnable = new Runnable() {
//			public void run() {
//				if (Options._debug)
//					Log.d("Background_shake", "Runnable is running");
//
//				mSensorManager.unregisterListener(BackgroundShake.this);
//				mSensorManager.registerListener(BackgroundShake.this,
//						mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
//
//				handler.postDelayed(this, SCREEN_OFF_RECEIVER_DELAY);
//			}
//		};
//
//		handler.postDelayed(runnable, SCREEN_OFF_RECEIVER_DELAY);

		if (Options._debug)
			Log.d("Background_shake", "Service onStart");
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	public void onSensorChanged(SensorEvent event) {

		/* Test */
		float[] g = new float[3];
		g = event.values.clone();

		double norm_Of_g = Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]);

		int x = 0;// (int) Math.round(Math.toDegrees(Math.acos(g[0] /
					// norm_Of_g)));
		int y = 0;// (int) Math.round(Math.toDegrees(Math.acos(g[1] /
					// norm_Of_g)));
		int z = (int) Math.round(Math.toDegrees(Math.acos(g[2] / norm_Of_g)));

		/* Endtest */

		// if (Options._debug)
		// Log.d("Background_shake", x + " | " + y + " | "
		// + z);

		// if (!mInitialized) {
		// mLastX = x;
		// mLastY = y;
		// mLastZ = z;
		// mInitialized = true;
		// lastUpdate = System.currentTimeMillis();
		// } else {
		// float deltaX = mLastX - x; // Math.abs(mLastX - x);
		// float deltaY = mLastY - y; // Math.abs(mLastY - y);
		// float deltaZ = mLastZ - z; // Math.abs(mLastZ - z);
		// if (Math.abs(deltaX) < NOISE)
		// deltaX = (float) 0.0;
		// if (Math.abs(deltaY) < NOISE)
		// deltaY = (float) 0.0;
		// if (Math.abs(deltaZ) < NOISE)
		// deltaZ = (float) 0.0;
		// mLastX = x;
		// mLastY = y;
		// mLastZ = z;
		// }

		// long currentTime = System.currentTimeMillis();

		// if (currentTime - lastUpdate > deltaTime)
		// movePoints.clear();
		//
		// lastUpdate = currentTime;

		if (checkMoves(new MovePoint(x, y, z))) {
			wakeLock.acquire();
			wakeLock.release();
		}

		/*
		 * if (deltaX > deltaY) { // Horizon moving } else if (deltaY > deltaX)
		 * { // Vertical moving } else { // No moving }
		 */

	}

	private boolean checkMoves(MovePoint pAdd) {

		if (pm.isScreenOn() && !Options._debug)
			return false;

		movePoints.add(pAdd);

		// Check count
		if (movePoints.size() > max_steps) {
			movePoints.remove(0);
		}

		boolean is_lift = false, _is_was_flat = false, is_was_shaked = false;
		// int x_left = 0, x_right = 0;

		// Check Sign
		for (MovePoint point : movePoints) {

			// if (point.x > x_left_min && point.x < x_left_max) {
			// x_left++;
			// } else if (point.x > x_right_min && point.x < x_right_max) {
			// x_right++;
			// }

			if (point.z < z_low || point.z > z_high) {
				// Flat state
				_is_was_flat = true;
				// if (Options._debug)
				// Log.d("Background_shake", point.x + " | " + point.y + " | "
				// + point.z + " ::::: Was Flat");
				continue;
			} else if (_is_was_flat) {
				is_lift = true;
				if (Options._debug)
					Log.d("Background_shake", point.x + " | " + point.y + " | "
							+ point.z + " ::::: Was lift");
				break;
			}

		}

		// if (x_left > shake_rate && x_right > shake_rate) {
		// is_was_shaked = true;
		// if (Options._debug)
		// Log.d("Background_shake", "Was shaked, rate: " + shake_rate);
		// }

		if (is_lift || is_was_shaked) {
			if (opts.notifSound) {
				try {
					mPlayer.start();
				} catch (Exception e) {
				}
			}

			movePoints.clear();
			return true;
		} else
			return false;
	}

	protected void onResume() {
		// super.onResume();

		mSensorManager.registerListener(this, mAccelerometer,
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	protected void onPause() {
		// unregister listener
		mSensorManager.unregisterListener(this);

	}

	protected void onStop() {
		mSensorManager.unregisterListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY));
	}

	/**
	 * Handler of incoming messages from clients.
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {

			if (Options._debug)
				Log.d("Background_shake", "Service: Receive msg");

			switch (msg.what) {
			case 1:
				opts = (Options) msg.obj;
				calcRate();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		// startForeground(Process.myPid(), new Notification());

		return super.onStartCommand(intent, flags, startId);
		// return START_STICKY;
	}

}