package me.jmhend.PinchListView;

import java.util.ArrayList;
import java.util.List;

import me.jmhend.PinchListView.R;
import me.jmhend.PinchListView.PinchListView.OnItemPinchedListener;
import me.jmhend.PinchListView.PinchListView.PinchState;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Activity that demonstrates usage of PinchListView.
 * @author jmhend
 *
 */
public class MainActivity extends Activity {
	
	private static final String TAG = MainActivity.class.getSimpleName();
	
////=====================================================================================
//// Member variables.
////=====================================================================================
	
	private PinchListView mListView;
	private SimplePinchAdapter mListAdapter;
	
////=====================================================================================
//// Activity lifecycle.
////=====================================================================================

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Populate some data to put in the PinchListView.
		List<PinchItem> pinchItems = new ArrayList<PinchItem>();
		for (int i = 0; i < 3000; i++) {
			final boolean isPinchable = (i % 3 == 0) || (i % 4 == 0); // "randomize" which cells are pinchable.
			String text = isPinchable ? "Pinch me!" : "Don't even think about it.";
			pinchItems.add(new PinchItem(text, isPinchable));
		}
		
		mListView = (PinchListView) findViewById(R.id.pinch_list);
		mListAdapter = new SimplePinchAdapter(mListView, this, pinchItems);
		mListView.setAdapter(mListAdapter);
		
		// Add an OnItemPinchedListener 
		mListView.setOnItemPinchedListener(new OnItemPinchedListener() {
			/*
			 * (non-Javadoc)
			 * @see me.jmhend.ui.pinchlist.PinchListView.OnItemPinchedListener#onViewPinched(me.jmhend.ui.pinchlist.PinchListView, android.view.View, int, float)
			 */
			@Override
			public void onItemPinched(PinchListView listView, View view, int newHeight, float heightPercent) { 
				// Adjust the TextView alpha based on how the cells have been pinched.
				((TextView) view.findViewById(R.id.text)).setAlpha(heightPercent);
			}
		});
		
		mListView.setOnItemClickListener(new OnItemClickListener() {
			/*
			 * (non-Javadoc)
			 * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
			 */
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				((ListView) parent).smoothScrollToPosition(30);
			}
			
		});
	}
	
////=====================================================================================
//// PinchItem
////=====================================================================================
	
	/**
	 * Example model to use as the PinchListView's datatype.
	 * 
	 * @author jmhend
	 *
	 */
	private static class PinchItem {
		public final String text;
		public final boolean collapsable;

		public PinchItem(String text, boolean collapsable) {
			this.text = text;
			this.collapsable = collapsable;
		}
	}

////=====================================================================================
//// MyPinchListAdapter
////=====================================================================================
	
	/**
	 * Example implemenation of PinchListAdapter.
	 * 
	 * @author jmhend
	 */
	private static class SimplePinchAdapter extends ArrayAdapter<PinchItem> implements PinchAdapter {
		
		private static final int PINCHABLE_COLOR = 0x22006622;
		private static final int NONPINCHABLE_COLOR = 0x22FFFFFF;
		
		private List<PinchItem> mPinches;
		private PinchListView mListView;
		
		/**
		 * Default constructor.
		 * @param context
		 * @param objects
		 */
		public SimplePinchAdapter(PinchListView listView, Context context, List<PinchItem> objects) {
			super(context, 0, objects);
			mPinches = objects;
			mListView = listView;
		}
	
		/*
		 * (non-Javadoc)
		 * @see me.jmhend.ui.pinchlist.PinchListAdapter#isRowCollapsable(int)
		 */
		@Override
		public boolean isRowPinchable(int position) {
			return mPinches.get(position).collapsable;
		}
	
		/*
		 * (non-Javadoc)
		 * @see me.jmhend.ui.pinchlist.PinchListAdapter#provideView(int, android.view.View, android.view.ViewGroup)
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			// Typical Adapter View recycling.
			ViewHolder holder;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.listitem_pinch, parent, false);
				holder.textView = (TextView) convertView.findViewById(R.id.text);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			final PinchItem pinch = mPinches.get(position);
			
			// Set the alpha of the cell's TextView based upon the state of the pinched items.
			boolean isCollapsed = isRowPinchable(position) && mListView.getPinchState() == PinchState.COLLAPSED;
			holder.textView.setAlpha(isCollapsed ? 0.0f : 1.0f);
			holder.textView.setText(pinch.text);

			// Set the background color of pinchable cells.
			final int background = (isRowPinchable(position)) ? PINCHABLE_COLOR : NONPINCHABLE_COLOR;
			convertView.setBackgroundColor(background);
			
			// REQUIRED.
			// Handles setting the height of each cell correctly.
			// Call this right before returning.
			mListView.adjustCellHeight(convertView, position);
			
			return convertView;
		}
		
		private static final class ViewHolder {
			private TextView textView;
		}
	}
}
