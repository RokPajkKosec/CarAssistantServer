package com.cosso.assistant;

import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;

/**
 * @author Rok Pajk Kosec
 * Class used to store data into MongoDB database
 */
@Entity(value="dbEntry", noClassnameStored=true)
public class DbEntry {
	@Id private ObjectId id;
	
	//username
	private String userName;
	//starting latitude of monitored segment
	private double latitude;
	//starting longitde of monitored segment
	private double longitude;
	//average heading of monitored segment
	private int heading;
	//average consumption of monitored segment
	private double consumption;
	//average speed of monitored segment
	private double speed;
	//road type of monitored segment
	private String roadType;
	//speed limint of montiored segment
	private int speedLimit;
	//weather type of monitored segment
	private String weather;
	//wind speed of monitored segment
	private double windSpeed;
	//wind direction of monitored segment
	private int windDirection;
	//temperature of monitored segment
	private double temperature;
	//obstacle on monitored segment if any
	private String obstacleType;
	//if obstacle is blocking path
	private byte noPass;
	//time of entry HH:mm
	private String timestamp;

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public int getHeading() {
		return heading;
	}

	public void setHeading(int heading) {
		this.heading = heading;
	}

	public double getConsumption() {
		return consumption;
	}

	public void setConsumption(double consumption) {
		this.consumption = consumption;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public String getRoadType() {
		return roadType;
	}

	public void setRoadType(String roadType) {
		this.roadType = roadType;
	}

	public int getSpeedLimit() {
		return speedLimit;
	}

	public void setSpeedLimit(int speedLimit) {
		this.speedLimit = speedLimit;
	}

	public String getWeather() {
		return weather;
	}

	public void setWeather(String weather) {
		this.weather = weather;
	}

	public double getWindSpeed() {
		return windSpeed;
	}

	public void setWindSpeed(double windSpeed) {
		this.windSpeed = windSpeed;
	}

	public int getWindDirection() {
		return windDirection;
	}

	public void setWindDirection(int windDirection) {
		this.windDirection = windDirection;
	}

	public double getTemperature() {
		return temperature;
	}

	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}

	public String getObstacleType() {
		return obstacleType;
	}

	public void setObstacleType(String obstacleType) {
		this.obstacleType = obstacleType;
	}

	public byte getNoPass() {
		return noPass;
	}

	public void setNoPass(byte noPass) {
		this.noPass = noPass;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
}
