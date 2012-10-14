package ru.allweeks.sensors_tracker;

import java.util.ArrayList;

public class LogEntity {
	public long timestamp;
	public int type;
	public ArrayList<Double> data;
	
	public static final int TYPE_GPS = 1;
	public static final int TYPE_ACCELEROMETER = 2;
	public static final int TYPE_ORIENTATION = 3;
}
