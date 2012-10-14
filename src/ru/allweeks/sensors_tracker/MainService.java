package ru.allweeks.sensors_tracker;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainService extends Service {

	private static final String TAG = "MainService";
	
	ThreadGroup threads = new ThreadGroup("Worker");
	
	SensorManager sensorManager;
	LocationManager locationManager;
	
	SensorEventListener accelerometerListener = new AccelerometerListener();
	SensorEventListener orientationListener = new OrientationListener();
	LocationListener gpsListener = new GpsListener();
	
	Sensor accelerometer, orientation;
	
	BlockingQueue<LogEntity> pushQueue = new LinkedBlockingQueue<LogEntity>();
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
		Log.d(TAG, "onStart");
		
		new Thread(threads, new Worker(), "MainService").start();
		
		sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
		locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
		
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		
		sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(orientationListener, orientation, SensorManager.SENSOR_DELAY_FASTEST);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);
		
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "Service Stopped", Toast.LENGTH_LONG).show();
		
		sensorManager.unregisterListener(accelerometerListener);
		sensorManager.unregisterListener(orientationListener);
		locationManager.removeUpdates(gpsListener);
		
		threads.interrupt();
		Log.d(TAG, "onDestroy");
	}
	
	class GpsListener implements LocationListener
	{
		public void onLocationChanged(Location location) {
			if (null != location)
			{
				LogEntity entity = new LogEntity();
				entity.timestamp = location.getTime();
				entity.type = LogEntity.TYPE_GPS;
				entity.data = new ArrayList<Double>();
				entity.data.add(location.getLatitude());
				entity.data.add(location.getLongitude());
				entity.data.add((double) location.getSpeed());
				entity.data.add(location.getAltitude());
				entity.data.add((double) location.getAccuracy());
			}
		}

		public void onProviderDisabled(String provider) {
			// do nothing
		}

		public void onProviderEnabled(String provider) {
			// do nothing
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			// do nothing
		}
	}
	
	class AccelerometerListener implements SensorEventListener
	{
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// do nothing
		}

		public void onSensorChanged(SensorEvent event) {
			LogEntity entity = new LogEntity();
			entity.timestamp = System.currentTimeMillis();
			entity.type = LogEntity.TYPE_ACCELEROMETER;
			entity.data = new ArrayList<Double>();
			for (int index = 0; index < event.values.length; ++index)
			{
				entity.data.add((double) event.values[index]);
			}
			try {
				pushQueue.put(entity);
			} catch (InterruptedException e) {
				Log.d(TAG, "Accelerometer onSensorChanged Interrupted: " + e.getMessage());
			}
		}
	}
	
	class OrientationListener implements SensorEventListener
	{
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// do nothing
		}

		public void onSensorChanged(SensorEvent event) {
			LogEntity entity = new LogEntity();
			entity.timestamp = System.currentTimeMillis();
			entity.type = LogEntity.TYPE_ORIENTATION;
			entity.data = new ArrayList<Double>();
			for (int index = 0; index < event.values.length; ++index)
			{
				entity.data.add((double) event.values[index]);
			}
			try {
				pushQueue.put(entity);
			} catch (InterruptedException e) {
				Log.d(TAG, "Orientation onSensorChanged Interrupted: " + e.getMessage());
			}
		}
		
	}
	
	class Worker implements Runnable
	{
		FileWriter fileWriter;
		
		public Worker()
		{
			String sdState = android.os.Environment.getExternalStorageState();
			if (sdState.equals(android.os.Environment.MEDIA_MOUNTED)) 
			{
				File sdDir = android.os.Environment.getExternalStorageDirectory();
	            File file = new File(sdDir, "track_data.txt");
	            try {
	                fileWriter = new FileWriter(file);
	                fileWriter.write("DEBUG: " + String.valueOf(System.currentTimeMillis()) + " service started\n");
	            } catch (IOException e) {
	            	Log.d(TAG, "FileWriter: " + e.getMessage());
	            }
			}
			else
			{
				Log.d(TAG, "Storage is unavailable");
			}
		}
		
		public void run() {
			try
			{
				try
				{
					while (true)
					{
						fileWriter.write("DEBUG: Queue size: " + pushQueue.size() + "\n");
						while (!pushQueue.isEmpty())
						{
							LogEntity entity = pushQueue.take();
							
							StringBuilder sb = new StringBuilder();
							sb.append(entity.timestamp + ";" + entity.type);
							
							for (Double value : entity.data)
							{
								sb.append(";");
								sb.append(value);
							}
							
							sb.append("\n");
							
							fileWriter.write(sb.toString());
						}
						fileWriter.flush();
						Thread.sleep(500);
					}
				} catch (InterruptedException e) {
					Log.d(TAG, "Worker interrupted");
					fileWriter.write("DEBUG: " + System.currentTimeMillis() + " interrupted\n");
					fileWriter.flush();
					fileWriter.close();
				} 
			} catch (IOException e) {
				Log.d(TAG, "Worker IOException: " + e.getMessage());
			}
		}	
	}
}
