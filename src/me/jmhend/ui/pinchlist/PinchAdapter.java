package me.jmhend.ui.pinchlist;

/**
 * Interface for a ListAdapter to implement.
 * Tells the PinchListView which rows to manipulate.
 * 
 * @author jmhend
 *
 */
public interface PinchAdapter {

	/**
	 * True if the child View at 'position' is pinchable.
	 * i.e, it responds and animates to pinch events.
	 * 
	 * @param position
	 * @return
	 */
	public boolean isRowPinchable(int position);
}
