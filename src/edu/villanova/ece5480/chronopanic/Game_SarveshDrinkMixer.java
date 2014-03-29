package edu.villanova.ece5480.chronopanic;

import java.util.Arrays;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Sarvesh Drink Mixer: Mix the drinks in the correct order!
 */
public class Game_SarveshDrinkMixer  extends Activity implements View.OnClickListener{
	// general constants
		private static final String TAG = Game_SarveshDrinkMixer.class.getCanonicalName();
		
		// Handler constants
		public static final int CHANGE_BG			= 0;	// Change the backgrounds
		public static final int ENABLE_BUTTONS		= 1;	// Enable the ImageButtons
		public static final int DEC_PROGRESS_BAR	= 50;	// Decrement the progress bar
		public static final int FAIL_GAME			= 200;	// Game timed out --
		public static final int RESTART_GAME		= 999;	// Restart
		
		// Game constants
		public static final int NUM_TRANSITIONS = 3;	// The number of drinks which the user will have to match
		private static final int[] backgrounds = {		// Resource array for the background images
			//R.drawable.sdm_bg,						//default background without mixed drink display
			R.drawable.sdm_bg_brpiyel,
			R.drawable.sdm_bg_bryelpi,
			R.drawable.sdm_bg_pibryel,
			R.drawable.sdm_bg_piyelbr,
			R.drawable.sdm_bg_yelpibr,
			R.drawable.sdm_bg_yelbrpi
		};
		
		// convenience constants for button clicks (see onClick)
		private static final int WINE = 1;		// Pink
		private static final int GIN = 2;		// Yellow
		private static final int WHISKEY = 3;	// Brown

		// Game mechanic variables
		private static final Integer[][] pattern = {	// Array storing the pattern that needs to be matched; Integer for Arrays.deepEquals()
			{GIN, WINE, WHISKEY},						// 6 backgrounds with 3 possibilities per background
			{WINE, GIN, WHISKEY},
			{GIN, WHISKEY, WINE},
			{WHISKEY, GIN, WINE},
			{WHISKEY, WINE, GIN},
			{WINE, WHISKEY, GIN}
		};
		private static Integer[] userinput;		// Array storing the pattern that the user enters
		private int buttonCounter = 0;			// Counter for storing which index of userinput we're on
		private int bgId = 0;					// Background ID, used when toggling them in the Handler
		
		// Progress bar
		private static final int[] progressBarFrames = {
			R.drawable.progress_bar6,
			R.drawable.progress_bar5,
			R.drawable.progress_bar4,
			R.drawable.progress_bar3,
			R.drawable.progress_bar2,
			R.drawable.progress_bar
		};
		
		private int progressBarSteps = progressBarFrames.length-1;		// Number of progress bar steps
		private boolean gameWin = false;		// Used to indicate whether the game has been won (progress bar)
		private boolean timerStop = false;
		private ImageView progressBar;

		// Components
		private ImageButton wButton;
		private ImageButton gButton;
		private ImageButton whButton;
		RelativeLayout layout;			// Used for setting different backgrounds

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game__sarvesh_drink_mixer);

		// Get handles to views
		layout = (RelativeLayout)findViewById(R.id.background);
		wButton = (ImageButton)findViewById(R.id.wine_button);
		gButton = (ImageButton)findViewById(R.id.gin_button);
		whButton = (ImageButton)findViewById(R.id.whiskey_button);
		progressBar = (ImageView)findViewById(R.id.progress_bar);
		
		// Set action listeners
		wButton.setOnClickListener(this);	// "this" because we implemented the interface
		gButton.setOnClickListener(this);	// View.OnClickListener -- this lets us override
		whButton.setOnClickListener(this);	// onClick and use one ActionListener for mult. buttons
		
		// Turn/keep the screen on when this activity is open, bypass keyguard
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
				WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onStop() {
		super.onStop();
		// Kill the game threads so toasts don't constantly
		// show up when the user backs out of the activity
		gameWin = true;
		timerStop = true;
	}

	/** Initializes/resets variables for the game and shows the animation */
	public void startGame(View view) {
		// declare/initialize vars
		bgId = 0;
		buttonCounter = 0;
		userinput = new Integer[3];
		progressBarSteps = progressBarFrames.length-1;
		gameWin = false;
		timerStop = false;
		
		// Make start button/overlay invisible
		((Button)findViewById(R.id.start_button)).setVisibility(View.GONE);
		((RelativeLayout)findViewById(R.id.overlay)).setVisibility(View.GONE);
		((TextView)findViewById(R.id.game_instructions)).setVisibility(View.GONE);
		((TextView)findViewById(R.id.fail_text)).setVisibility(View.GONE);
		
		progressBar.setImageResource(progressBarFrames[progressBarSteps]);
		progressBar.setVisibility(View.VISIBLE);
		
		// Set a random background
		bgId = randInt(0, 5);
		layout.setBackgroundResource(backgrounds[bgId]);
		Log.d(TAG, "Pattern: " + Arrays.toString(pattern[bgId]));	// Display the correct pattern
		
		// Start a 5 second timeout for games
		new Thread() {
			@Override
			public void run() {
				Looper.prepare();
				while (progressBarSteps >= 0) {
					if (timerStop) break;
					try {
						sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					// Update the progress bar
					updateUi.obtainMessage(DEC_PROGRESS_BAR).sendToTarget();
				}
				
				if (!gameWin) {
					updateUi.obtainMessage(FAIL_GAME).sendToTarget();
					try {
						sleep(1500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					updateUi.obtainMessage(RESTART_GAME).sendToTarget();
				}
			}
		}.start();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.game__sarvesh_drink_mixer, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		if (buttonCounter < userinput.length) {
			switch(v.getId()) {
			case R.id.wine_button:		// Wine button pressed
				// Append wine to the user-input array
				userinput[buttonCounter] = WINE;
				++buttonCounter;
				break;
			case R.id.gin_button:	// Gin button pressed
				// Append gin to the user-input array
				userinput[buttonCounter] = GIN;
				++buttonCounter;
				break;
			case R.id.whiskey_button:		// Whiskey button pressed
				// Append whiskey to the user-input array
				userinput[buttonCounter] = WHISKEY;
				++buttonCounter;
				break;
				
			default: // shouldn't ever occur
				Log.d(TAG, "Button clicklistener default case.");
				break;
			}
		}
		
		Log.d(TAG, "buttonCounter = " + buttonCounter + "; length = " + pattern[bgId].length);
		// If the user has pressed enough buttons, check their answer
		if (buttonCounter >= pattern[bgId].length) {
			checkInput();
		}
		
	}
	
	/** Checks whether the pattern the user entered matches the randomly-chosen one */
	private void checkInput() {
		Log.d(TAG, "Pattern: " + Arrays.toString(pattern[bgId]));
		Log.d(TAG, "INPUT:   " + Arrays.toString(userinput));
		
		// If the elements in the array are the same (deepEquals required for this)
		if (Arrays.deepEquals(pattern[bgId], userinput)) {
			Log.d(TAG, "PATTERN MATCH SUCCESS!");
			gameWin = true;
			Toast.makeText(this, R.string.sdm_pattern_success, Toast.LENGTH_SHORT).show();
			// close the activity
			//this.finish();
			Intent rintent = new Intent();
			setResult(Activity.RESULT_OK, rintent);
			finish();
		}
		
		// If they aren't the same, restart the game with a new pattern
		else {
			Log.d(TAG, "PATTERN MATCH FAILED!");
			timerStop = true;	// stop the timer, causing a reset
		}
	}

	/** Generates a random integer between two values */
	public static int randInt(int min, int max) {
		Random rand = new Random();
		return rand.nextInt((max - min) + 1) + min;
	}
	
	/** Handler to update the UI from a thread */
	@SuppressLint("HandlerLeak") // the Handler is supposed to be static, but UI element fields can't be
	private Handler updateUi = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == DEC_PROGRESS_BAR) {
				if ((--progressBarSteps) < 0) progressBar.setVisibility(View.GONE);
				else progressBar.setImageResource(progressBarFrames[progressBarSteps]);
			}
			
			else if (msg.what == FAIL_GAME) {
				Toast.makeText(getApplicationContext(), R.string.sdm_pattern_fail, Toast.LENGTH_SHORT).show();
				((RelativeLayout)findViewById(R.id.overlay)).setVisibility(View.VISIBLE);
				((TextView)findViewById(R.id.fail_text)).setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.GONE);
			}
			
			else if (msg.what == RESTART_GAME) {
				Game_SarveshDrinkMixer.this.startGame(null);
			}

			// Shouldn't ever occur
			else {
				Log.d(TAG, "Handler: Default case?!");
			}
		}
	};
}
