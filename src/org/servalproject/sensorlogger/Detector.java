package org.servalproject.sensorlogger;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class Detector extends Service{
	private static final String TAG ="SensorLogger";
	SensorManager sensorManager;
	PowerManager powerManager;
	List<SensorLogger> loggers;
	WakeLock lock;
	static boolean running = false;
	static String typeNames[];
	
	BroadcastReceiver receiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
				// re-register sensor to ensure we can receive events while the screen is off
				Log.v(TAG, "Restarting sensors as the screen has turned off");
				for (int i=0;i<loggers.size();i++){
					loggers.get(i).restart(sensorManager);
				}
			}else if(intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)){
				// TODO check if level below threshold
			}
		}
	};
	
	@Override
	public void onDestroy() {
		Log.v(TAG, "Stopping capture of sensor data");
		for (int i=0;i<loggers.size();i++){
			loggers.get(i).stop(sensorManager);
		}
		loggers=null;
		this.unregisterReceiver(receiver);
		
		if (lock!=null){
			lock.release();
			lock=null;
		}
		running = false;
		super.onDestroy();
	}

	// TODO configurable??
	String sendVia="org.servalproject";
	
	public void finished(File file){
		// TODO compress & add name for this device
		
		// share file
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
		intent.setType("text/plain");
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		for (ResolveInfo r : getPackageManager().queryIntentActivities(intent, 0)) {
			if (r.activityInfo.packageName.equals(sendVia)) {
				intent.setClassName(r.activityInfo.packageName,r.activityInfo.name);
				
				this.startActivity(intent);
				return;
			}
		}
		Log.v(TAG,"Did not find ACTION_SEND handler with package matching "+sendVia+". Is it installed?");
	}
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG, "Starting capture of sensor data");
		sensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
		powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
		
		lock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Accelerometer");
		lock.acquire();
		
		// Why doesn't android use enum's? sigh...
		if (typeNames==null){
			Field fields[]=Sensor.class.getDeclaredFields();
			typeNames = new String[30];
			for (int i=0;i<fields.length;i++){
				String name = fields[i].getName();
				if (!name.startsWith("TYPE_"))
					continue;
				if (name.equals("TYPE_ALL"))
					continue;
				try {
					int value = fields[i].getInt(null);
					if (value>=0 && value <typeNames.length)
						typeNames[value]=name;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		File logFolder = this.getExternalFilesDir(null);
		
		// look for all interesting sensors
		List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
		loggers = new ArrayList<SensorLogger>();
		
		for (int i=0;i<sensors.size();i++){
			Sensor s = sensors.get(i);
			
			Log.v(TAG, "Name: "+s.getName());
			Log.v(TAG, "Maximum Range: "+s.getMaximumRange());
			Log.v(TAG, "Power: "+s.getPower());
			Log.v(TAG, "Resolution: "+s.getResolution());
			Log.v(TAG, "Vendor: "+s.getVendor());
			Log.v(TAG, "Version: "+s.getVersion());
			if (s.getType()>=0 && s.getType() < typeNames.length)
				Log.v(TAG, "Type: "+typeNames[s.getType()]);
			else
				Log.v(TAG, "Type: "+s.getType());
			
			switch(s.getType()){
			case Sensor.TYPE_ACCELEROMETER:
				SensorLogger l=new AccelLogger(this, s, logFolder);
				l.start(sensorManager);
				loggers.add(l);
				break;
				
			case Sensor.TYPE_TEMPERATURE:
				// TODO
				break;
			}
		}
		
		// TODO temp / battery / device identifier
		IntentFilter intentFilter=new IntentFilter();
		intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
		intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
		this.registerReceiver(receiver, intentFilter);
		
		running = true;
		
		// tell android to restart the service if it is killed
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
}
