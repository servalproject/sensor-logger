package org.servalproject.sensorlogger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class Main extends Activity implements OnClickListener {
	Button start;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.main);
		start = (Button) this.findViewById(R.id.start);
		
		start.setOnClickListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		setText(Detector.running);
		TextView deviceId = (TextView)this.findViewById(R.id.device_id);
		deviceId.setText("Device ID: "+Detector.getId(this));
	}

	public void setText(boolean started){
		start.setText(started?"Stop":"Start");
	}
	
	public void onClick(View arg0) {
		
		Intent serviceIntent = new Intent(this, Detector.class);
		if (Detector.running){
			stopService(serviceIntent);
			setText(false);
		}else{
			startService(serviceIntent);
			setText(true);
		}
	}

}
