package edu.villanova.ece5480.chronopanic;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class AlarmAudioService extends Service {
	private static final String TAG = AlarmAudioService.class.getCanonicalName();
	private final IBinder binder = new MyBinder();
	private MediaPlayer mp;
	
	public AlarmAudioService() {}

	@Override
	public IBinder onBind(Intent intent) {
		// Provide an interface for clients in the same process to use
		mp = MediaPlayer.create(this, R.raw.music);
		return binder;
	}
	
	public class MyBinder extends Binder {
		// return an instance of the service
		AlarmAudioService getService() {
			return AlarmAudioService.this;
		}
	}
	
	public void playSong() {
		Log.d(TAG, "Audio service playing...");
		if (mp.isPlaying() == false) {
			mp.start();
			mp.setLooping(true);
		}
		else
			Toast.makeText(getApplicationContext(), R.string.audio_play_error, Toast.LENGTH_SHORT).show();
	}
	
	public void stopSong() {
		Log.d(TAG, "Audio service stopping");
		if (mp.isPlaying() == true) {
			mp.stop();
			mp.release();
		}
		else
			Toast.makeText(getApplicationContext(), R.string.audio_stop_error, Toast.LENGTH_SHORT).show();
	}
}
