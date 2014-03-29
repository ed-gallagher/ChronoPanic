package edu.villanova.ece5480.chronopanic.broadcastreceiver;

import java.util.Calendar;
import java.util.Random;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import edu.villanova.ece5480.chronopanic.MainActivity;
import edu.villanova.ece5480.chronopanic.contentprovider.AlarmContentProvider;
import edu.villanova.ece5480.chronopanic.datastructure.Alarm;

/**
 * Handles alarm reception and management
 * @see https://developer.android.com/reference/android/app/AlarmManager.html
 * @see http://code4reference.com/2012/07/tutorial-on-android-alarmmanager/
 * @see https://github.com/rakeshcusat/Code4Reference/tree/master/AndroidProjects/AlarmManagerExample
 */
public class AlarmBroadcastReceiver extends BroadcastReceiver {
	private static final String TAG = AlarmBroadcastReceiver.class.getCanonicalName();
	public static WakeLock wakelock;
	int currentApiVersion = android.os.Build.VERSION.SDK_INT;
	
	Uri alarmUri = null;
	
	public AlarmBroadcastReceiver() {}

	/**
	 * Called when the BroadcastReceiver receives an Intent broadcast.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "BroadcastReceiver received a broadcast.");
		// Get a device WakeLock
		PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
		// Partial wake lock so we can turn the screen on
		// wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		wakelock.acquire();
		
		try {
			// Get the alarm ContentProvider URI
			Bundle extras = intent.getExtras();
			alarmUri = extras.getParcelable(AlarmContentProvider.CONTENT_ITEM_TYPE);

			// Run MainActivity with a bundle, to start the games
			Intent launchMain = new Intent(context, MainActivity.class);
			launchMain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			launchMain.putExtra(AlarmContentProvider.CONTENT_ITEM_TYPE, alarmUri);
			context.startActivity(launchMain);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			wakelock.release();
		}
		
		// Avoid Wakelock leaks if something crashes
		/*try {
			Log.d(TAG, "Acquired Wakelock");
			// Get the alarm CP Uri from the extras
			Bundle extras = intent.getExtras();
			alarmUri = extras.getParcelable(AlarmContentProvider.CONTENT_ITEM_TYPE);
			
			Log.d(TAG, "URI Received from intent: " + alarmUri.toString());
			
			Alarm alarm = new Alarm(context, alarmUri);
			Log.d(TAG, "Alarm from URI: " + alarm.toString());
			
			// Run the games
			Intent penpenGame = new Intent(context, Game_PenpenPoke.class);
			context.startActivity(penpenGame);
			//Toast.makeText(context, "ALARM!\nTime: " + alarm.time + "\nDays: " + alarm.dayStr + "\nDesc: " + alarm.desc, Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Log.e(TAG, "An error occurred for this alarm: " + e.toString());
			e.printStackTrace();
		} finally {
			wakelock.release();
		}*/
	}
	
	/** sets an alarm for a database entry and recurs every day */
	public void setAlarm(Context context, Uri uri) {
		Log.d(TAG, "setAlarm() called with uri: " + uri.toString());
		// Get handle to AlarmManager
		AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		// Create the intents
		AlarmManagerIntents intents = new AlarmManagerIntents(context, uri);
		// add the alarm to alarm manager
		//alarm.setRepeating(AlarmManager.RTC_WAKEUP, intents.alarmTime, 24*60*60*1000, intents.pendingIntent);
		
		/* AlarmManager behavior was changed in KitKat -- it no longer wakes up exactly on the time an
		 * alarm is scheduled. See: https://developer.android.com/reference/android/app/AlarmManager.html */
		if (currentApiVersion >= android.os.Build.VERSION_CODES.KITKAT) {
			Log.d(TAG, "Setting KitKat Alarm...");
			alarmSetKitkat(alarm, intents);
		}
		
		else {
			Log.d(TAG, "Setting non-KitKat Alarm...");
			alarmSetICS(alarm, intents);
		}
	}
	
	@TargetApi(19)
	public void alarmSetKitkat(AlarmManager alarm, AlarmManagerIntents intents) {
		alarm.setExact(AlarmManager.RTC_WAKEUP, intents.alarmTime, intents.pendingIntent);
	}
	
	public void alarmSetICS(AlarmManager alarm, AlarmManagerIntents intents) {
		alarm.set(AlarmManager.RTC_WAKEUP, intents.alarmTime, intents.pendingIntent);
	}
	
	public void cancelAlarm(Context context, Uri uri) {
		Log.d(TAG, "cancelAlarm() called with uri: " + uri.toString());
		// Get handle to AlarmManager
		AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		// Create the intents
		AlarmManagerIntents intents = new AlarmManagerIntents(context, uri);
		// Delete the alarm from AlarmManager
		alarm.cancel(intents.pendingIntent);
	}
	
	/** Generates a random integer between two values */
	public static int randInt(int min, int max) {
		Random rand = new Random();
		return rand.nextInt((max - min) + 1) + min;
	}
	
	private class AlarmManagerIntents {
		//public static final int DAILY_ALARM = 0;
		//public static final int WEEKLY_ALARM = 1;
		//public static final int ONE_TIME = 2;
		
		public Intent intent;
		public PendingIntent pendingIntent;
		public long alarmTime;
		
		public AlarmManagerIntents(Context context, Uri uri) {
			// get alarm information
			Alarm alarmInfo = new Alarm(context, uri);
			Log.d(TAG, "URI: " + uri.toString() + "Alarm: " + alarmInfo.toString());
			
			// Get the current date for setting relative alarms
			//Date curDate = new Date();
			
			// Create the new alarm time in ms
			Calendar curCal = Calendar.getInstance();
			
			// Adjust for tomorrow if the time has already passed
			if (curCal.get(Calendar.HOUR_OF_DAY) >= alarmInfo.hour && curCal.get(Calendar.MINUTE) >= alarmInfo.min)
				curCal.set(Calendar.DATE, curCal.get(Calendar.DATE)+1);
			
			// Set the hours and minutes
			curCal.set(Calendar.HOUR_OF_DAY, alarmInfo.hour);
			curCal.set(Calendar.MINUTE, alarmInfo.min);
			
			alarmTime = curCal.getTimeInMillis();
			
			intent = new Intent (context, AlarmBroadcastReceiver.class);
			intent.putExtra(AlarmContentProvider.CONTENT_ITEM_TYPE, uri);
			pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		}
	}
}
