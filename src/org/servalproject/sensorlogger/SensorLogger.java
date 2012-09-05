package org.servalproject.sensorlogger;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public abstract class SensorLogger implements SensorEventListener{
	final Detector detector;
	final Sensor sensor;
	final int interval;
	final String typeName;
	File currentFile;
	final File logFolder;
	static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
	long started=-1;
	long firstEntry=-1;
	DataOutputStream out;
	
	public SensorLogger(Detector detector, Sensor sensor, int interval, String typeName, File logFolder){
		this.detector=detector;
		this.sensor = sensor;
		this.interval = interval;
		this.typeName=typeName;
		this.logFolder=logFolder;
	}
	
	public void start(SensorManager sensorManager){
		sensorManager.registerListener(this, sensor, interval);
	}
	
	public void stop(SensorManager sensorManager){
		sensorManager.unregisterListener(this);
		finishFile();
	}
	
	public void restart(SensorManager sensorManager){
		sensorManager.unregisterListener(this);
		sensorManager.registerListener(this, sensor, interval);
	}
	
	public abstract void logHeader();
	
	public void log(long nanoTime, String data){
		// log the time relative to the first entry in the file
		if (firstEntry==-1)
			firstEntry = nanoTime;
		log(String.format("%.4f,",(nanoTime - firstEntry)/1000000000.0)+data);
	}
	
	public void log(String data){
		try {
			// start a new file every hour
			if (started==-1 || System.currentTimeMillis() - started >= (60*60*1000))
				startFile();
			
			if (out!=null)
				out.writeBytes(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void startFile(){
		finishFile();
		
		firstEntry=-1;
		started = System.currentTimeMillis();
		currentFile=new File(
				logFolder,
				typeName+"_"+sensor.getName()+"_"+dateFormat.format(new Date(started))+".log"
		);
		try {
			out = new DataOutputStream(
					new FileOutputStream(currentFile));
			logHeader();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void finishFile(){
		if (out==null)
			return;
		
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		out = null;
		
		if (currentFile!=null){
			detector.finished(currentFile);
			currentFile=null;
		}
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
