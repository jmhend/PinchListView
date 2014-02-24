package me.jmhend.PinchListView;


import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * ListView that supports expanding and collapsing certain cells via pinching gestures.
 * 
 * @author jmhend
 *
 */
public class PinchListView extends ListView  {
	
	private static final String TAG = PinchListView.class.getSimpleName();
	
////=========================================================================================
//// Static constants.
////=========================================================================================
	
	private static final int DEFAULT_HEIGHT_EXPANDED_DP = 80;
	private static final int DEFAULT_HEIGHT_COLLAPSED_DP = 2;
	
////=========================================================================================
//// PinchState
////=========================================================================================
	
	/**
	 * Describes the current state of the pinchable cells.
	 * @author jmhend
	 *
	 */
	public static enum PinchState {
		COLLAPSING,
		COLLAPSED,
		EXPANDING,
		EXPANDED,
	}
	
////=========================================================================================
//// Member variables.
////=========================================================================================
	
	private PinchHandler mPinchHandler;
	private ScaleGestureDetector mScaleDetector;
	private PinchAdapter mPinchAdapter;
	private List<OnItemPinchListener> mPinchListeners = new ArrayList<OnItemPinchListener>();
	private OnPinchCompleteListener mPinchCompleteListener;
	
	private int mExpandedHeight;
	private int mCollapsedHeight;
	private int mPinchHeight;
	
	private boolean mPinchable = true;

////=========================================================================================
//// Constructor.
////=========================================================================================

	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public PinchListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	/**
	 * @param context
	 * @param attrs
	 */
	public PinchListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	/**
	 * @param context
	 */
	public PinchListView(Context context) {
		super(context);
		init();
	}
	
////=========================================================================================
//// Init
////=========================================================================================

	/**
	 * Common init.
	 */
	private void init() {
		mExpandedHeight = PinchUtils.dpToPx(DEFAULT_HEIGHT_EXPANDED_DP, getContext());
		mCollapsedHeight = PinchUtils.dpToPx(DEFAULT_HEIGHT_COLLAPSED_DP, getContext());
		mPinchHeight = mCollapsedHeight;
		mPinchHandler = new PinchHandler();
		mScaleDetector = new ScaleGestureDetector(getContext(), mPinchHandler);
	}
	
////=========================================================================================
//// ListAdapter
////=========================================================================================
	
	/*
	 * (non-Javadoc)
	 * @see android.widget.ListView#setAdapter(android.widget.ListAdapter)
	 */
	@Override
    public void setAdapter(ListAdapter adapter) {
		if (!(adapter instanceof PinchAdapter)) {
			throw new IllegalArgumentException("Cannot use PinchListView with " + adapter.getClass().getCanonicalName() + "!");
		}
		mPinchAdapter = (PinchAdapter) adapter;
		super.setAdapter(adapter);
	}
	
////=========================================================================================
//// Dimensions
////=========================================================================================
	
	/**
	 * @return The current PinchState the PinchListView is in.
	 */
	public PinchState getPinchState() {
		if (mPinchHeight == mCollapsedHeight) {
			return PinchState.COLLAPSED;
		}
		if (mPinchHeight == mExpandedHeight) {
			return PinchState.EXPANDED;
		}
		if (mPinchHandler.isExpanding) {
			return PinchState.EXPANDING;
		}
		return PinchState.COLLAPSING;
	}
	
	/**
	 * Adjusts the height of View 'view' at child index 'position'.
	 * @param view
	 * @param position
	 */
	public void adjustCellHeight(View view, int position) {
		final int height = (mPinchable && mPinchAdapter.isRowPinchable(position)) ? getPinchHeight() : getExpandedHeight(position);
		ViewGroup.LayoutParams params = view.getLayoutParams();
		if (params.height != height) {
			params.height = height;
			view.setLayoutParams(params);
		}
	}
	
	/**
	 * @return True if the ListView adjusts to pinch gestures.
	 */
	public boolean isPinchable() {
		return mPinchable;
	}
	
	/**
	 * @param pinchable True if the ListView adjusts to pinch gestures.
	 */
	public void setPinchable(boolean pinchable) {
		mPinchable = pinchable;
	}
	
	/**
	 * @param context
	 * @return the height, in pixels, of a pinchable cell when it's fully expanded.
	 */
	public int getExpandedHeight(int position) {
		return mExpandedHeight;
	}
	
	/**
	 * Sets the height, in pixels, of a pinchable cell when it's full expanded.
	 * @param expandedHeight
	 */
	public void setExpandedHeightInPx(int expandedHeight) {
		mExpandedHeight = expandedHeight;
		mPinchHandler.setMaxHeight(expandedHeight);
	}
	
	/**
	 * @param context
	 * @return the height, in pixels, of a pinchable cell when it's fully collapsed.
	 */
	public int getCollapsedHeight() {
		return mCollapsedHeight;
	}
	
	/**
	 * Sets the height, in pixels, of a pinchable cell when it's fully collapsed.
	 * @param collapsedHeight
	 */
	public void setCollapsedHeightInPx(int collapsedHeight) {
		mCollapsedHeight = collapsedHeight;
	}
	
	/**
	 * @return The current height of pinchable rows.
	 */
	public int getPinchHeight() {
		return mPinchHeight;
	}
	
	/**
	 * @param height The current height of pinchable rows.
	 */
	public void setPinchHeight(int height) {
		mPinchHeight = height;
	}
	
	/**
	 * @return The fraction of max height that the pinchable rows are expanded.
	 */
	public float getCellHeightPercentage() {
		return calculateHeightPercentage(mPinchHeight, mExpandedHeight, mCollapsedHeight);
	}
	
	/**
	 * Calculates the percentage of total height 'height' is between
	 * the minimum and maximum heights.
	 * @param height
	 * @return
	 */
	public static float calculateHeightPercentage(int height, int max, int min) {
		return ((float) height - min) / ((float) (max - min));
	}
	
	/**
	 * Animates the PinchListView fully open.
	 */
	public void animateExpanded() {
		mPinchHandler.animateExpanded();
	}
	
	/**
	 * Aniamtes the PinchListView fully closed.
	 */
	public void animateCollapsed() {
		mPinchHandler.animateCollapsed();
	}
	
////=========================================================================================
//// Touch Events
////=========================================================================================
	
	/*
	 * (non-Javadoc)
	 * @see android.widget.AbsListView#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (mPinchable) {
			mScaleDetector.onTouchEvent(ev);
		}
		return super.onTouchEvent(ev);
	}
	
////=========================================================================================
//// OnViewPinchedListener
////=========================================================================================
	
	/**
	 * Listens for pinch actions on PinchListView children rows.
	 * 
	 * @author jmhend
	 */
	public static interface OnItemPinchListener {
		
		/**
		 * Called when a pinch action is taken on the PinchListView.
		 * Called for each pinched child.
		 * @param listView
		 * @param view
		 * @param newHeight
		 * @param heightPercent
		 */
		public void onItemPinch(PinchListView listView, View view, int newHeight, float heightPercent);
	}
	
	/**
	 * @param listener The OnItemPinchListener to callback to.
	 */
	public void addOnItemPinchListener(OnItemPinchListener listener) {
		mPinchListeners.add(listener);
	}
	
	/**
	 * Listens for a pinch action or animation to complete.
	 * @author jmhend
	 *
	 */
	public static interface OnPinchCompleteListener {
		
		/**
		 * Called when the pinch action or animation is completed.
		 * @param listView
		 * @param endingState
		 */
		public void onPinchComplete(PinchListView listView, PinchState endingState);
	}
	
	/**
	 * @param listener
	 */
	public void setOnPinchCompleteListener(OnPinchCompleteListener listener) {
		mPinchCompleteListener = listener;
	}
	
	
////=========================================================================================
//// PinchListener
////=========================================================================================
	
	/**
	 * Handles callbacks for OnScaleGestureListener callbacks.
	 * Contains the majority of the logic for View transformations and 
	 * animations based on pinch actions.
	 * 
	 * @author jmhend
	 *
	 */
	private class PinchHandler implements ScaleGestureDetector.OnScaleGestureListener {

		/**
		 * Duration of a full expand/collapse pinch animation.
		 */
		private static final long ANIMATE_DURATION_MILLIS = 200; 
		
		/**
		 * True if the pinch action is expanding the cell, false if it is collapsing the cell.
		 */
		private boolean isExpanding;
		
		/**
		 * Max height of a pinchable cell.
		 */
		private int maxHeight;
		
		/**
		 * Min height of a pinchable cell.
		 */
		private int minHeight;
		
		/**
		 * Maximum distance a cell height can be pinched.
		 */
		private int maxPinchDistance;
		
		/**
		 * Minimum distance a cell height can be pinched.
		 */
		private final int minPinchDistance = 0;
		
	////====================================================================================
	//// Constructor.
	////====================================================================================
		
		/**
		 * Empty constructor.
		 */
		public PinchHandler() {
			maxHeight = getExpandedHeight(0);
			minHeight = getCollapsedHeight();
			maxPinchDistance = maxHeight - minHeight;
		}
		
		/**
		 * @param maxHeight Maximum expansion height.
		 */
		private void setMaxHeight(int maxHeight) {
			this.maxHeight = maxHeight;
		}
		
	////====================================================================================
	//// OnScaleGestureListener callbacks.
	////====================================================================================

		/*
		 * (non-Javadoc)
		 * @see android.view.ScaleGestureDetector.OnScaleGestureListener#onScale(android.view.ScaleGestureDetector)
		 */
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			final int currentHeight = getPinchHeight();
			final float scalingFactor = 1 + ((detector.getScaleFactor() - 1) * 8);
			
			// Check the pinch direction.
			final boolean currentlyExpanding = scalingFactor > 1.0f;
			if (currentlyExpanding != isExpanding) {
				isExpanding = currentlyExpanding;
			}
			
			// Calculate new cell height
			int newHeight = ((int) (currentHeight * scalingFactor));

			// Make sure the height changes, even if the scale is too small to affect integer changes.
			if (newHeight == currentHeight) {
				if (scalingFactor > 1.0f) {
					newHeight++;
				} else {
					newHeight--;
				}
			}
			
			// Verify height is in bounds.
			if (newHeight > maxHeight) {
				newHeight = maxHeight;
			} else if (newHeight < minHeight) {
				newHeight = minHeight;
			}
			
			// Set new height.
			setPinchHeight(newHeight);
			setChildrenHeight(newHeight);
			return true;
		}

	
		/*
		 * (non-Javadoc)
		 * @see android.view.ScaleGestureDetector.OnScaleGestureListener#onScaleBegin(android.view.ScaleGestureDetector)
		 */
		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			return true;
		}
	
		/*
		 * (non-Javadoc)
		 * @see android.view.ScaleGestureDetector.OnScaleGestureListener#onScaleEnd(android.view.ScaleGestureDetector)
		 */
		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			int height = calcTargetHeight();
			long duration = calcAnimationDuration(getPinchHeight(), height);
			setPinchHeight(height);
			animateChildrenHeight(height, duration);
		}
		
		/**
		 * Animates each pinchable cell fully open.
		 */
		public void animateExpanded() {
			animateHeightTo(maxHeight);
		}
		
		/**
		 * ANimates each pinchable cell fully closed.
		 */
		public void animateCollapsed() {
			animateHeightTo(minHeight);
		}
		
		/**
		 * Animates each pinchable cell to 'height'.
		 * @param height
		 */
		private void animateHeightTo(int height) {
			long duration = calcAnimationDuration(getPinchHeight(), height);
			setPinchHeight(height);
			animateChildrenHeight(height, duration);
		}
		
	////====================================================================================
	//// View
	////====================================================================================
		
		/**
		 * Sets the height of all visible pinchable children to 'height'.
		 * @param height
		 */
		private void setChildrenHeight(int height) {
			for (int i = 0; i < getChildCount(); i++) {
				View child = getChildAt(i);
				int position = i + getFirstVisiblePosition();
				if (position >= getCount() - getFooterViewsCount()) {
					continue;
				}
				if (mPinchAdapter.isRowPinchable(position)) {
					child.getLayoutParams().height = height;
					child.requestLayout();
					
					final float newHeightPercent = calculateHeightPercentage(height, mExpandedHeight, mCollapsedHeight);
					for (OnItemPinchListener l : mPinchListeners) {
						l.onItemPinch(PinchListView.this, child, height, newHeightPercent);
					}
				}
			}
		}
		
		/**
		 * Animates the height of all visible pinchable children to 'height'.
		 */
		private void animateChildrenHeight(int height, long duration) {
			boolean isFirst = true;
			
			for (int i = 0; i < getChildCount(); i++) {
				View child = getChildAt(i);
				int position = i + getFirstVisiblePosition();
				if (mPinchAdapter.isRowPinchable(position)) {
					
					// Only create 1 listener.
					AnimationListener l = null;
					if (isFirst) {
						isFirst = false;
						l = new AnimationListener() {
							/*
							 * (non-Javadoc)
							 * @see android.view.animation.Animation.AnimationListener#onAnimationEnd(android.view.animation.Animation)
							 */
							@Override
							public void onAnimationEnd(Animation animation) {
								if (mPinchCompleteListener != null) {
									mPinchCompleteListener.onPinchComplete(PinchListView.this, getPinchState());
								}
							}
							@Override
							public void onAnimationStart(Animation animation) { }
							@Override
							public void onAnimationRepeat(Animation animation) { }
							
						};
					}
					PinchAnimation.withView(child).toHeight(height).withListener(l).go(duration);
				}
			}
		}
		
	////====================================================================================
	//// Maths.
	////====================================================================================
		
		/**
		 * @return The cell height each child should animate to.
		 */
		private int calcTargetHeight() {
			float heightFraction = getCellHeightPercentage();
			if (heightFraction > 0.85f) {
				return maxHeight;
			}
			if (heightFraction < 0.15f) {
				return minHeight;
			}
			return isExpanding ? maxHeight : minHeight;
		}
		
		/**
		 * @return The length of the animation duration.
		 */
		private long calcAnimationDuration(int currentHeight, int targetHeight) {
			final int distance = Math.abs(targetHeight - currentHeight);
			float percent = ((float) (distance - minPinchDistance)) / ((float) (maxPinchDistance - minPinchDistance));
			long duration = Math.max((long) (percent * ANIMATE_DURATION_MILLIS), 1L);
			return duration;
		}
	}
	
////=========================================================================================
//// Animation
////=========================================================================================
	
	/**
	 * Animates the collapsing/expanding of a View.
	 * 
	 * @author jmhend
	 *
	 */
	public static class PinchAnimation extends Animation {
		private final View view;
		private final PinchListView parent;
		private final int startHeight;
		private final int endHeight;
		private final boolean willChangeHeight;
		private List<OnItemPinchListener> mItemPinchListeners;
		
		/**
		 * Constructor.
		 */
		public PinchAnimation (View view, int startHeight, int endHeight) {
			this.view = view;
			this.parent = (PinchListView) view.getParent();
			this.startHeight = startHeight;
			this.endHeight = endHeight;
			this.willChangeHeight = startHeight != endHeight;
			this.mItemPinchListeners = parent.mPinchListeners;
		}
		
		/**
		 * Starts building a CollapseAnimation.
		 * @param view
		 * @return
		 */
		public static PinchAnimation.Builder withView(View view) {
			return new Builder(view);
		}

		/*
		 * (non-Javadoc)
		 * @see android.view.animation.Animation#applyTransformation(float, android.view.animation.Transformation)
		 */
		@Override
		protected void applyTransformation(float interpolatedTime, Transformation t) {
			if (willChangeHeight) {
				int height = (int) (interpolatedTime * (endHeight - startHeight) + startHeight);
				view.getLayoutParams().height = height;
				view.requestLayout();
				
				final float newHeightPercent = PinchListView.calculateHeightPercentage(height, parent.mExpandedHeight, parent.mCollapsedHeight);
				for (OnItemPinchListener l : mItemPinchListeners) {
					l.onItemPinch(parent, view, height, newHeightPercent);
				}
			}
		}
		
		/*
		 * (non-Javadoc)
		 * @see android.view.animation.Animation#willChangeBounds()
		 */
		@Override
		public boolean willChangeBounds() {
			return willChangeHeight;
		}
		
		/**
		 * Builder class for creating a CollapseAnimation.
		 * @author jmhend
		 *
		 */
		public static class Builder {
			private View view;
			private int startHeight;
			private int endHeight;
			private AnimationListener listener;
			
			public Builder(View view) {
				this.view = view;
				this.startHeight = view.getLayoutParams().height;
				this.endHeight = startHeight;
			}
			
			public Builder withListener(AnimationListener listener) {
				this.listener = listener;
				return this;
			}
			
			public Builder fromHeight(int startHeight) {
				this.startHeight = startHeight;
				return this;
			}
			
			public Builder toHeight(int endHeight) {
				this.endHeight = endHeight;
				return this;
			}
			
			public Builder byHeight(int amount) {
				endHeight = startHeight + amount;
				return this;
			}
			
			public void go(long duration) {
				PinchAnimation a = new PinchAnimation(view, startHeight, endHeight);
				a.setDuration(duration);
				if (listener != null) {
					a.setAnimationListener(listener);
				}
				a.view.startAnimation(a);
			}
		}
		
	}
}
