package edu.villanova.ece5480.chronopanic.database;

/** Created following SQLite Tutorial at {@link http://www.vogella.com/articles/AndroidSQLite/article.html#todo} */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper class for the Alarm Table
 */
public class AlarmDatabaseHelper extends SQLiteOpenHelper {
	public static final String DB_NAME = "alarmtable.db";
	public static final int DB_VERSION = 1;
	
	public AlarmDatabaseHelper (Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	/** Called to create the database */
	@Override
	public void onCreate(SQLiteDatabase db) {
		AlarmTable.onCreate(db);
	}

	/** Called to update the database if the version number changes */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		AlarmTable.onUpgrade(db, oldVersion, newVersion);
	}

}
