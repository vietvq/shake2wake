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

public class Options {
	private static Options mInstance = null;
	public String PREFS_NAME = "wakeShakes";

	public boolean notifSound = false;
	public boolean isStart = false;
	public int sensitive = 105;
	
	static final boolean _debug = false;

	public Options() {
	}

	public static synchronized Options getInstance() {
		if (null == mInstance) {
			mInstance = new Options();
		}
		return mInstance;
	}

//	public void loadSettings() {
//		// Restore preferences
//		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
//		isStart = settings.getBoolean("isStart", false);
//	}
//
//	public void saveSettings() {
//		
//		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
//		SharedPreferences.Editor editor = settings.edit();
//		editor.putBoolean("isStart", isStart);
//
//		// Commit the edits!
//		editor.commit();
//	}
}
