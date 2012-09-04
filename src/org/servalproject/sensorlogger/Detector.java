package org.servalproject.sensorlogger;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.FloatMath;
import android.util.Log;

public class Detector extends Service implements SensorEventListener{
	SensorManager sensorManager;
	Sensor sensor;
	static boolean running = false;
	Date started;
	float gravity[]=new float[3];
	long startTime=-1;
	long previousTime=-1;
	DataOutputStream out;
	
	BroadcastReceiver receiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
				// re-register sensor to ensure we can receive events while the screen is off
				Log.v("Acceleration", "Restarting sensor as the screen has turned off");
				sensorManager.unregisterListener(Detector.this);
				sensorManager.registerListener(Detector.this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
			}else if(intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)){
				// TODO check if level below threshold
			}
		}
	};
	
	@Override
	public void onDestroy() {
		Log.v("Acceleration", "Stopping capture of sensor data");
		sensorManager.unregisterListener(this);
		this.unregisterReceiver(receiver);
		finishFile();
		running = false;
		super.onDestroy();
	}

	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
	private void startFile(){
		finishFile();
		
		startTime=-1;
		started = new Date();
		
		try {
			out = new DataOutputStream(
					new FileOutputStream(
						new File(
								this.getExternalFilesDir(null),
								"movement_"+dateFormat.format(started)+".log"
					)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private void finishFile(){
		if (out==null)
			return;
		
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		out = null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v("Acceleration", "Start capture of sensor data");
		log("time,accuracy,x,y,z,magnitude,hpf_x,hpf_y,hpf_z,hpf_magnitude\n");
		sensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		Log.v("Sensor", "Name: "+sensor.getName());
		Log.v("Sensor", "Maximum Range: "+sensor.getMaximumRange());
		Log.v("Sensor", "Power: "+sensor.getPower());
		Log.v("Sensor", "Resolution: "+sensor.getResolution());
		Log.v("Sensor", "Vendor: "+sensor.getVendor());
		Log.v("Sensor", "Version: "+sensor.getVersion());
		sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
		IntentFilter intentFilter=new IntentFilter();
		intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
		intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
		this.registerReceiver(receiver, intentFilter);
		// tell android to restart the service if it is killed
		running = true;
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// Noop
	}

	private void log(String data){
		try {
			Log.v("Acceleration",data);
			if (out!=null)
				out.writeBytes(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void onSensorChanged(SensorEvent event) {
		// messages are queued and may arrive after stopping
		if (!running) 
			return;
		
		// start a new file every hour
		if (started==null || System.currentTimeMillis() - started.getTime() >= (60*60*1000))
			startFile();
		
		if (startTime==-1)
			startTime = event.timestamp;
		
		// determine the magnitude of the acceleration
		float rawMagnitude = FloatMath.sqrt(event.values[0]*event.values[0] + event.values[1]*event.values[1] + event.values[2]*event.values[2]);
		
		// based on adxl345 sensor
		// quantisation in measurements with +-2g range, with a 10 bit output would be about 0.038 ms^2
		// noise in each axis has been measured at about 3x that
		// so the smallest change in acceleration we can reliably detect is probably ~0.20ms^2
		
		// detect large / fast movements by using a low pass filter to identify the contribution of gravity
		float alpha = 1f;
		
		if (previousTime!=-1){
			// use the time between samples in seconds (probably 0.2) to tweak the filter
			alpha = (float) ((event.timestamp - previousTime) / 800000000.0); //(0.8s)
			if (alpha <0)
				alpha=0;
			if (alpha >1)
				alpha=1;
		}
		
		gravity[0] = (1 - alpha) * gravity[0] + alpha * event.values[0];
		gravity[1] = (1 - alpha) * gravity[1] + alpha * event.values[1];
		gravity[2] = (1 - alpha) * gravity[2] + alpha * event.values[2];
		
		previousTime = event.timestamp;
		
		// subtracting gravity gives us any remaining fast movements.
		float hpf_accel[]=new float[3];
		hpf_accel[0] = event.values[0] - gravity[0];
		hpf_accel[1] = event.values[1] - gravity[1];
		hpf_accel[2] = event.values[2] - gravity[2];

		float hpf_magnitude = FloatMath.sqrt(hpf_accel[0]*hpf_accel[0] + hpf_accel[1]*hpf_accel[1] + hpf_accel[2]*hpf_accel[2]);
		
		// log the raw data
		log(String.format("%.4f,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f\n",
						(event.timestamp - startTime)/1000000000.0,
						event.accuracy,
						event.values[0],
						event.values[1],
						event.values[2],
						rawMagnitude,
						hpf_accel[0],
						hpf_accel[1],
						hpf_accel[2],
						hpf_magnitude
				)
			);
	}

}
