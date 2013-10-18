package me.jmhend.PinchListView;

import android.content.Context;
import android.util.TypedValue;

/**
 * Utility methods for Pinching.
 * 
 * @author jmhend
 *
 */
public class PinchUtils {
	
	/**
	 * @param dp
	 * @param context
	 * @return The value of 'dp' in pixels.
	 */
	public static int dpToPx(int dp, Context context) {
		return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5);
	}
}
