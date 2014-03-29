package edu.villanova.ece5480.chronopanic.datastructure;

import java.util.Arrays;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import edu.villanova.ece5480.chronopanic.AlarmDayPicker;
import edu.villanova.ece5480.chronopanic.database.AlarmTable;

/**
 * Data Structure for Alarms. Holds all information for a given alarm.
 */
public class Alarm {
	private static final String TAG = Alarm.class.getCanonicalName();
	
	public String time;
	public int hour;
	public int min;
	public String dayStr;
	public boolean[] days = {false, false, false, false, false, false, false};
	public String desc;
	public String sound;
	public String options;
	
	/** Default constructor used to simply initialize days */
	public Alarm() {}
	
	/** Most common constructor: fetches alarm data from ContentProvider given a Uri */
	public Alarm(Context context, Uri uri) {
		String[] projection = {
				AlarmTable.COL_TIME,
				AlarmTable.COL_DAYS,
				AlarmTable.COL_DESC,
				AlarmTable.COL_SND,
				AlarmTable.COL_OPT
		};
		
		// Get the data from the ContentProvider
		Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst(); // Go to the head
			
			// Time
			time = cursor.getString(cursor.getColumnIndexOrThrow(AlarmTable.COL_TIME));
			String[] timeSplit = time.split(":");
			hour = Integer.valueOf(timeSplit[0]);
			min = Integer.valueOf(timeSplit[1]);
			
			// Recurring days
			dayStr = cursor.getString(cursor.getColumnIndexOrThrow(AlarmTable.COL_DAYS));
			if (!TextUtils.isEmpty(dayStr)) {
				days = stringToBoolArray(dayStr);
			}
			
			// description
			desc = cursor.getString(cursor.getColumnIndexOrThrow(AlarmTable.COL_DESC));
			
			// Sound
			sound = cursor.getString(cursor.getColumnIndex(AlarmTable.COL_SND));
			
			// Options
			options = cursor.getString(cursor.getColumnIndex(AlarmTable.COL_OPT));
		}
		// Unload the cursor
		cursor.close();
	}
	
	private boolean[] stringToBoolArray(String str) {
		boolean[] localDays = {false, false, false, false, false, false, false};
		String[] split = str.split(", ");
		Log.d(TAG, "String: " + str + "Split Array: " + Arrays.toString(split));
		try {
			for (String s : split) {
				Log.d(TAG, "Current string: " + s);
				Log.d(TAG, "Index: " + indexOfDay(s));
				localDays[indexOfDay(s)] = true;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			Log.e(TAG, "Array index out of bounds. indexOfDay didn't find a day.");
			localDays = null;
		}
		
		return localDays;
	}
	
	private int indexOfDay(String day) {
		int i = 0;
		for (String aDay : AlarmDayPicker.dayNames) {
			Log.d(TAG, "i: " + i + "; day: `" + aDay + "`; target day: `" + day + "`");
			if (day.equals(aDay)) return i;
			++i;
		}
		
		return -1;
	}

	@Override
	public String toString() {
		return "Time: " + time + "; Days: " + dayStr + "; Desc: " + desc;
	}
}
