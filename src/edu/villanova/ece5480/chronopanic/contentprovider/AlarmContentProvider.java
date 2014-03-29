package edu.villanova.ece5480.chronopanic.contentprovider;

import java.util.Arrays;
import java.util.HashSet;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import edu.villanova.ece5480.chronopanic.database.AlarmDatabaseHelper;
import edu.villanova.ece5480.chronopanic.database.AlarmTable;

/**
 * Content Provider for the Alarm table.
 * <p>
 * I'm no expert on Content Providers, so I'm kind of playing this one by ear, 
 * relying heavily on tutorials until I figure it out...
 * <p>
 * ...
 * <p>
 * After going through the tutorial code, content providers seem (a) really cool and
 * (b) really powerful. They're a bit difficult to understand at first, but I find them
 * fairly fascinating. I would have liked to see more of them in the class (... or maybe
 * a second class could be offered with more advanced stuff? ;])
 * 
 * @see http://www.vogella.com/articles/AndroidSQLite/article.html#todo
 */
public class AlarmContentProvider extends ContentProvider {
	private AlarmDatabaseHelper db;
	
	/**
	 * Constants for ContentProvider UriMatcher
	 * {@link https://developer.android.com/reference/android/content/UriMatcher.html}
	 */
	// IDs - used to determine the type of content request made
	private static final int ALARMS = 10;
	private static final int ALARM_ID = 20;
	
	// URI vars
	private static final String AUTH = "com.chronopanic.contentprovider";
	private static final String BASE_PATH = "alarms";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTH + "/" + BASE_PATH);
	
	// Content Resolver types
	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/alarms";
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/alarm";
	
	// The URI Matcher
	private static final UriMatcher mURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		mURIMatcher.addURI(AUTH, BASE_PATH, ALARMS);
		mURIMatcher.addURI(AUTH, BASE_PATH + "/#", ALARM_ID);
	}
	
	/** Get a handle to the Database Helper */
	@Override
	public boolean onCreate() {
		db = new AlarmDatabaseHelper(getContext());
		return false;
	}

	/** Deletes rows from the database */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int reqType = mURIMatcher.match(uri);
		SQLiteDatabase dbHandle = db.getWritableDatabase();
		int rowsDeleted = 0;
		
		switch (reqType) {
		case ALARMS:
			// Deletion from table WITHOUT using the alarm's unique ID -- useful for multiple deletions
			rowsDeleted = dbHandle.delete(AlarmTable.TBL_NAME, selection, selectionArgs);
		case ALARM_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				// Not using COL_ID=? because converting a simple int to a string array seems... like overkill
				rowsDeleted = dbHandle.delete(AlarmTable.TBL_NAME, AlarmTable.COL_ID + "=" + id, null);
			}
			else {
				// Allows for more args to be added to the query... neat
				rowsDeleted = dbHandle.delete(AlarmTable.TBL_NAME, AlarmTable.COL_ID + "=" + id + " AND " + selection, selectionArgs);
			}
			
			break;
		default:
			throw new IllegalArgumentException("URI type not found for: " + uri);
		}
		
		// Notify listeners and return the number of rows deleted
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsDeleted;
	}

	/** TODO Not really sure what this does, but it has to be implemented... I'll look it up later */
	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int reqType = mURIMatcher.match(uri);
		SQLiteDatabase dbHandle = db.getWritableDatabase();
		long id = 0;
		
		switch (reqType) {
		case ALARMS:
			id = dbHandle.insert(AlarmTable.TBL_NAME, null, values);
			break;
		default:
			throw new IllegalArgumentException("URI type not found for: " + uri); 
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		//return Uri.parse(BASE_PATH + "/" + id);
		return Uri.parse(CONTENT_URI + "/" + id);
	}

	/** Queries the database based on the type of content being requested */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder mQueryBuilder = new SQLiteQueryBuilder();
		
		// Do all of the columns requested exist?
		checkCols(projection);
		
		// We're using the "Alarms" table...
		mQueryBuilder.setTables(AlarmTable.TBL_NAME);
		
		// Match the Content Request URI to one of our ID types and act accordingly.
		int reqType = mURIMatcher.match(uri);
		
		switch (reqType) {
		case ALARMS:
			// Everything we need for this case is after the switch
			break;
		case ALARM_ID:
			// We're looking for a specific alarm ID -- add it to the query
			mQueryBuilder.appendWhere(AlarmTable.COL_ID + "=" + uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("URI type not found for: " + uri);
		}
		
		// Execute the query and return the cursor
		SQLiteDatabase dbHandle = db.getWritableDatabase();
		Cursor cursor = mQueryBuilder.query(dbHandle, projection, selection, selectionArgs, null, null, sortOrder);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		
		return cursor;
	}
	
	/** Make sure that only valid columns are included in the request */
	private void checkCols(String[] projection) {
		String[] tblCols = {
				AlarmTable.COL_ID,
				AlarmTable.COL_TIME,
				AlarmTable.COL_DAYS,
				AlarmTable.COL_DESC,
				AlarmTable.COL_SND,
				AlarmTable.COL_OPT
		};
		
		if (projection != null) {
			HashSet<String> reqCols = new HashSet<String>(Arrays.asList(projection));
			HashSet<String> tblColsSet = new HashSet<String>(Arrays.asList(tblCols));
			
			if (!tblColsSet.containsAll(reqCols))
				throw new IllegalArgumentException("Unknown columns in your request.");
		}
	}

	/** Update items in the database */
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int reqType = mURIMatcher.match(uri);
		SQLiteDatabase dbHandle = db.getWritableDatabase();
		int rowsUpdated = 0;
		switch(reqType) {
		case ALARMS:
			rowsUpdated = dbHandle.update(AlarmTable.TBL_NAME, values, selection, selectionArgs);
			break;
		case ALARM_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = dbHandle.update(AlarmTable.TBL_NAME, values, AlarmTable.COL_ID + "=" + id, null);
			}
			else {
				rowsUpdated = dbHandle.update(AlarmTable.TBL_NAME, values, AlarmTable.COL_ID + "=" + id + " AND " + selection, selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("URI type not found for: " + uri);
		}
		
		// Notify listeners and return the number of rows updated by the query
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsUpdated;
	}

}
