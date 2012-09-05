package org.servalproject.sensorlogger;

import java.io.File;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.util.FloatMath;

public class AccelLogger extends SensorLogger {
	long previousTime=-1;
	float gravity[]=new float[3];
	
	public AccelLogger(Sensor sensor, File logFolder) {
		super(sensor, SensorManager.SENSOR_DELAY_NORMAL, "Accelerometer", logFolder);
	}

	@Override
	public void logHeader() {
		log("time,accuracy,x,y,z,magnitude,hpf_x,hpf_y,hpf_z,hpf_magnitude\n");
	}
	
	public void onSensorChanged(SensorEvent event) {
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
		log(event.timestamp,
				String.format("%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f\n",
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
			);	}
}
