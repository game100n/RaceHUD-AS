package com.RFR.glass.racehud;

import android.content.Context;
import android.graphics.Canvas;
import android.location.Location;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.animation.RotateAnimation;


import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;


/**
 * Created by game1_000 on 12/23/13.
 */

public class RaceView implements SurfaceHolder.Callback
{
	
    /** The refresh rate, in frames per second, of the speedometer. */
    private static final int REFRESH_RATE_FPS = 45;

    /** The duration, in milliseconds, of one frame. */
    private static final long FRAME_TIME_MILLIS = TimeUnit.SECONDS.toMillis(1) / REFRESH_RATE_FPS;

    /** For logging. */
	private static final String TAG = "RaceView";

    private final NumberFormat mSpeedFormat;
    private final FrameLayout mLayout;
    private final TextView mSpeedView;
    private SurfaceHolder mHolder;
    private RenderThread mRenderThread;
    private int mSurfaceWidth;
    private int mSurfaceHeight;

    /** For Animation */
    private final ImageView mNeedle;
    private double SpeedtoAngle = 0.8888;
    private int measuredWidth;
    private int measuredHeight;

    private final GPSManager mGPSManager;

    private final GPSManager.OnChangedListener mSpeedListener =
            new GPSManager.OnChangedListener()
    {
	    public void onOrientationChanged(GPSManager gpsManager)
        {
            // Do Nothing
        }

	    public void onAccuracyChanged(GPSManager gpsManager)
        {
            // Do Nothing
        }

	    public void onLocationChanged(GPSManager gpsManager)
        {
	        Location location = gpsManager.getLocation();
	        Double currentSpeed = gpsManager.getCurrentSpeed();
	        if(location == null)
	        {
		        mSpeedView.setText("-.-");
	        }
	        else
	        {
		        double CurrentSpeedMPH = currentSpeed*2.23694;
		        mSpeedView.setText(mSpeedFormat.format(CurrentSpeedMPH));

                RotateAnimation NeedleAngle = new RotateAnimation(
                        (float) (CurrentSpeedMPH / SpeedtoAngle), (float) (CurrentSpeedMPH / SpeedtoAngle), (measuredWidth / 2), (measuredHeight / 2));
                NeedleAngle.setDuration(1900);
                NeedleAngle.setFillAfter(true);
                mNeedle.startAnimation(NeedleAngle);
                mNeedle.refreshDrawableState();
	        }
        }
    };
            
   /** Creates a new instance of the RaceView with the specified context and GPS manager. */
   public RaceView(Context context, GPSManager gpsManager)
   {
	   LayoutInflater inflater = LayoutInflater.from(context);
	   mLayout = (FrameLayout) inflater.inflate(R.layout.activity_race_view, null);
	   mLayout.setWillNotDraw(false);
	   
	   mSpeedView = (TextView) mLayout.findViewById(R.id.raceText);

	   mSpeedFormat = NumberFormat.getNumberInstance();
	   mSpeedFormat.setMinimumFractionDigits(0);
	   mSpeedFormat.setMaximumFractionDigits(1);
	   
	   mGPSManager = gpsManager;

       mNeedle = (ImageView) mLayout.findViewById(R.id.raceNeedle);
   }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
    	mSurfaceWidth = width;
        mSurfaceHeight = height;
        layoutSetup();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {

        mHolder = holder;
        
        mGPSManager.addOnChangedListener(mSpeedListener);
        mGPSManager.start();
        
        mRenderThread = new RenderThread();
        mRenderThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        mRenderThread.quit();

        mGPSManager.removeOnChangedListener(mSpeedListener);
        mGPSManager.stop();
    }
    
    /**
     * Requests that the views redo their layout. This must be called manually every time the
     * tips view's text is updated because this layout doesn't exist in a GUI thread where those
     * requests will be enqueued automatically.
     */
    private void layoutSetup()
    {
        /**
         * Measure and update the layout so that it will take up the entire surface space
         * when it is drawn.
         */
        measuredWidth = View.MeasureSpec.makeMeasureSpec(mSurfaceWidth,
                View.MeasureSpec.EXACTLY);
        measuredHeight = View.MeasureSpec.makeMeasureSpec(mSurfaceHeight,
                View.MeasureSpec.EXACTLY);

        mLayout.measure(measuredWidth, measuredHeight);
        mLayout.layout(0, 0, mLayout.getMeasuredWidth(), mLayout.getMeasuredHeight());
    }

    private synchronized void repaint()
    {
        Canvas canvas = null;

        try
        {
            canvas = mHolder.lockCanvas();
        }
        catch (RuntimeException e)
        {
            Log.d(TAG, "lockCanvas failed", e);
            return;
        }

        if (canvas != null)
        {
        	layoutSetup();
            mLayout.draw(canvas);

            try
            {
                mHolder.unlockCanvasAndPost(canvas);
                
            }
            catch (RuntimeException e)
            {
                Log.d(TAG, "unlockCanvasAndPost failed", e);
            }
        }
    }

    /** Redraws the View in the background. */
    private class RenderThread extends Thread
    {
        private boolean mShouldRun;

        /** Initializes the background rendering thread. */
        public RenderThread() {
            mShouldRun = true;
        }

        /** Returns true if the rendering thread should continue to run. */
        private synchronized boolean shouldRun() {
            return mShouldRun;
        }

        /** Requests that the rendering thread exit at the next opportunity. */
        public synchronized void quit()
        {
            mShouldRun = false;
        }

        @Override
        public void run()
        {
            while (shouldRun())
            {
                long frameStart = SystemClock.elapsedRealtime();
                repaint();
                long frameLength = SystemClock.elapsedRealtime() - frameStart;

                long sleepTime = FRAME_TIME_MILLIS - frameLength;
                if (sleepTime > 0)
                {
                    SystemClock.sleep(sleepTime);
                }
            }
        }
    }
}
