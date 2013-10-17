package com.cosso.CBA;
import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;

/**
 * @author Rok Pajk Kosec
 * Class used to store processed data into MongoDB database
 */
@Entity(value="processedEntry", noClassnameStored=true)
public class ProcessedEntry {
	@Id private ObjectId id;
	
	//username
	private String userName;
	//consumption
	private double consumption;
	//speed difference
	private double speedDifference;
	
	/*
	 *  dodaj nazaj!!!! keys, switch v tem classu / done
	 *  v dataPreprocessing dodaj speedLimit in speed v processEntry - ni potrebe po preracunavanju - kopiraj surovo / done
	 *  v CbaRg dodaj pogoje za skip podatkov glede na to kaj se racuna / done, ker speed limit se ne preskakuje, speeda pa ni
	 *  v classificatorju dodaj pogoje za skup podatkov glede na to kaj se racuna / done, isto kot zgoraj
	 *  zaporedje popravkov: processedEntry, dataPreprocesing, CbaRg, Classificator / done
	 */
	
	//speed limit
	private int speedLimit;
	//road type
	private String roadType;
	//weather type
	private String weather;
	//wind resistance coefficient
	private double windResistance;
	//temperature
	private double temperature;
	//obstacle type
	private String obstacleType;
	//if obstacle can not be passed
	private byte noPass;
	//time stamp (hour)
	private int timestamp;
	//keys table for key value function
	private String[] keys;
	
	/**
	 * Constructor
	 */
	public ProcessedEntry(){
		keys = new String[9];
		keys[7] = "consumption";
		keys[2] = "speedDifference";
		keys[4] = "roadType";
		keys[6] = "weather";
		keys[0] = "windResistance";
		keys[1] = "temperature";
		keys[5] = "obstacleType";
		keys[3] = "timestamp";
		keys[8] = "speedLimit";
	}
	
	/**
	 * @param key
	 * @return value
	 * Returns value of provided key
	 */
	public Object getValue(String key){
		switch(key){
			case "consumption":
				return consumption;
			case "speedDifference":
				return speedDifference;
			case "roadType":
				return roadType;
			case "weather":
				return weather;
			case "windResistance":
				return windResistance;
			case "temperature":
				return temperature;
			case "obstacleType":
				return obstacleType;
			case "timestamp":
				return timestamp;
			case "speedLimit":
				return speedLimit;
			default:
				return null;
		}
	}

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public double getConsumption() {
		return consumption;
	}

	public void setConsumption(double consumption) {
		this.consumption = consumption;
	}

	public String getRoadType() {
		return roadType;
	}

	public void setRoadType(String roadType) {
		this.roadType = roadType;
	}

	public String getWeather() {
		return weather;
	}

	public void setWeather(String weather) {
		this.weather = weather;
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

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public double getSpeedDifference() {
		return speedDifference;
	}

	public void setSpeedDifference(double speedDifference) {
		this.speedDifference = speedDifference;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	public String[] getKeys() {
		return keys;
	}

	public double getWindResistance() {
		return windResistance;
	}

	public void setWindResistance(double windResistance) {
		this.windResistance = windResistance;
	}

	public int getSpeedLimit() {
		return speedLimit;
	}

	public void setSpeedLimit(int speedLimit) {
		this.speedLimit = speedLimit;
	}
}
