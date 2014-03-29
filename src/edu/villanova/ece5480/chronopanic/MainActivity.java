package edu.villanova.ece5480.chronopanic;

import android.app.Activity;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import edu.villanova.ece5480.chronopanic.broadcastreceiver.AlarmBroadcastReceiver;
import edu.villanova.ece5480.chronopanic.contentprovider.AlarmContentProvider;
import edu.villanova.ece5480.chronopanic.database.AlarmTable;

/**
 * Displays a list of all alarms the user has created.
 * Also used to launch games when an alarm goes off.
 */
public class MainActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = MainActivity.class.getCanonicalName();
	// Constants
	private static final int DELETE_ALARM = Menu.FIRST + 1;
	
	// Cursor adapter for the list
	private SimpleCursorAdapter adapter;
	
	// Game variables (when an alarm goes off)
	Class<?>[] games = {
			Game_WrathOfKresch.class,
			Game_SarveshDrinkMixer.class,
			Game_PenpenPoke.class
	};
	
	public static final int NUM_GAMES = 3;
	
	// Activity Result Request Codes
	public static final int RUN_KRESCH = 0;
	public static final int RUN_SARVESH = 1;
	public static final int RUN_PENPEN = 2;
	public static final int DISMISS_ALARM = 999;
	
	/* Generating distinct random numbers is... not very easy/efficient
	 so we're just hardcoding the order for now */
	public static final int[] gameOrder = {0, 1, 2};
	
	// Alarm variables
	private AlarmAudioService svc;
	private boolean isBound = false;
	Bundle runAlarm = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Add a context menu to our ListView
		registerForContextMenu(getListView());
		
		Log.d(TAG, "Entered onCreate()");
		// Get entries from the database and place them in the list
		propagateList();
		
		runAlarm = getIntent().getExtras();
		if (runAlarm != null) {
			// Bind the alarm audio service
			Log.d(TAG, "binding service");
			Intent audioIntent = new Intent(MainActivity.this, AlarmAudioService.class);
			bindService(audioIntent, mConnection, Context.BIND_AUTO_CREATE);
			if(!isBound) Log.d(TAG, "Service not bound!!");
			
			Log.d(TAG, "Alarm going off!");
			// Just in-case we want to display alarm information at some point
			Uri cpUri = runAlarm.getParcelable(AlarmContentProvider.CONTENT_ITEM_TYPE);
			runGames(cpUri);
		}
		
		Log.d(TAG, "onCreate done");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		Log.d(TAG, "Entered onCreateOptionsMenu()");
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG, "Entered onOptionsItemSelected()");
		switch (item.getItemId()) {
			case R.id.action_add_alarm:
				// create intent for add alarm activity, show activity
				Intent intent = new Intent(this, AlarmEditorActivity.class);
				startActivity(intent);
				return true;
				
			case R.id.action_wok_shortcut:
				// TEMPORARY: Launch the Wrath of Kresch Game Activity
				Intent wokTemp = new Intent(this, Game_WrathOfKresch.class);
				startActivity(wokTemp);
				return true;
			
			case R.id.action_sdm_shortcut: 
				// TEMPORARY: Launch the Sarvesh Game Activity
				Intent boop = new Intent(this,Game_SarveshDrinkMixer.class);
				startActivity(boop);
				return true;
				
			case R.id.action_ppp_shortcut:
				// TEMPORARY: Launch the Penpen Game Activity
				Intent penpenTemp = new Intent(this, Game_PenpenPoke.class);
				startActivity(penpenTemp);
				return true;
				
			default:
				return false;
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, DELETE_ALARM, 0, R.string.context_delete);
		
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case DELETE_ALARM:
			
			// Get the selected item's information
			AdapterContextMenuInfo itemInfo = (AdapterContextMenuInfo) item.getMenuInfo();
			// parse a ContentProvider URI for the alarm with this id
			Uri uri = Uri.parse(AlarmContentProvider.CONTENT_URI + "/" + itemInfo.id);
			
			Log.d(TAG, "Deletion URI: " + uri.toString());
			
			// Delete the item from AlarmManager
			try {
			AlarmBroadcastReceiver abr = new AlarmBroadcastReceiver();
			abr.cancelAlarm(getApplicationContext(), uri);
			} catch (Exception e) {
				Log.e(TAG, "Alarm could not be canceled: " + e.toString());
				e.printStackTrace();
				return true;
			}
			
			// Delete the item with the ContentProvider
			getContentResolver().delete(uri, null, null);
			// Refresh the ListView
			propagateList();
			return true;
		}
		
		return super.onContextItemSelected(item);
	}

	/** Open the Alarm Editor when an item is clicked */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		// Start the Alarm Editor with the ContentProvider URI as an extra
		Intent intent = new Intent(this, AlarmEditorActivity.class);
		Uri alarmId = Uri.parse(AlarmContentProvider.CONTENT_URI + "/" + id);
		intent.putExtra(AlarmContentProvider.CONTENT_ITEM_TYPE, alarmId);
		startActivity(intent);
	}

	private void propagateList() {
		Log.d(TAG, "Entered propagateList()");
		// Map database cols to fields in the listview's rows
		getLoaderManager().initLoader(0, null, this);
		adapter = new SimpleCursorAdapter(this,
				R.layout.alarm_list_row, // Row layout for ListView
				null,
				new String[] {AlarmTable.COL_TIME, AlarmTable.COL_DAYS, AlarmTable.COL_DESC}, // Map from these cols
				new int[] {R.id.alarm_time, R.id.alarm_days, R.id.alarm_desc}, // Map to these IDs
				0 // Do not requery
		);
		
		setListAdapter(adapter);
	}
	
	public void runGames(Uri cpUri) {
		// Start the game activity
		Intent firstGameIntent = new Intent(this, games[gameOrder[0]]);
		startActivityForResult(firstGameIntent, RUN_SARVESH);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
			case RUN_KRESCH:
				Intent kreschIntent = new Intent(this, games[RUN_KRESCH]);
				startActivityForResult(kreschIntent, RUN_SARVESH);
				break;
				
			case RUN_SARVESH:
				Intent sarveshIntent = new Intent(this, games[RUN_SARVESH]);
				startActivityForResult(sarveshIntent, RUN_PENPEN);
				break;
				
			case RUN_PENPEN:
				Intent penpenIntent = new Intent(this, games[RUN_PENPEN]);
				startActivityForResult(penpenIntent, DISMISS_ALARM);
				break;
				
			case DISMISS_ALARM:
				Log.d(TAG, "stopping audio alarm");
				// Stop the alarm audio
				if (isBound) {
					// Stop the alarm audio
					svc.stopSong();
					// Stop the bound service
					unbindService(mConnection);
				}
				break;
				
			default:
				// shouldnt ever occur
				break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.d(TAG, "onCreateLoader called");
		
		String[] projection = {AlarmTable.COL_ID, AlarmTable.COL_TIME, AlarmTable.COL_DAYS, AlarmTable.COL_DESC};
		CursorLoader mCursorLoader = new CursorLoader(this, AlarmContentProvider.CONTENT_URI, projection, null, null, null);
		return mCursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		Log.d(TAG, "onLoadFinished called");
		adapter.swapCursor(cursor);
		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.d(TAG, "onLoaderReset called");
		adapter.swapCursor(null);
	}

	/** Service connection for alarm audio player */
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// gets handle to the service, sets isBound to true
			Log.d(TAG, "onServiceConnected() called -- getting service handle");
			svc = ((AlarmAudioService.MyBinder)service).getService();
			if (svc != null) {
				Log.d(TAG, "service handle success!");
				Log.d(TAG, "setting isBound (" + isBound + ") to true");
				isBound = true;
				Log.d(TAG, "set isBound to true (" + isBound + ")");
				svc.playSong();
			}
			else  {
				Log.d(TAG, "could not get service handle");
				Log.d(TAG, "Could not start alarm audio/service is not bound; isBound=" + isBound);
			}
			
		}
		
		@Override
		public void onServiceDisconnected (ComponentName name) {
			// Cleans up the service after disconnect
			Log.d(TAG, "onServiceDisconnected called -- disconnecting service");
			svc = null;
			isBound = false;
		}
	};
}
