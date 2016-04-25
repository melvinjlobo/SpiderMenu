package com.abysmel.spidermenu;

import android.content.Context;
import android.util.TypedValue;

/**
 * Created by Melvin Lobo on 4/18/2016. Common Utilities
 */
public class Util {
	/**
	 * Convert dip to pixels
	 *
	 * param size The size to be converted
	 *
	 * @author Melvin Lobo
	 */
	public static float d2x( Context context, int size) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, context.getResources().getDisplayMetrics());
	}
}
