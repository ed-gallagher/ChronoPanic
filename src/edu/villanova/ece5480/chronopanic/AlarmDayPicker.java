package edu.villanova.ece5480.chronopanic;

import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

public class AlarmDayPicker extends DialogFragment {
	public interface DialogListener {
		public void onDialogPositiveClick(DialogFragment dialog, boolean[] days);
		public void onDialogNegativeClick(DialogFragment dialog);
	}
	
	private CheckBox monC;
	private CheckBox tuesC;
	private CheckBox wedC;
	private CheckBox thursC;
	private CheckBox friC;
	private CheckBox satC;
	private CheckBox sunC;
	private DialogListener mListener;
	
	boolean[] days = {false, false, false, false, false, false, false}; // Initialize to all unchecked
	public static final String[] dayNames = {"Mon", "Tues", "Wed", "Thurs", "Fri", "Sat", "Sun"};
	
	/** used to get currently selected dates from parent activity */
	public void initArgs() {
		Bundle args = getArguments();
		boolean[] daysTemp = args.getBooleanArray("days");
		if (daysTemp != null)
			this.days = daysTemp;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Verify that host activity implements the listener interface
		try {
			mListener = (DialogListener)activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + "needs to implement DialogListener");
		}
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Inflate XML Layout in XML
		LayoutInflater inflater = getActivity().getLayoutInflater();
		final View view = inflater.inflate(R.layout.alarm_day_picker_dialog, null);
		
		// Get handles for all Components
		monC = (CheckBox)view.findViewById(R.id.monCheck);
		tuesC = (CheckBox)view.findViewById(R.id.tuesCheck);
		wedC = (CheckBox)view.findViewById(R.id.wedCheck);
		thursC = (CheckBox)view.findViewById(R.id.thursCheck);
		friC = (CheckBox)view.findViewById(R.id.friCheck);
		satC = (CheckBox)view.findViewById(R.id.satCheck);
		sunC = (CheckBox)view.findViewById(R.id.sunCheck);
		
		// Put current values into each checkbox from array
		monC.setChecked(days[0]);
		tuesC.setChecked(days[1]);
		wedC.setChecked(days[2]);
		thursC.setChecked(days[3]);
		friC.setChecked(days[4]);
		satC.setChecked(days[5]);
		sunC.setChecked(days[6]);
		
		// Show the dialog
		return new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT)
		.setTitle(R.string.day_picker_dialog_title)
		.setView(view)
		// If the user presses the affirmative button
		.setPositiveButton(R.string.day_picker_positive_button, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// return the dataset
				days[0] = monC.isChecked();
				days[1] = tuesC.isChecked();
				days[2] = wedC.isChecked();
				days[3] = thursC.isChecked();
				days[4] = friC.isChecked();
				days[5] = satC.isChecked();
				days[6] = sunC.isChecked();
				Log.d(AlarmDayPicker.class.getCanonicalName(), Arrays.toString(days));
				mListener.onDialogPositiveClick(AlarmDayPicker.this, days);
			}
		})
		// If the user presses the negative button
		.setNegativeButton(R.string.day_picker_negative_button, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Cancel operation
				mListener.onDialogNegativeClick(AlarmDayPicker.this);
				AlarmDayPicker.this.getDialog().cancel();
			}
		})
		.create();
	}
}
