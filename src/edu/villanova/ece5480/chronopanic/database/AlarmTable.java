package edu.villanova.ece5480.chronopanic.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Table class for Alarms.
 * <p>
 * Defines table structure and is used to create/upgrade the database.
 * <p>
 * Created using a combination of the SQLite exercise and the tutorial
 * linked in the "See Also" section.
 * @see http://www.vogella.com/articles/AndroidSQLite/article.html#todo
 *
 */
public class AlarmTable {
	public static final String TAG = AlarmTable.class.getCanonicalName();
	// Table Information
	public static final String TBL_NAME = "alarms";
	public static final String COL_ID = "_id";				// Unique ID of the alarm
	public static final String COL_TIME = "time";			// Time the alarm will go off
	public static final String COL_DAYS = "days";			// Days on which alarm will repeat
	public static final String COL_DESC = "description";	// Description of the alarm
	public static final String COL_SND = "sound";			// What to play when the alarm sounds
	public static final String COL_OPT = "options";			// Options for the alarm
	
	// SQL Create statement
	private static final String DB_CREATE = "CREATE TABLE "
			+ TBL_NAME + " ("
			+ COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ COL_TIME + " TEXT NOT NULL, "
			+ COL_DAYS + " TEXT, "
			+ COL_DESC + " TEXT, "
			+ COL_SND + " TEXT NOT NULL, "
			+ COL_OPT + " TEXT NOT NULL"
			+ ");";
	
	/** Create the database */
	public static void onCreate(SQLiteDatabase database) {
		database.execSQL(DB_CREATE);
	}
	
	/** Upgrade old version to the new version */
	public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading DB from " + oldVersion + " to " + newVersion + " will destroy old data.");
		database.execSQL("DROP TABLE IF EXISTS " + TBL_NAME);
		database.execSQL(DB_CREATE);
	}
}
