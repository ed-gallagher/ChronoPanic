package edu.villanova.ece5480.chronopanic;

import java.util.Calendar;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import edu.villanova.ece5480.chronopanic.broadcastreceiver.AlarmBroadcastReceiver;
import edu.villanova.ece5480.chronopanic.contentprovider.AlarmContentProvider;
import edu.villanova.ece5480.chronopanic.database.AlarmTable;
import edu.villanova.ece5480.chronopanic.datastructure.Alarm;

public class AlarmEditorActivity extends Activity implements AlarmDayPicker.DialogListener {
	private static final String TAG = AlarmEditorActivity.class.getCanonicalName();
	// Components
	private TextView alarmTimeText;
	private Button changeTimeButton;
	private TextView alarmDays;
	private EditText alarmDesc;
	private Button changeDaysButton;
	
	// time vars
	Alarm alarm = null;
	int hour, min;
	
	Uri cpUri = null;
	Uri oldcpUri = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alarm_editor);
		// Show the Up button in the action bar.
		setupActionBar();
		
		// Get handles to components
		alarmTimeText = (TextView)findViewById(R.id.time_display);
		changeTimeButton = (Button)findViewById(R.id.time_button);
		alarmDays = (TextView)findViewById(R.id.days_display);
		changeDaysButton = (Button)findViewById(R.id.days_button);
		alarmDesc = (EditText)findViewById(R.id.desc_edittext);
		
		// Restore a saved instance, if it exists
		if (savedInstanceState != null) {
			oldcpUri = cpUri = (Uri)savedInstanceState.getParcelable(AlarmContentProvider.CONTENT_ITEM_TYPE);
		}
		
		// Get an alarm via intent (for editing existing alarms)
		Bundle existingAlarm = getIntent().getExtras();
		if (existingAlarm != null) {
			oldcpUri = cpUri = existingAlarm.getParcelable(AlarmContentProvider.CONTENT_ITEM_TYPE);
			getActionBar().setTitle(R.string.title_activity_alarm_editor_edit);
			updateFields(cpUri);
		}
		
		if (alarm == null)
			alarm = new Alarm();
		
		// Set time change button's click listener to display a time picker dialog
		changeTimeButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Calendar cal = Calendar.getInstance();
				hour = cal.get(Calendar.HOUR_OF_DAY);
				min = cal.get(Calendar.MINUTE);
				boolean timeFormat = DateFormat.is24HourFormat(getApplicationContext());
				new TimePickerDialog(
						AlarmEditorActivity.this,
						timePickerCallback,
						hour,
						min,
						timeFormat
				).show();
			}
		});
		
		changeDaysButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AlarmDayPicker pickDays = new AlarmDayPicker();
				// Send the currently selected days to the dialog
				Bundle args = new Bundle();
				args.putBooleanArray("days", alarm.days);
				pickDays.setArguments(args);
				pickDays.initArgs();
				// Show the dialog
				pickDays.show(getFragmentManager(), AlarmEditorActivity.class.getCanonicalName());
			}
		});
		
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "onPause() called");
		// Save the data to the database
		saveForm();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy() called");
		
		// There has to be at least an alarm time
		if (alarmTimeText.getText().toString().equals(getString(R.string.alarm_edit_time_lbl)) ||
				TextUtils.isEmpty(alarmTimeText.getText().toString()))
			return;
		
		AlarmBroadcastReceiver abr = new AlarmBroadcastReceiver();
		
		// If we're editing an alarm, we need to get rid of the old AlarmManager before adding it again.
		if (getActionBar().getTitle().toString().equals(getString(R.string.title_activity_alarm_editor_edit)))
			abr.cancelAlarm(getApplicationContext(), oldcpUri);
		
		// Create the alarm in AlarmManager
		abr.setAlarm(getApplicationContext(), cpUri);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.d(TAG, "onSaveInstanceState() Called");
		saveForm();
		outState.putParcelable(AlarmContentProvider.CONTENT_ITEM_TYPE, cpUri);
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.alarm_editor, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/** Used to update all components when editing/restoring alarms */
	private void updateFields(Uri uri) {
		alarm = new Alarm(getApplicationContext(), uri);
		this.alarmTimeText.setText(alarm.time);
		this.alarmDays.setText(alarm.dayStr);
		this.alarmDesc.setText(alarm.desc);
	}
	
	/** Saves the alarm creation/editing form to the database */
	private void saveForm() {
		Log.d(TAG, "saveForm() called");
		// There must be at least an alarm time
		if (alarmTimeText.getText().toString().equals(getString(R.string.alarm_edit_time_lbl)) ||
				TextUtils.isEmpty(alarmTimeText.getText().toString()))
			return;
		
		// If the default value is in the days textbox, clear it (for the db submission)
		if (alarmDays.getText().toString().equals(getString(R.string.alarm_edit_days_hint)))
			alarmDays.setText("");
		
		// Store the values in a ContentValues object
		ContentValues alarmInfo = new ContentValues();
		alarmInfo.put(AlarmTable.COL_TIME, alarmTimeText.getText().toString());
		alarmInfo.put(AlarmTable.COL_DAYS, alarmDays.getText().toString());
		alarmInfo.put(AlarmTable.COL_DESC, alarmDesc.getText().toString());
		alarmInfo.put(AlarmTable.COL_SND, "none");
		alarmInfo.put(AlarmTable.COL_OPT, "none");
		
		// Add/update them to the database with the ContentProvider
		if (!TextUtils.isEmpty(alarmTimeText.getText().toString())) {
			if (cpUri == null) { // this is a new alarm
				Log.d(TAG, "cpUri is null!");
				cpUri = getContentResolver().insert(AlarmContentProvider.CONTENT_URI, alarmInfo);
			}

			else { // this is an edited alarm
				Log.d(TAG, cpUri.toString());
				getContentResolver().update(cpUri, alarmInfo, null, null);
			}
		}
	}
	
	private TimePickerDialog.OnTimeSetListener timePickerCallback = new TimePickerDialog.OnTimeSetListener() {
		
		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			hour = hourOfDay;
			min = minute;
			alarmTimeText.setText(((hour < 10) ? "0" + hour : hour) + ":" + ((min < 10) ? "0" + min : min) +
					(DateFormat.is24HourFormat(getApplicationContext()) ? "" : " " + Calendar.getInstance().get(Calendar.AM_PM)));
		}
	};
	
	/** AlarmDayPicker Interface method for positive click */
	@Override
	public void onDialogPositiveClick(DialogFragment dialog, boolean[] days) {
		// Get data from dialog and set the label
		int i = 0;
		alarm.days = days;
		alarmDays.setText(""); // Clear the text view
		
		// Create a string in the text view to show the days to repeat
		for (boolean day : days) {
			if (day) {
				if (!TextUtils.isEmpty(alarmDays.getText().toString())) alarmDays.append(", ");
				alarmDays.append(AlarmDayPicker.dayNames[i]);
			}
			++i;
		}
		
		// No longer needed with hint
		/*if (TextUtils.isEmpty(alarmDays.getText().toString())) // No days were chosen
			alarmDays.setText(R.string.alarm_edit_days_hint);*/
	}
	
	/** AlarmDayPicker Interface method for negative click */
	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {
		// when selecting days is canceled, do nothing
		return;
	}
}
