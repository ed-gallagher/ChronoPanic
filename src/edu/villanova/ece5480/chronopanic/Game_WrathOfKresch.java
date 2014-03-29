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
 * Wrath of Kresch: Traffic Panic!!
 * <p>
 * Game which requires the user to match a pattern shown on a traffic light.
 */
public class Game_WrathOfKresch extends Activity implements View.OnClickListener {
	// general constants
	private static final String TAG = Game_WrathOfKresch.class.getCanonicalName();
	
	// Handler constants
	public static final int CHANGE_BG = 0;			// Change the backgrounds
	public static final int ENABLE_BUTTONS = 1;		// Enable the ImageButtons
	public static final int DEC_PROGRESS_BAR	= 50;	// Decrement the progress bar
	public static final int FAIL_GAME			= 200;	// Game timed out --
	public static final int RESTART_GAME		= 999;	// Restart
	
	// Game constants
	public static final int NUM_TRANSITIONS = 5;	// The number of lights which the user will have to match
	private static final int[] backgrounds = {		// Resource array for the background images
		R.drawable.wok_bg,
		R.drawable.wok_bg_red,
		R.drawable.wok_bg_yellow,
		R.drawable.wok_bg_green
	};
	
	// convenience constants for button clicks (see onClick)
	private static final int RED = 1;
	private static final int YELLOW = 2;
	private static final int GREEN = 3;

	// Game mechanic variables
	private static Integer[] pattern;		// Array storing the pattern that needs to be matched; Integer for Arrays.deepEquals()
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
	
	private boolean gameWin = false;		// Used to indicate whether the game has been won (progress bar)
	private int progressBarSteps = progressBarFrames.length-1;		// Number of progress bar steps
	
	Thread timerThread;
	private boolean timerStop = false;
	private ImageView progressBar;

	// Components
	private ImageButton rButton;
	private ImageButton yButton;
	private ImageButton gButton;
	RelativeLayout layout;			// Used for setting different backgrounds

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game__wrath_of_kresch);

		// Get handles to views
		layout = (RelativeLayout)findViewById(R.id.background);
		rButton = (ImageButton)findViewById(R.id.red_button);
		yButton = (ImageButton)findViewById(R.id.yellow_button);
		gButton = (ImageButton)findViewById(R.id.green_button);
		progressBar = (ImageView)findViewById(R.id.progress_bar);
		
		// Set action listeners
		rButton.setOnClickListener(this);	// "this" because we implemented the interface
		yButton.setOnClickListener(this);	// View.OnClickListener -- this lets us override
		gButton.setOnClickListener(this);	// onClick and use one ActionListener for mult. buttons
		
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
		int i;
		bgId = 0;
		buttonCounter = 0;
		pattern = new Integer[2*NUM_TRANSITIONS]; // 2* for intermediary transitions (turn the light off in-between)
		userinput = new Integer[2*NUM_TRANSITIONS];
		progressBarSteps = progressBarFrames.length-1;
		gameWin = false;
		timerStop = false;
		
		// Make start button/overlay invisible
		((Button)findViewById(R.id.start_button)).setVisibility(View.GONE);
		((RelativeLayout)findViewById(R.id.overlay)).setVisibility(View.GONE);
		((TextView)findViewById(R.id.game_instructions)).setVisibility(View.GONE);
		((TextView)findViewById(R.id.fail_text)).setVisibility(View.GONE);
		
		// Disable the buttons until the animation is done -- otherwise the uesr could just follow along
		rButton.setVisibility(View.GONE);
		yButton.setVisibility(View.GONE);
		gButton.setVisibility(View.GONE);
		
		// get random values for lights to show
		for (i = 0; i < 2*NUM_TRANSITIONS; i += 2) {
			pattern[i] = randInt(1, 3);		// the IDs for the backgrounds in the array are between 1 and 3
			pattern[i+1] = 0;				// Turn off (display default bg) between each light shown
		}
		
		// Runs a timer to delay/"animate" background changes
		// This makes the lights "light-up"
		new Thread() {
			@Override
			public void run() {
				//super.run();
				Looper.prepare(); // required for Handler
				while (bgId < pattern.length-1) {
					try {
						Log.d(TAG, "Sarting sleep");
						sleep(750); // amount of time to delay between light changes
						Log.d(TAG, "Sleep done");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					// Update background
					Log.d(TAG, "Calling Handler");
					updateUi.obtainMessage(CHANGE_BG).sendToTarget();
				}
				
				// After showing the animation, enable the ImageButtons
				updateUi.obtainMessage(ENABLE_BUTTONS).sendToTarget();
			}
		}.start();
	}
	
	public void startTimer() {
		// Start a 5 second timeout for games
		timerThread = new Thread() {
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
				
				if (gameWin == false) {
					updateUi.obtainMessage(FAIL_GAME).sendToTarget();
					try {
						sleep(1500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					updateUi.obtainMessage(RESTART_GAME).sendToTarget();
				}
			}
		};
		
		timerThread.start();
	}

	/** Generic onClick listener for ImageButtons */
	@Override
	public void onClick(View v) {
		if (buttonCounter < userinput.length) {
			switch(v.getId()) {
			case R.id.red_button:		// Red button pressed
				// Append a RED light to the user-input array
				userinput[buttonCounter] = RED;
				userinput[buttonCounter+1] = 0;
				buttonCounter += 2;
				break;
			case R.id.yellow_button:	// Yellow button pressed
				// Append a YELLOW light to the user-input array
				userinput[buttonCounter] = YELLOW;
				userinput[buttonCounter+1] = 0;
				buttonCounter += 2;
				break;
			case R.id.green_button:		// Green button pressed
				// Append a GREEN light to the user-input array
				userinput[buttonCounter] = GREEN;
				userinput[buttonCounter+1] = 0;
				buttonCounter += 2;
				break;
				
			default: // shouldn't ever occur
				Log.d(TAG, "Button clicklistener default case.");
				break;
			}
		}
		
		// If the user has pressed enough buttons, check their answer
		if (buttonCounter >= 2*NUM_TRANSITIONS) {
			checkInput();
		}
	}
	
	/** Checks whether the pattern the user entered matches the randomly-chosen one */
	private void checkInput() {
		Log.d(TAG, "Pattern: " + Arrays.toString(pattern));
		Log.d(TAG, "INPUT:   " + Arrays.toString(userinput));
		
		// If the elements in the array are the same (deepEquals required for this)
		if (Arrays.deepEquals(pattern, userinput)) {
			Log.d(TAG, "PATTERN MATCH SUCCESS!");
			gameWin = true;
			Toast.makeText(this, R.string.wok_pattern_success, Toast.LENGTH_SHORT).show();
			// close the activity
			//this.finish();
			Intent rintent = new Intent();
			setResult(Activity.RESULT_OK, rintent);
			finish();
		}
		
		// If they aren't the same, restart the game with a new pattern
		else {
			Log.d(TAG, "PATTERN MATCH FAILED!");
			timerStop = true; // stop the timer, causing a restart
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.game__wrath_of_kresch, menu);
		return true;
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
			//super.handleMessage(msg);
			Log.d(TAG, "Handler Called");
			
			// Changes the background
			if (msg.what == CHANGE_BG) {
				//Log.d(TAG, "bgId=" + bgId);
				//Log.d(TAG, "pattern[bgId]=" + pattern[bgId]);
				//Log.d(TAG, "" + backgrounds[pattern[bgId]]);
				Log.d(TAG, "Switching to Background" + pattern[bgId]);
				// set to the resource specified by the number in pattern's bgId-th index.
				layout.setBackgroundResource(backgrounds[pattern[bgId++]]);
			}
			
			// Enable the ImageButtons after the animation thread completes
			else if(msg.what == ENABLE_BUTTONS) {
				rButton.setVisibility(View.VISIBLE);
				yButton.setVisibility(View.VISIBLE);
				gButton.setVisibility(View.VISIBLE);
				
				// And display the final pattern, for debugging
				Log.d(TAG, "Pattern: " + Arrays.toString(pattern));
				// Enable the progress bar
				progressBar.setImageResource(progressBarFrames[progressBarSteps]);
				progressBar.setVisibility(View.VISIBLE);
				startTimer();
			}
			
			else if (msg.what == DEC_PROGRESS_BAR) {
				if ((--progressBarSteps) < 0) progressBar.setVisibility(View.GONE);
				else progressBar.setImageResource(progressBarFrames[progressBarSteps]);
			}
			
			else if (msg.what == FAIL_GAME) {
				Toast.makeText(getApplicationContext(), R.string.wok_pattern_fail, Toast.LENGTH_SHORT).show();
				((RelativeLayout)findViewById(R.id.overlay)).setVisibility(View.VISIBLE);
				((TextView)findViewById(R.id.fail_text)).setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.GONE);
			}
			
			else if (msg.what == RESTART_GAME) {
				Game_WrathOfKresch.this.startGame(null);
			}
			
			// Shouldn't ever occur
			else {
				Log.d(TAG, "Handler: Default case?!");
			}
		}
	};
}
