package edu.villanova.ece5480.chronopanic;

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
 * Game where the user has to wake a Periwinkle Panda named Penpen by tapping on him.
 */
public class Game_PenpenPoke extends Activity implements View.OnClickListener {
	// general constants
	private static final String TAG = Game_PenpenPoke.class.getCanonicalName();

	// Handler constants
	public static final int ANIMATE_SLEEP		= 0;	// Animate Penpen sleeping
	public static final int ANIMATE_SHOCK		= 1;	// Animate Penpen shocked
	public static final int ANIMATE_CRY			= 2;	// Animate Penpen crying
	public static final int DEC_PROGRESS_BAR	= 50;	// Decrement the progress bar
	public static final int WIN_GAME			= 100;	// Finish up the activity
	public static final int FAIL_GAME			= 200;	// Game timed out --
	public static final int RESTART_GAME		= 999;	// Restart

	// Game resources
	private static final int[] sleepFrames = {		// Resource array for the images where Penpen is sleeping
		R.drawable.penpen_snooze1,
		R.drawable.penpen_snooze2
	};
	
	private static final int shockFrame = R.drawable.penpen_shock;
	
	private static final int[] cryFrames = {
		R.drawable.penpen_cry1,
		R.drawable.penpen_cry2
	};
	
	private static final int[] progressBarFrames = {
		R.drawable.progress_bar6,
		R.drawable.progress_bar5,
		R.drawable.progress_bar4,
		R.drawable.progress_bar3,
		R.drawable.progress_bar2,
		R.drawable.progress_bar
	};

	// Game mechanic variables
	private int pressToWake;				// The number of times the user will have to poke Penpen to wake
	private int userinput;					// The number of times the user actually pokes Penpen
	private int frameId = 0;				// used to hold the current animation frame ID
	private boolean gameWin = false;		// Used to indicate whether the game has been won (progress bar)
	private int progressBarSteps = progressBarFrames.length-1;		// Number of progress bar steps

	// Components
	private ImageButton penpen;				// The button containing Penpen the Periwinkle Panda
	private ImageView progressBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game__penpen_poke);

		// Get handles to views
		penpen = (ImageButton)findViewById(R.id.penpen_button);
		progressBar = (ImageView)findViewById(R.id.progress_bar);

		// Set action listeners
		penpen.setOnClickListener(this);	// "this" because we implemented the interface
											// View.OnClickListener -- this lets us override
											// onClick and use one ActionListener for mult. buttons
		
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
		userinput = pressToWake;
	}

	/** Initializes/resets variables for the game and shows the animation */
	public void startGame(View view) {
		// declare/initialize vars
		progressBarSteps = progressBarFrames.length-1;
		gameWin = false;
		frameId = 0;
		userinput = 0;
		pressToWake = randInt(3, 12); // set the number of times the user has to poke Penpen to wake

		// Make start button/overlay invisible
		((Button)findViewById(R.id.start_button)).setVisibility(View.GONE);
		((RelativeLayout)findViewById(R.id.overlay)).setVisibility(View.GONE);
		((TextView)findViewById(R.id.game_instructions)).setVisibility(View.GONE);
		((TextView)findViewById(R.id.fail_text)).setVisibility(View.GONE);
		
		progressBar.setImageResource(progressBarFrames[progressBarSteps]);
		progressBar.setVisibility(View.VISIBLE);
		
		// Show the number of presses to wake Penpen
		Log.d(TAG, "Poke " + pressToWake + " times to wake Penpen.");

		// Runs a timer to delay/"animate" background changes
		// This makes the lights "light-up"
		new Thread() {
			@Override
			public void run() {
				//super.run();
				Looper.prepare(); // required for Handler
				// Keep sleeping until woken
				while (userinput < pressToWake) {
					try {
						sleep(1000); // amount of time to delay between light changes
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					// toggle sleeping frame
					updateUi.obtainMessage(ANIMATE_SLEEP).sendToTarget();
				}
				
				// win the game!
				gameWin = true;
				
				// Shock animation when finally awake
				frameId = 1;
				updateUi.obtainMessage(ANIMATE_SHOCK).sendToTarget();
				try { // Sleep
					sleep(1000); // amount of time to delay between light changes
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				// Crying :'(
				int i;
				for (i=0; i < 8; ++i) {
					updateUi.obtainMessage(ANIMATE_CRY).sendToTarget();
					try { // Sleep
						sleep(250); // amount of time to delay between light changes
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				updateUi.obtainMessage(WIN_GAME).sendToTarget();
			}
		}.start();
		
		// Start a 5 second timeout for games
		new Thread() {
			@Override
			public void run() {
				Looper.prepare();
				while (progressBarSteps >= 0) {
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
		}.start();
	}

	/** Generic onClick listener for ImageButtons */
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.penpen_button:	// Penpen was poked
			// Add one to the number of times Penpen was poked
			++userinput;
			break;

		default: // shouldn't ever occur
			Log.d(TAG, "Button clicklistener default case.");
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.game__penpen_poke, menu);
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
			// Changes the background
			if (msg.what == ANIMATE_SLEEP) {
				frameId = (frameId == 0) ? 1 : 0;
				penpen.setImageResource(sleepFrames[frameId]);
			}

			// Enable the ImageButtons after the animation thread completes
			else if (msg.what == ANIMATE_SHOCK) {
				penpen.setImageResource(shockFrame);
			}
			
			else if (msg.what == ANIMATE_CRY) {
				frameId = (frameId == 0) ? 1 : 0;
				penpen.setImageResource(cryFrames[frameId]);
			}
			
			else if (msg.what == DEC_PROGRESS_BAR) {
				if ((--progressBarSteps) < 0) progressBar.setVisibility(View.GONE);
				else progressBar.setImageResource(progressBarFrames[progressBarSteps]);
			}
			
			else if (msg.what == WIN_GAME) {
				Toast.makeText(getApplicationContext(), R.string.ppp_pattern_success, Toast.LENGTH_SHORT).show();
				//Game_PenpenPoke.this.finish();
				Intent rintent = new Intent();
				Game_PenpenPoke.this.setResult(Activity.RESULT_OK, rintent);
				Game_PenpenPoke.this.finish();
			}
			
			else if (msg.what == FAIL_GAME) {
				Toast.makeText(getApplicationContext(), R.string.ppp_pattern_fail, Toast.LENGTH_SHORT).show();
				((RelativeLayout)findViewById(R.id.overlay)).setVisibility(View.VISIBLE);
				((TextView)findViewById(R.id.fail_text)).setVisibility(View.VISIBLE);
			}
			
			else if (msg.what == RESTART_GAME) {
				Game_PenpenPoke.this.startGame(null);
			}

			// Shouldn't ever occur
			else {
				Log.d(TAG, "Handler: Default case?!");
			}
		}
	};
}
