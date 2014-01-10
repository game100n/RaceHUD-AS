package com.RFR.glass.racehud;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.glass.timeline.TimelineManager;

/**
 * Created by game1_000 on 12/23/13.
 */

public class RaceService extends Service
{

    /** For logging. */
    private static final String TAG = "RaceService";
    private static final String LIVE_CARD_ID = "RaceHUD";

    private RaceView mCallback;

    private TimelineManager mTimelineManager;
    private LiveCard mLiveCard;

    private GPSManager mGPSManager;

    @Override
    public void onCreate()
    {
        super.onCreate();
        
        mTimelineManager = TimelineManager.from(this);

        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mGPSManager = new GPSManager(locationManager);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (mLiveCard == null)
        {
            Log.d(TAG, "Publishing LiveCard");
            mLiveCard = mTimelineManager.createLiveCard(LIVE_CARD_ID);

            /** Keep track of the callback to remove it before unpublishing */
            mCallback = new RaceView(this, mGPSManager);
            //mLiveCard.setDirectRenderingEnabled(true).getSurfaceHolder().addCallback(mCallback);

            LiveCard cardON = mLiveCard.setDirectRenderingEnabled(true);
            cardON.getSurfaceHolder().addCallback(mCallback);
            cardON.getSurfaceHolder().setKeepScreenOn(true);

            /** Display the options menu when the live card is tapped */
            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
            
            /** Jump to the LiveCard when API is available */
            mLiveCard.publish(PublishMode.REVEAL);
            Log.d(TAG, "Done publishing LiveCard");
        }
        else
        {
            /** Card Already Published */
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        if (mLiveCard != null && mLiveCard.isPublished())
        {
            Log.d(TAG, "Unpublishing LiveCard");
            mLiveCard.getSurfaceHolder().removeCallback(mCallback);
            mLiveCard.unpublish();
            mLiveCard = null;
        }
        
        mGPSManager = null;

        super.onDestroy();
    }
}
