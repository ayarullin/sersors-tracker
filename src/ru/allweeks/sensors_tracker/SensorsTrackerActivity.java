package ru.allweeks.sensors_tracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SensorsTrackerActivity extends Activity {
    /** Called when the activity is first created. */
	
	Button buttonStart;
	Button buttonStop;
	
	MainService service;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        buttonStart = (Button) findViewById(R.id.buttonStart);
        buttonStop = (Button) findViewById(R.id.buttonStop);
        
        buttonStart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startService(new Intent(SensorsTrackerActivity.this, MainService.class));
			}
		});
        
        buttonStop.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				stopService(new Intent(getApplicationContext(), MainService.class));
			}
		});
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    }
    
    
}