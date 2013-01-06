package se.uu.it.android.progress;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class ProgressService extends Service {
	
	// Notifications
    protected Notification.Builder mBuilder;
    protected Intent resultIntent;
    protected PendingIntent resultPendingIntent;
    protected TaskStackBuilder stackBuilder;
    protected NotificationManager mNotificationManager;
	
	// Binder given to clients
    private final IBinder mBinder = new ProgressBinder();
    
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class ProgressBinder extends Binder {
        ProgressService getService() {
            // Return this instance of ProgressService so clients can call public methods
            return ProgressService.this;
        }
    }
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		createNotification();
	    // We want this service to continue running until it is explicitly
	    // stopped, so return sticky.
	    return START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		// Cancel the persistent notification.
        mNotificationManager.cancel("Progress", 0);
	}
	
	private void createNotification() {
		mBuilder =
	            new Notification.Builder(this)
	            .setSmallIcon(R.drawable.ic_launcher)
	            .setContentTitle("Progress")
	            .setOngoing(true);

	    resultIntent = new Intent(this, ProgressActivity.class);
	    resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
	    resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
	    mBuilder.setContentIntent(resultPendingIntent);
	    mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	    mNotificationManager.notify("Progress", 0, mBuilder.build());
	}

	
	/**
     * Method to update the notification when changing sets.
     * @param setDescription The description of the set as shown in the app interface
     * @param currentRepetition The current repetition of a running set
     * @param setRepetitions The number of repetitions in the set
     * @param setSteps The number of steps (ticks) of the timers
     */
	public void updateNotification(CharSequence setDescriptionLabel, int setRepetitions, int setSteps) {
		mBuilder.setContentText(setDescriptionLabel)
				.setContentInfo("Rep: 0/" + setRepetitions)
				.setProgress(setSteps, 0, false);
		mNotificationManager.notify("Progress", 0, mBuilder.build());
	}
	
	/**
     * Method to update the notification for each time step (tick) of the set timers.
     * @param duration The duration of the current state (active or passive)
     * @param progress Progress of the current state (between 0 and getMax() of progressBar)
     */
	public void updateNotification(int duration, int progress) {
		mBuilder.setProgress(duration, progress, false);
		mNotificationManager.notify("Progress", 0, mBuilder.build());
	}
	
	/**
     * Method to update the notification when a set is finished.
     * @param setDescription The description of the set as shown in the app interface
     * @param setRepetitions The number of repetitions in the set
     * @param duration The duration of the current state
     * @param progress Progress of the current state
     */
	public void updateNotification(CharSequence setDescriptionLabel, int setRepetitions, int duration, int progress) {
		mBuilder.setContentText(setDescriptionLabel)
				.setContentInfo("Rep: " + setRepetitions + "/" + setRepetitions)
				.setProgress(duration, progress, false);
		mNotificationManager.notify("Progress", 0, mBuilder.build());
	}
	
	/**
     * Method to update the notification on transition from passive to active state.
     * @param repetitionCount The number of already finished repetitions
     * @param setRepetitions The total number of repetitions in the set
     * @param duration The duration of the next state
     * @param progress Progress of the next state
     */
	public void updateNotification(int repetitionCount, int setRepetitions, int duration, int progress) {
		mBuilder.setContentInfo("Rep: " + (repetitionCount + 1) + "/" + setRepetitions)
				.setProgress(duration, progress, false);
		mNotificationManager.notify("Progress", 0, mBuilder.build());
	}

}
