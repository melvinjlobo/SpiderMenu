package com.abysmel.spidermenu;

import android.util.Log;



/**
 * Created by Melvin Lobo on 12/10/2015.
 *
 * Class for conditional logging. Uses Android Log internally
 */
public class Logger {

	////////////////////////////////////// CLASS MEMBERS ///////////////////////////////////////////
	private static boolean mDevModeEnabled = true;
	private static final String TAG = "SpiderMenu";

	////////////////////////////////////// CLASS METHODS ///////////////////////////////////////////
	public static void e(String msg) {
		e(TAG, msg);
	}

	public static void e(String tag, String msg) {
		if (mDevModeEnabled) Log.e(tag, msg);
	}

	public static void w(String msg) {
		w(TAG, msg);
	}

	public static void w(String tag, String msg) {
		if (mDevModeEnabled) Log.w(tag, msg);
	}

	public static void i(String msg) {
		i(TAG, msg);
	}

	public static void i(String tag, String msg) {
		if (mDevModeEnabled) Log.i(tag, msg);
	}

	public static void d(String msg) {
		d(TAG, msg);
	}

	public static void d(String tag, String msg) {
		if (mDevModeEnabled) Log.d(tag, msg);
	}

	public static void v(String msg) {
		v(TAG, msg);
	}

	public static void v(String tag, String msg) {
		if (mDevModeEnabled) Log.v(tag, msg);
	}

	public static void enable() {
		mDevModeEnabled = true;
	}

	public static void disable() {
		mDevModeEnabled = false;
	}

	public static boolean isEnabled() {
		return mDevModeEnabled;
	}
}
