package me.jmhend.PinchListView;


import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import me.jmhend.PinchListView.IScaleGestureDetector.IOnScaleGestureListener;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

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
	
	private static final int DEFAULT_GROUPING_VICINITY = DEFAULT_HEIGHT_EXPANDED_DP / 3;
	
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
	private IScaleGestureDetector mScaleDetector;
	private PinchAdapter mPinchAdapter;
	private List<OnItemPinchListener> mPinchListeners = new ArrayList<OnItemPinchListener>();
	private OnPinchCompleteListener mPinchCompleteListener;
	
	private int mExpandedHeight;
	private int mCollapsedHeight;
	private int mPinchHeight;
	private int mGroupingVicinityThreshold;
	
	private boolean mLockListView = false;
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
		mGroupingVicinityThreshold = PinchUtils.dpToPx(DEFAULT_GROUPING_VICINITY, getContext());
		mPinchHeight = mCollapsedHeight;
		mPinchHandler = new PinchHandler();
		mScaleDetector = new IScaleGestureDetector(getContext(), mPinchHandler);
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
	 * Sets the height of all pinchable children rows.
	 * @param height
	 */
	public void setPinchableChildrenHeight(int height) {
		mPinchHandler.setChildrenHeight(height);
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
	
	public void pulse() {
		PinchAnimation.withPinchListView(PinchListView.this)
		.fromHeight(mCollapsedHeight)
		.toHeight(mCollapsedHeight * 3)
		.withListener(new SimpleAnimationListener() {
			/*
			 * (non-Javadoc)
			 * @see android.view.animation.Animation.AnimationListener#onAnimationEnd(android.view.animation.Animation)
			 */
			@Override
			public void onAnimationEnd(Animation animation) {
				PinchAnimation.withPinchListView(PinchListView.this)
				.fromHeight(mCollapsedHeight * 3)
				.toHeight(mCollapsedHeight)
				.withListener(new SimpleAnimationListener() {
					/*
					 * (non-Javadoc)
					 * @see android.view.animation.Animation.AnimationListener#onAnimationEnd(android.view.animation.Animation)
					 */
					@Override
					public void onAnimationEnd(Animation animation) {
						PinchAnimation.withPinchListView(PinchListView.this)
						.fromHeight(mCollapsedHeight)
						.toHeight(mCollapsedHeight * 4)
						.withListener(new SimpleAnimationListener() {
							/*
							 * (non-Javadoc)
							 * @see android.view.animation.Animation.AnimationListener#onAnimationEnd(android.view.animation.Animation)
							 */
							@Override
							public void onAnimationEnd(Animation animation) {
								PinchAnimation.withPinchListView(PinchListView.this)
								.fromHeight(mCollapsedHeight * 4)
								.toHeight(mCollapsedHeight)
								.go(100);
							}
						})
						.go(80);
					}
				})
				.go(100);
			}
		})
		.go(80);
	}
	
////=========================================================================================
//// Touch Events
////=========================================================================================
	
	/*
	 * (non-Javadoc)
	 * @see android.widget.AbsListView#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTouchEvent(final MotionEvent ev) {
		if (mPinchable) {
			mScaleDetector.onTouchEvent(ev);
		}
		
		// Lock ListView after a scale occurs until the user stops touching the screen.
		if (!mLockListView && mScaleDetector.isInProgress()) {
			mLockListView = true;
		}
		
		// Unset 
		switch (ev.getActionMasked()) {
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			mLockListView = false;
			break;
		}
		
		if (interceptTouchEvent(ev)) {
			return true;
		}
		
		return super.onTouchEvent(ev);
	}
	
	
	/**
	 * @return True if the TouchEvent should not be passed along to the super class.
	 */
	private boolean interceptTouchEvent(final MotionEvent ev) {
		if (mLockListView) {
			return true;
		}
		return false;
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
	private class PinchHandler implements IOnScaleGestureListener {

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
		public boolean onScale(IScaleGestureDetector detector) {
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
		public boolean onScaleBegin(IScaleGestureDetector detector) {
			int y = (int) detector.getFocusY();
			setAnchorView(findAnchorView(y));
			return true;
		}
	
		/*
		 * (non-Javadoc)
		 * @see android.view.ScaleGestureDetector.OnScaleGestureListener#onScaleEnd(android.view.ScaleGestureDetector)
		 */
		@Override
		public void onScaleEnd(IScaleGestureDetector detector) {
			int fromHeight = getPinchHeight();
			int toHeight = calcTargetHeight();
			long duration = calcAnimationDuration(getPinchHeight(), toHeight);
			setPinchHeight(toHeight);
			animateChildrenHeight(fromHeight, toHeight, duration);
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
		 * @param toHeight
		 */
		private void animateHeightTo(int toHeight) {
			setAnchorView(findAnchorView(PinchListView.this.getHeight() / 2));
			
			long duration = calcAnimationDuration(getPinchHeight(), toHeight);
			int fromHeight = getPinchHeight();
			setPinchHeight(toHeight);
			animateChildrenHeight(fromHeight, toHeight, duration);
		}
		
	////====================================================================================
	//// View
	////====================================================================================
		
		/**
		 * Sets the height of all visible pinchable children to 'height'.
		 * @param height
		 */
		public void setChildrenHeight(int height) {
			boolean haveAnchor = mAnchorView != null;
			boolean reachedAnchor = false;
			int anchorIndex = - 1;
			
			int heightDiff = 0;
			for (int i = 0; i < getChildCount(); i++) {
				View child = getChildAt(i);
				int position = i + getFirstVisiblePosition();
				if (position >= getCount() - getFooterViewsCount()) {
					continue;
				}
				if (child == mAnchorView) {
					anchorIndex = position;
					reachedAnchor = true;
				}
				if (mPinchAdapter.isRowPinchable(position)) {
					int oldHeight = child.getLayoutParams().height;
					if (oldHeight != height) {
						
						if (haveAnchor) {
							if (!reachedAnchor) {
								if (child != mAnchorView) {
									heightDiff += (height - oldHeight);
								}
							}
						}
						
						child.getLayoutParams().height = height;
						child.requestLayout();
						
						if (!mPinchListeners.isEmpty()) {
							final float newHeightPercent = calculateHeightPercentage(height, mExpandedHeight, mCollapsedHeight);
							for (OnItemPinchListener l : mPinchListeners) {
								l.onItemPinch(PinchListView.this, child, height, newHeightPercent);
							}
						}
					}
				}
			}
			
			
			if (haveAnchor && supportsScrollAdjusting()) {
				final int offset = heightDiff;
				final ListView list = PinchListView.this;
				list.smoothScrollBy(offset, 0);
			}
		}
		
		/**
		 * @return True if scroll adjust is supported.
		 */
		private boolean supportsScrollAdjusting() {
			return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
		}
		
		private View mAnchorView = null;
		
		private void setAnchorView(View view) {
			mAnchorView = view;
		}
		
		private View findAnchorView(int focusY) {
			View groupedView = findGroupingCenterInVicinity(focusY);
			if (groupedView != null) {
				return groupedView;
			}
			int prevChildTop = 0;
			for (int i = 0; i < getChildCount(); i++) {
				View child = getChildAt(i);
				
				int top = child.getTop();
				
				if (top > focusY && prevChildTop < focusY) {
					int prevIndex = i - 1;
					if (prevIndex < 0) {
						prevIndex = 0;
					}
					View view = getChildAt(prevIndex);
					return view;
				}
				
				prevChildTop = top;
			}
			return null;
		}
		
		/**
		 * @param focusY
		 * @return
		 */
		private View findGroupingCenterInVicinity(int focusY) {
			final int searchStart = focusY - mGroupingVicinityThreshold;
			final int searchEnd = focusY + mGroupingVicinityThreshold;
			
			int groupStartPosition = -1;
			int groupCount = 0;
			final int childCount = getChildCount();
			for (int i = 0; i < childCount; i++) {
				View child = getChildAt(i);
				
				final int top = child.getTop();
				if (top < searchStart) {
					continue;
				}
				if (top > searchEnd) {
					break;
				}
				
				final int height = child.getHeight();
				
				// Row is collapsed.
				if (height == mCollapsedHeight) {
					if (groupCount == 0) {
						groupStartPosition = i;
					}
					groupCount++;
					
				} else {
					// We're iterating over a collapsed grouping, but passed the end of it.
					if (groupCount > 0) {
						break;
					}
				}
			}
			
			if (groupCount > 0) {
				Assert.assertTrue(groupStartPosition != -1);
				int groupOffset = groupCount / 2;
				int viewPosition = groupStartPosition + groupOffset;
				return (getChildAt(viewPosition));
			}
			
			return null;
		}
		
		/**
		 * Animates the height of all visible pinchable children to 'height'.
		 */
		private void animateChildrenHeight(int fromHeight, int toHeight, long duration) {
			final AnimationListener l = new AnimationListener() {
				/*
				 * (non-Javadoc)
				 * @see android.view.animation.Animation.AnimationListener#onAnimationEnd(android.view.animation.Animation)
				 */
				@Override
				public void onAnimationEnd(Animation animation) {
					if (mPinchCompleteListener != null) {
						mPinchCompleteListener.onPinchComplete(PinchListView.this, getPinchState());
					}
					setAnchorView(null);
				}
				/*
				 * (non-Javadoc)
				 * @see android.view.animation.Animation.AnimationListener#onAnimationStart(android.view.animation.Animation)
				 */
				@Override
				public void onAnimationStart(Animation animation) { }
				/*
				 * (non-Javadoc)
				 * @see android.view.animation.Animation.AnimationListener#onAnimationRepeat(android.view.animation.Animation)
				 */
				@Override
				public void onAnimationRepeat(Animation animation) { }
			};
			PinchAnimation.withPinchListView(PinchListView.this).fromHeight(fromHeight).toHeight(toHeight).withListener(l).go(duration);
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
		private final PinchListView plv;
		private final int startHeight;
		private final int endHeight;
		private final boolean willChangeHeight;
		
		/**
		 * Constructor.
		 */
		public PinchAnimation(PinchListView plv, int startHeight, int endHeight) {
			this.plv = plv;
			this.startHeight = startHeight;
			this.endHeight = endHeight;
			this.willChangeHeight = startHeight != endHeight;
		}
		
		/**
		 * Starts building a CollapseAnimation.
		 * @param view
		 * @return
		 */
		public static PinchAnimation.Builder withPinchListView(PinchListView plv) {
			return new Builder(plv);
		}

		/*
		 * (non-Javadoc)
		 * @see android.view.animation.Animation#applyTransformation(float, android.view.animation.Transformation)
		 */
		@Override
		protected void applyTransformation(float interpolatedTime, Transformation t) {
			if (willChangeHeight) {
				int height = (int) (interpolatedTime * (endHeight - startHeight) + startHeight);
				plv.setPinchableChildrenHeight(height);
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
			private PinchListView plv;
			private int startHeight;
			private int endHeight;
			private AnimationListener listener;
			
			public Builder(PinchListView plv) {
				this.plv = plv;
				this.startHeight = plv.getPinchHeight();
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
				PinchAnimation a = new PinchAnimation(plv, startHeight, endHeight);
				a.setDuration(duration);
				if (listener != null) {
					a.setAnimationListener(listener);
				}
				plv.startAnimation(a);
			}
		}
		
	}
	
	private static class SimpleAnimationListener implements AnimationListener {

		@Override
		public void onAnimationStart(Animation animation) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub
			
		}

		
	}
}
