package com.cosso.CBA;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;

/**
 * @author Rok Pajk Kosec
 * Class used for saving mapping tables data into database
 */
@Entity(value="mapTables", noClassnameStored=true)
public class MapTables {
	@Id private ObjectId id;
	
	//Wind resistance divider
	private double windResistanceDiv;
	//Wind resistance mapping table
	private List<Double> windResistanceMap;
	//Minimum windResistance
	private double minWindResistance;
	//Temperature divider
	private double temperatureDiv;
	//Temperature mapping table
	private List<Double> temperatureMap;
	//Minimum temperature
	private double minTemperature;
	//Consumption divider
	private double consumptionDiv;
	//Consumption mapping table
	private List<Double> consumptionMap;
	//Minimum consumption
	private double minConsumption;
	//Username
	private String userName;
	
	/**
	 * Constructor
	 * Initializes class variables
	 */
	public MapTables(){
		windResistanceDiv = 0;
		windResistanceMap = new ArrayList<Double>();
		minWindResistance = 0;
		
		temperatureDiv = 0;
		temperatureMap = new ArrayList<Double>();
		minTemperature = 0;

		consumptionDiv = 0;
		consumptionMap = new ArrayList<Double>();
		minConsumption = 0;
		
		setUserName("");
	}

	public double getWindResistanceDiv() {
		return windResistanceDiv;
	}

	public void setWindResistanceDiv(double windResistanceDiv) {
		this.windResistanceDiv = windResistanceDiv;
	}

	public List<Double> getWindResistanceMap() {
		return windResistanceMap;
	}

	public void setWindResistanceMap(List<Double> windResistanceMap) {
		this.windResistanceMap = windResistanceMap;
	}

	public double getTemperatureDiv() {
		return temperatureDiv;
	}

	public void setTemperatureDiv(double temperatureDiv) {
		this.temperatureDiv = temperatureDiv;
	}

	public List<Double> getTemperatureMap() {
		return temperatureMap;
	}

	public void setTemperatureMap(List<Double> temperatureMap) {
		this.temperatureMap = temperatureMap;
	}

	public double getConsumptionDiv() {
		return consumptionDiv;
	}

	public void setConsumptionDiv(double consumptionDiv) {
		this.consumptionDiv = consumptionDiv;
	}

	public List<Double> getConsumptionMap() {
		return consumptionMap;
	}

	public void setConsumptionMap(List<Double> consumptionMap) {
		this.consumptionMap = consumptionMap;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public double getMinWindResistance() {
		return minWindResistance;
	}

	public void setMinWindResistance(double minWindResistance) {
		this.minWindResistance = minWindResistance;
	}

	public double getMinTemperature() {
		return minTemperature;
	}

	public void setMinTemperature(double minTemperature) {
		this.minTemperature = minTemperature;
	}

	public double getMinConsumption() {
		return minConsumption;
	}

	public void setMinConsumption(double minConsumption) {
		this.minConsumption = minConsumption;
	}
}
