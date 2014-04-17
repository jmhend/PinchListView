package me.jmhend.PinchListView;

import me.jmhend.PinchListView.SupportScaleGestureDetector.OnSupportScaleGestureListener;
import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;

/**
 * Interface for abstracting away ScaleGestureDetector.
 * 
 * @author jmhend
 */
public class IScaleGestureDetector implements OnScaleGestureListener, OnSupportScaleGestureListener {
	
	private static final String TAG = IScaleGestureDetector.class.getSimpleName();
	
////=========================================================================================
//// Member variables.
////=========================================================================================
	
	private ScaleGestureDetector mScaleGestureDetector;
	private SupportScaleGestureDetector mSupportScaleGestureDetector;
	private IOnScaleGestureListener mListener;
	
	private boolean mUseSupport;
	
////=========================================================================================
//// Abstractions
////=========================================================================================
	
	/**
	 * @param context
	 * @param listener
	 */
	public IScaleGestureDetector(Context context, IOnScaleGestureListener listener) {
		mListener = listener;
		mUseSupport = Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN;
		
		if (useSupport()) {
			mSupportScaleGestureDetector = new SupportScaleGestureDetector(context, this);
		} else {
			mScaleGestureDetector = new ScaleGestureDetector(context, this);
		}
	}
	
	/**
	 * Direct the MotionEvent to the correct ScaleGestureDetector.
	 * @param event
	 * @return
	 */
	public boolean onTouchEvent(MotionEvent event) {
		if (useSupport()) {
			return mSupportScaleGestureDetector.onTouchEvent(event);
		} else {
			return mScaleGestureDetector.onTouchEvent(event);
		}
	}
	
	/**
	 * @return The ScaleGestureDetector's scaleFactor.
	 */
	public float getScaleFactor() {
		if (useSupport()) {
			return mSupportScaleGestureDetector.getScaleFactor();
		} else {
			return mScaleGestureDetector.getScaleFactor();
		}
	}
	
	/**
	 * @return True if a scale gesture is in Progress.
	 */
	public boolean isInProgress() {
		if (useSupport()) {
			return mSupportScaleGestureDetector.isInProgress();
		} else {
			return mScaleGestureDetector.isInProgress();
		}
	}
	
	
////=========================================================================================
//// Versioning
////=========================================================================================
	
	/**
	 * @return If the SupportScaleGestureDetector should be used.
	 */
	private boolean useSupport() {
		return mUseSupport;
	}
	
	
////=========================================================================================
//// IOnScaleGestureListener
////=========================================================================================
	
	/**
	 * Mocks ScaleGestureDector's OnScaleGestureListener
	 * @author jmhend
	 *
	 */
	public static interface IOnScaleGestureListener  {
		
		/**
		 * @return {@link OnScaleGestureListener#onScale(ScaleGestureDetector)}
		 */
		public boolean onScale(IScaleGestureDetector detector);
		
		/**
		 * @return {@link OnScaleGestureListener#onScaleBegin(ScaleGestureDetector)}
		 */
		public boolean onScaleBegin(IScaleGestureDetector detector);
		
		/**
		 * {@link OnScaleGestureListener#onScaleEnd(ScaleGestureDetector)}
		 */
		public void onScaleEnd(IScaleGestureDetector detector);
	}


	/*
	 * (non-Javadoc)
	 * @see me.jmhend.PinchListView.SupportScaleGestureDetector.OnSupportScaleGestureListener#onScale(me.jmhend.PinchListView.SupportScaleGestureDetector)
	 */
	@Override
	public boolean onScale(SupportScaleGestureDetector detector) {
		return mListener.onScale(this);
	}
	
	/*
	 * (non-Javadoc)
	 * @see me.jmhend.PinchListView.SupportScaleGestureDetector.OnSupportScaleGestureListener#onScaleBegin(me.jmhend.PinchListView.SupportScaleGestureDetector)
	 */
	@Override
	public boolean onScaleBegin(SupportScaleGestureDetector detector) {
		return mListener.onScaleBegin(this);
	}
	
	/*
	 * (non-Javadoc)
	 * @see me.jmhend.PinchListView.SupportScaleGestureDetector.OnSupportScaleGestureListener#onScaleEnd(me.jmhend.PinchListView.SupportScaleGestureDetector)
	 */
	@Override
	public void onScaleEnd(SupportScaleGestureDetector detector) {
		mListener.onScaleEnd(this);
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.view.ScaleGestureDetector.OnScaleGestureListener#onScale(android.view.ScaleGestureDetector)
	 */
	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		return mListener.onScale(this);
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.view.ScaleGestureDetector.OnScaleGestureListener#onScaleBegin(android.view.ScaleGestureDetector)
	 */
	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		return mListener.onScaleBegin(this);
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.view.ScaleGestureDetector.OnScaleGestureListener#onScaleEnd(android.view.ScaleGestureDetector)
	 */
	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {
		mListener.onScaleEnd(this);
	}

}
