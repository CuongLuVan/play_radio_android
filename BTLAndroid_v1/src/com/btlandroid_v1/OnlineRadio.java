package com.btlandroid_v1;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class OnlineRadio extends Activity implements
		MediaPlayer.OnPreparedListener, AdapterView.OnItemClickListener {
	MediaPlayer player;
	String[] radio_name;
	String[] radio_url;
	ToggleButton play_pause; //, ready_recording
	ImageButton ready_recording;
	ImageButton next, previous;
	ListView radio_listview;
	TextView current_station;
	ArrayAdapter<String> adapter = null;
	ProgressBar progress_bar;
	String current_station_url;
	int index_station = 0;
	int max_volume, current_volume;
	AudioManager audio_manager;
	SeekBar seekbar_volume;
	File work_directory, out_file;
	FileOutputStream file_output_stream;
	InputStream input_stream;
	static final int INIT_CONNECTION = 0, WRITE_STREAM = 1;
	boolean write_status = false;
	byte[] buffer = new byte[512 * 1024];
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTheme(android.R.style.Theme_Black_NoTitleBar);
		setContentView(R.layout.activity_online_radio);

		// get control*****************************************
		play_pause = (ToggleButton) findViewById(R.id.play_pause);
		radio_listview = (ListView) findViewById(R.id.radio_list);
		radio_name = getResources().getStringArray(R.array.radio_name);
		radio_url = getResources().getStringArray(R.array.radio_url);
		progress_bar = (ProgressBar) findViewById(R.id.progress_bar);
		progress_bar.setVisibility(View.VISIBLE);

		player = new MediaPlayer();
		current_station = (TextView) findViewById(R.id.current_station);

		// initiaion a channel*********************************************
		current_station.setText(radio_name[0]);
		current_station_url = radio_url[0];

		try {
			player.setDataSource(radio_url[0]);
			player.setOnPreparedListener(this);
			player.prepareAsync();

		} catch (Exception e) {
			Toast.makeText(OnlineRadio.this, "Error" + e.toString(),
					Toast.LENGTH_LONG).show();
		}
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, radio_name);
		radio_listview.setAdapter(adapter);
		radio_listview.setOnItemClickListener(this);

		// * play - pause Button*********************************************
		play_pause = (ToggleButton) findViewById(R.id.play_pause);
		play_pause
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							player.pause();
						} else
							player.start();

					}
				});

		// ***********next Button*********************************************
		next = (ImageButton) findViewById(R.id.next);
		next.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (index_station < radio_url.length - 1)
					index_station++;
				else
					index_station = 0; // loop back the first
				player.stop();
				player.reset();
				progress_bar.setVisibility(View.VISIBLE);
				try {
					current_station_url = radio_url[index_station];
					player.setDataSource(current_station_url);
				} catch (Exception e) {
					Toast.makeText(OnlineRadio.this, e.toString(),
							Toast.LENGTH_LONG).show();

				}
				player.prepareAsync();
				current_station.setText(radio_name[index_station]);

			}
		});

		// *prrevious Button*********************************************
		previous = (ImageButton) findViewById(R.id.previous);
		previous.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (index_station > 0)
					index_station--;
				else
					index_station = radio_url.length - 1; // loop back the end
				player.stop();
				player.reset();
				progress_bar.setVisibility(View.VISIBLE);
				try {
					current_station_url = radio_url[index_station];
					player.setDataSource(current_station_url);
				} catch (Exception e) {
					Toast.makeText(OnlineRadio.this, e.toString(),
							Toast.LENGTH_LONG).show();

				}
				player.prepareAsync();
				current_station.setText(radio_name[index_station]);

			}
		});

		// ***************Volume*********************************************
		// Use with getSystemService(String) to retrieve a AudioManager for
		// handling management of volume, ringer modes and audio routing.
		audio_manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		max_volume = audio_manager
				.getStreamMaxVolume(AudioManager.STREAM_MUSIC); // Returns the
																// maximum
																// volume index
																// for a
																// particular
																// stream.
		current_volume = audio_manager
				.getStreamVolume(AudioManager.STREAM_MUSIC); // Returns the
																// current
																// volume index
																// for a
																// particular
																// stream.
		seekbar_volume = (SeekBar) findViewById(R.id.volume);
		seekbar_volume.setMax(max_volume); // Set the range of the progress bar
											// to 0...max.
		seekbar_volume.setProgress(current_volume);

		seekbar_volume
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {

					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {

					}

					@Override
					// function: Notification that the progress level has
					// changed.
					// parameters:
					// seekBar The SeekBar whose progress has changed
					// progress The current progress level. This will be in the
					// range 0..max where max was set by setMax(int). (The
					// default value for max is 100.)
					// fromUser True if the progress change was initiated by the
					// user.
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						audio_manager.setStreamVolume(
								AudioManager.STREAM_MUSIC, progress, 0);

					}
				});

		// ********record*********************************************

		// create a file in order to save the recoreded file
		work_directory = new File(Environment.getExternalStorageDirectory(),
				"OnlineRadioRecord");
		if (!work_directory.exists())
			work_directory.mkdir();

		//ready_recording = (ToggleButton) findViewById(R.id.img_btn_record);
		ready_recording = (ImageButton)findViewById(R.id.img_btn_record);
		ready_recording.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//mỗi khi click thì biến trạng thái write_status sẽ đảo logic
				//coi như toggle button
				write_status =!write_status;
				//dat ten file theo so thu tu
				//moi lan tao file kiem tra xem file da ton tai chua, neu roi thi tang bien dem
				//len 1 de tranh trung lap
				if(write_status){
					int counter =0;
					while (new File(work_directory, "radio" + counter + ".mp3").exists()) {
						counter++;
					}
					 //+ (new Date().toString()): dau tien dinh dat ten file theo ngay thang
					//thay cho couter nhung se phai co 1 ham loai bo dau cach, ki tu dac biet
					//(:, +) trong date tra ve nen khong hop le
					out_file = new File(work_directory, "Radio"
							 +counter + ".mp3");
				try {
					out_file.createNewFile();
					file_output_stream = new FileOutputStream(
							out_file);
					new BackGround().execute(INIT_CONNECTION);

				} catch (Exception e) {
					android.util.Log.e("Error 1 ", e.toString());  //đặt tên exception theo thứ tự để dễ tìm lỗi
				}
				//write_status = true;
				new BackGround().execute(WRITE_STREAM);
				//set image cho button
				ready_recording.setImageResource(R.drawable.img_btn_recording);
				
				}
				else 
					//ngược lại thì set button ready
					ready_recording.setImageResource(R.drawable.img_btn_record_ready);
					
			
				
					
				
			}
		});

	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		write_status = false;
		try {
			input_stream.close();
			file_output_stream.close();
		} catch (Exception e) {
			android.util.Log.e("Error3: ", e.toString());
		}
		if (player.isPlaying())
			player.stop();
		player.release(); //giải phóng bộ nhớ

	}

	public void onPrepared(MediaPlayer media_player) {
		media_player.start();
		progress_bar.setVisibility(View.GONE);
		// play_pause.setChecked(false);
	}

	public void onItemClick(AdapterView<?> a, View v, int position, long l) {
		index_station = position;
		player.stop();
		player.reset();
		progress_bar.setVisibility(View.VISIBLE);
		try {
			current_station_url = radio_url[position];
			player.setDataSource(current_station_url);
		} catch (Exception e) {
			Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
		}
		player.prepareAsync();
		current_station.setText(radio_name[position]);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.online_radio, menu);
		return true;
	}

	// create a new class***********************************
	class BackGround extends AsyncTask<Integer, Void, Void> {
		@Override
		public Void doInBackground(Integer... args) {
			if (args[0] == INIT_CONNECTION) {
				try {
					URL url = new URL(current_station_url);
					URLConnection url_conn = url.openConnection();
					input_stream = new BufferedInputStream(
							url_conn.getInputStream());

				} catch (Exception e) {
					android.util.Log.e("Error 4: ", e.toString());
				}
			}
			if (args[0] == WRITE_STREAM) {
				while (write_status) {
					try {
						int length = input_stream.available();
						input_stream.read(buffer, 0, length - 1);
						file_output_stream.write(buffer, 0, length - 1);

					} catch (Exception e) {
						android.util.Log.e("Error  5", e.toString());
					}
				}
				try {
					input_stream.close();
					file_output_stream.close();
				} catch (Exception e) {
					android.util.Log.e("Error  6", e.toString());
				}
			}
			try {
				file_output_stream.flush();
			} catch (Exception e) {
				android.util.Log.e("Error  6", e.toString());
			}
			return null;

		}
	}

}
