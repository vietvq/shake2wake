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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class MainReceiver extends BroadcastReceiver {
	private Options opts;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		opts = Options.getInstance();
		SharedPreferences settings = context.getSharedPreferences(opts.PREFS_NAME, 0);
		opts.isStart = settings.getBoolean("isStart", false);
		opts.notifSound = settings.getBoolean("notifSound", false);
		opts.sensitive = settings.getInt("sensitivePercent", opts.sensitive);
		
		if (opts.isStart)
		{
			Intent startServiceIntent = new Intent(context, BackgroundShake.class);
			context.startService(startServiceIntent);
		}
	}
}