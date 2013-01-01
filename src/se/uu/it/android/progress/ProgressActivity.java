package se.uu.it.android.progress;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class ProgressActivity extends Activity implements OnClickListener, OnItemSelectedListener, CustomSetDialog.OnNumberSetListener {
	/** Properties **/
	protected Button startSet;
	protected TextView setCountLabel;
	protected TextView repetitionCountLabel;
	protected TextView setDescriptionLabel;
	protected TextView timeLabel;
	protected Spinner setSpinner;
	protected SetData setData;
	protected Vibrator v;

	protected boolean vibratorToggled = true;
	protected long[] vibratePattern = {0, 200, 100, 200};
	protected long vibrateShort = 70;
	protected long startTime = 0;
	protected int setTime = 0;
	protected int activeTime = 0;
	protected int passiveTime = 0;
	protected int[] customSet = {0, 0, 0};
	protected int customNumberCount = 0;
	protected CountDownTimer activeTimer;
	protected CountDownTimer passiveTimer;
	protected int repetitionCount = 0;
	protected int setRepetitions = 0;
	protected int setCount = 0;
	protected boolean inProgress = false;

	protected ProgressBar currentProgressBar;
	protected ProgressBar setProgressBar;
	protected int activeDuration = activeTime * 1000; 	// (milliseconds)
	protected int passiveDuration = passiveTime * 1000; 	// (milliseconds)
	protected int setDuration = setTime * 1000; 	// (milliseconds)
	protected int stepResolution = 10;				// (milliseconds)
	protected int activeSteps = activeDuration / stepResolution;
	protected int passiveSteps = passiveDuration / stepResolution;
	protected int setSteps = setDuration / stepResolution;

	protected PowerManager pm;
	protected PowerManager.WakeLock wl;
	
	// Notifications
    protected Notification.Builder mBuilder;
    protected Intent resultIntent;
    protected PendingIntent resultPendingIntent;
    protected TaskStackBuilder stackBuilder;
    protected NotificationManager mNotificationManager;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_progress);
		
		// Lock orientation to standard orientation (portrait for mobiles, landscape for tablets)
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

		// Connect interface elements to properties
		startSet = (Button) findViewById(R.id.set_start);
		setCountLabel = (TextView) findViewById(R.id.set_count_label);
		repetitionCountLabel = (TextView) findViewById(R.id.repetition_count);
		setDescriptionLabel = (TextView) findViewById(R.id.set_description);
		timeLabel = (TextView) findViewById(R.id.time_label);
		setData = new SetData(this);
		setSpinner = (Spinner) findViewById(R.id.set_spinner);
		setProgressBar = (ProgressBar) findViewById(R.id.set_progressbar);
		currentProgressBar = (ProgressBar) findViewById(R.id.current_progressbar);

		// Setup ClickListeners
		startSet.setOnClickListener(this);

		// Setup ItemSelectedListeners
		setSpinner.setOnItemSelectedListener(this);

		// Setup Vibrator
		v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		// Create persistent app notification
		createNotification();
		
		// Set the initial set values
		setCount(0);
		setTime(10, 1, 1);

		// Setup wakelock
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ProgressActivity");


		// Add defaults when starting app for the first time after install
		if(setData.count() == 0) {
			setData.insert("10/5 x10", 10, 5, 10);
			setData.insert("2/2 x10", 2, 2, 10);
			setData.insert("120/0 x1", 120, 0, 1);
		}

		// Load and populate setSpinner with sets from setData
		loadSetData();

	}
	
	@Override
    public void onDestroy() {
		super.onDestroy();
        // Cancel the persistent notification.
        mNotificationManager.cancel("Progress", 0); 
    }
	
	public void createNotification() {
	    mBuilder =
	            new Notification.Builder(this)
	            .setSmallIcon(R.drawable.ic_launcher)
	            .setContentTitle("Progress")
	            .setOngoing(true);
	    
	    resultIntent = new Intent(this, ProgressActivity.class);
	    resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
	    mBuilder.setContentIntent(resultPendingIntent);
	    mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	    mNotificationManager.notify("Progress", 0, mBuilder.build());
	}
	
	public void updateNotification() {
		mBuilder.setContentText(setDescriptionLabel.getText())
				.setContentInfo("Rep: 0/" + setRepetitions)
				.setProgress(setSteps, 0, false);
		mNotificationManager.notify("Progress", 0, mBuilder.build());
	}
	
	/** Methods **/

	/**
	 * Set an absolute value for the number of minutes to brew. Has no effect if a brew
	 * is currently running.
	 * @param seconds The number of seconds to brew.
	 */
	public void setTime(int active, int passive, int repetitions) {
		if(inProgress)
			return;

		setTime = (active + passive) * repetitions;
		activeTime = active;
		passiveTime = passive;
		repetitionCount = 0;
		setRepetitions = repetitions;

		setDuration = setTime * 1000; 	// (milliseconds)
		setSteps = setDuration / stepResolution;
		setProgressBar.setMax(setDuration);
		setProgressBar.setProgress(0);
		setProgressBar.setSecondaryProgress(0);

		activeDuration = activeTime * 1000;
		activeSteps = activeDuration / stepResolution;
		currentProgressBar.setMax(activeDuration);
		currentProgressBar.setProgress(0);
		currentProgressBar.setSecondaryProgress(0);

		passiveDuration = passiveTime * 1000;
		passiveSteps = passiveDuration / stepResolution;

		setDescriptionLabel.setText("Set: " + String.valueOf(activeTime) + "/" + String.valueOf(passiveTime) + " x" + String.valueOf(setRepetitions));
		repetitionCountLabel.setText("Completed repetitions: " + String.valueOf(repetitionCount));
		timeLabel.setText("0.0 / " + setDuration / 1000 + " s");
		
		updateNotification();
	}

	/**
	 * Set the number of completed sets and update the interface. 
	 * @param count The new number of completed sets
	 */
	public void setCount(int count) {
		setCount = count;
		setCountLabel.setText(String.valueOf(setCount));
	}

	/**
	 * Start the timer
	 */
	public void startSet() {

		activeTimer = new CountDownTimer(activeDuration, stepResolution) {
			int lastTick = activeDuration;
			@Override
			public void onTick(long millisUntilFinished) {
				currentProgressBar.incrementProgressBy(lastTick - (int) millisUntilFinished);
				currentProgressBar.incrementSecondaryProgressBy((passiveTime + activeTime) * (lastTick - (int) millisUntilFinished));
				setProgressBar.incrementProgressBy(lastTick - (int) millisUntilFinished);
				setProgressBar.incrementSecondaryProgressBy(lastTick - (int) millisUntilFinished);
				lastTick = (int) millisUntilFinished;
				timeLabel.setText(String.format("%.1f", (float) (System.currentTimeMillis() - startTime) / 1000) + " / " + setTime + " s");
				
				mBuilder.setProgress(activeDuration, currentProgressBar.getProgress(), false);
				mNotificationManager.notify("Progress", 0, mBuilder.build());
				
			}

			@Override
			public void onFinish() {
				currentProgressBar.setMax(passiveDuration);
				currentProgressBar.setProgress(passiveDuration);
				currentProgressBar.setSecondaryProgress(passiveDuration);
				lastTick = activeDuration;
				timeLabel.setText(String.format("%.1f", (float) (System.currentTimeMillis() - startTime) / 1000) + " / " + setTime + " s");

				if (vibratorToggled)
					v.vibrate(vibrateShort);

				passiveTimer.start();
			}
		};

		passiveTimer = new CountDownTimer(passiveDuration, stepResolution) {
			int lastTick = passiveDuration;
			@Override
			public void onTick(long millisUntilFinished) {
				currentProgressBar.incrementProgressBy(-(passiveTime + activeTime) * (lastTick - (int) millisUntilFinished));
				currentProgressBar.incrementSecondaryProgressBy(-(lastTick - (int) millisUntilFinished));
				setProgressBar.incrementSecondaryProgressBy(lastTick - (int) millisUntilFinished);
				lastTick = (int) millisUntilFinished;
				timeLabel.setText(String.format("%.1f", (float) (System.currentTimeMillis() - startTime) / 1000) + " / " + setTime + " s");
				
				mBuilder.setProgress(passiveDuration, currentProgressBar.getSecondaryProgress(), false);
				mNotificationManager.notify("Progress", 0, mBuilder.build());
				
			}

			@Override
			public void onFinish() {
				repetitionCount++;
				if (repetitionCount == setRepetitions) {
					getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
					wl.release();

					currentProgressBar.setMax(activeDuration);
					currentProgressBar.setProgress(0);
					currentProgressBar.setSecondaryProgress(0);
					setProgressBar.setProgress(activeDuration*setRepetitions);
					setProgressBar.setSecondaryProgress(setDuration);
					inProgress = false;
					setCount(setCount + 1);
					setDescriptionLabel.setText("Set finished!");
					repetitionCountLabel.setText("Completed repetitions: " + String.valueOf(repetitionCount));
					timeLabel.setText(String.format("%.1f", (float) (System.currentTimeMillis() - startTime) / 1000) + " / " + setTime + " s");
					repetitionCount = 0;
					setSpinner.setEnabled(true);
					startSet.setText("Start");
					
					mBuilder.setContentText(setDescriptionLabel.getText())
							.setContentInfo("Rep: " + setRepetitions + "/" + setRepetitions)
							.setProgress(passiveDuration, currentProgressBar.getSecondaryProgress(), false);
					mNotificationManager.notify("Progress", 0, mBuilder.build());

					if (vibratorToggled)
						v.vibrate(vibratePattern, -1);

				} else {
					repetitionCountLabel.setText("Completed repetitions: " + String.valueOf(repetitionCount));
					currentProgressBar.setMax(activeDuration);
					currentProgressBar.setProgress(0);
					currentProgressBar.setSecondaryProgress(0);
					lastTick = passiveDuration;
					timeLabel.setText(String.format("%.1f", (float) (System.currentTimeMillis() - startTime) / 1000) + " / " + setTime + " s");

					mBuilder.setContentInfo("Rep: " + (repetitionCount + 1) + "/" + setRepetitions)
							.setProgress(activeDuration, currentProgressBar.getProgress(), false);
					mNotificationManager.notify("Progress", 0, mBuilder.build());
					
					if (vibratorToggled)
						v.vibrate(vibrateShort);

					activeTimer.start();
				}
			}
		};

		setProgressBar.setProgress(0);
		setProgressBar.setSecondaryProgress(0);
		setSpinner.setEnabled(false);
		startTime = System.currentTimeMillis();
		activeTimer.start();
		setDescriptionLabel.setText("Set: " + String.valueOf(activeTime) + "/" + String.valueOf(passiveTime) + " x" + String.valueOf(setRepetitions));
		repetitionCountLabel.setText("Completed repetitions: " + String.valueOf(repetitionCount));
		startSet.setText("Stop");
		inProgress = true;
		
		mBuilder.setContentInfo("Rep: " + (repetitionCount + 1) + "/" + setRepetitions);
		
		getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		wl.acquire();
	}

	/**
	 * Stop the timer
	 */
	public void stopSet() {
		if(activeTimer != null) {
			activeTimer.cancel();
		}
		if(passiveTimer != null) {
			passiveTimer.cancel();
		}

		getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		wl.release();
		mBuilder.setContentText(setDescriptionLabel.getText() + " (stopped)");
		mNotificationManager.notify("Progress", 0, mBuilder.build());

		currentProgressBar.setMax(activeDuration);
		currentProgressBar.setProgress(0);
		currentProgressBar.setSecondaryProgress(0);
		setProgressBar.setProgress(0);
		setProgressBar.setSecondaryProgress(0);
		setSpinner.setEnabled(true);
		repetitionCount = 0;
		inProgress = false;
		startSet.setText("Start");
	}
	
	// Load and populate setSpinner with sets from setData
	public void loadSetData() {
		Cursor cursor = setData.all(this);

		@SuppressWarnings("deprecation")
		SimpleCursorAdapter setCursorAdapter = new SimpleCursorAdapter(
				this,
				android.R.layout.simple_spinner_item,
				cursor,
				new String[] { SetData.NAME },
				new int[] { android.R.id.text1 }
				);

		setSpinner.setAdapter(setCursorAdapter);
		setCursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	}

	/** Interface Implementations **/
	/* (non-Javadoc)
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		if(v == startSet) {
			if(!inProgress)
				startSet();
			else {
				stopSet();
			}
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> spinner, View view, int position, long id) {
		if(spinner == setSpinner) {
			// Update the set time with the selected set
			Cursor cursor = (Cursor) spinner.getSelectedItem();
			setTime(cursor.getInt(2), cursor.getInt(3), cursor.getInt(4));
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapterView) {
		// Do nothing
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_progress_activity, menu);
		return true;
	}
	
	// Deactivate menu items for creating new set and removing current set while the app is running a set.
	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
	    if (inProgress)	{
	    	menu.getItem(1).setEnabled(false);
	    	menu.getItem(2).setEnabled(false);
	    } else {
	    	menu.getItem(1).setEnabled(true);
	    	menu.getItem(2).setEnabled(true);
	    }
	    return true;
	}
	
	// If the Custom Set menu option is chosen, open a CustomSetDialog
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_dialog_item) {
            CustomSetDialog dialog = new CustomSetDialog(this, 4, 10, 5, 10);
            dialog.setTitle(getString(R.string.dialog_picker_title));
            dialog.setOnNumberSetListener(this);
            dialog.show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
	
	@Override
	public void onNumberSet(int number) {
        Log.d(ProgressActivity.class.getSimpleName(), "Number selected: " + number);
        addSet(number);
    }
	
	public void addSet(int number) {
		if (customNumberCount < 2) {
			customSet[customNumberCount] = number;
			customNumberCount++;
		} else {
			customSet[customNumberCount] = number;
			setData.insert(customSet[0] + "/" + customSet[1] + " x" + customSet[2], customSet[0], customSet[1], customSet[2]);
			customNumberCount = 0;
			
			// Reload setData in the setSpinner
			loadSetData();			
		}	
	}
	
	public void showHelpInfoDialog(MenuItem item) {
            HelpInfoDialog dialog = new HelpInfoDialog(this, 4);
            dialog.setTitle(getString(R.string.dialog_help_info_title));
            dialog.show();
	}
	
	// Remove currently selected set in setSpinner, reload setData to the setSpinner
	public void removeCurrentSet(MenuItem item) {
		setData.remove(String.valueOf(activeTime) + "/" + String.valueOf(passiveTime) + " x" + String.valueOf(setRepetitions));
		loadSetData();
	}

	public void toggleVibrate(MenuItem item) {
		if (item.isChecked()) {
			item.setChecked(false);
			vibratorToggled = false;
		} else { 
			item.setChecked(true);
			vibratorToggled = true;
		}
	}
}