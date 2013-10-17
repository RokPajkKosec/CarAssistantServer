package com.cosso.CBA;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cosso.assistant.DbEntry;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;

/**
 * @author Rok Pajk Kosec
 * CLass used for processing raw collected data into usable entries for CBA algorithm
 */
public class DataPreprocessingMDB {
	private MongoClient mongoClient;
	private Morphia morphia;
	private Datastore ds;
	//Tables for mapping later data into discrete values
	private MapTables mt;
	//Map for mapping weather types into smaller set of general types
	private Map<String, String> weatherMap;
	
	public static void main(String[] args){
		DataPreprocessingMDB dp = new DataPreprocessingMDB();
		dp.process("rok");
	}
	
	public DataPreprocessingMDB(){
		
	}
	
	/**
	 * @param windSpeed
	 * @param windDirection
	 * @param heading
	 * @param temperature
	 * @param speed
	 * @param speedLimit
	 * @param consumption
	 * @param hour
	 * @param noPass
	 * @param obstacleType
	 * @param roadType
	 * @param weather
	 * @param userName
	 * @return provided parameters in ProcessedEntry class form
	 * Processes provided data and packs it into more cross class handling friendy form.
	 * Uses MapTables class gathered from database for discretization of continuous data
	 */
	public ProcessedEntry processOne(double windSpeed, int windDirection, int heading, double temperature, double speed, 
			int speedLimit, double consumption, int hour, byte noPass, String obstacleType, String roadType, String weather, 
			String userName){
		
		ProcessedEntry pEntry = new ProcessedEntry();
		
		/*
		 * if mapping tables or weather map table is not initialized we gather them from database (during path search this
		 * hapens only on first call of this method
		 */
		if(mt == null || weatherMap == null){
			try {
				openConnection();
				
				mt = ds.createQuery(MapTables.class).filter("userName =", userName).get();
				
				weatherMap = new HashMap<String, String>();
				DBCollection coll = mongoClient.getDB("userData").getCollection("weather");
				DBCursor cursor = coll.find();
				while(cursor.hasNext()){
					cursor.next();
					String w1 = cursor.curr().get("weatherType").toString();
					String w2 = cursor.curr().get("weatherGroupedType").toString();
					weatherMap.put(w1, w2);
				}
				
				close();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		
		//we calculate wind index that tells us to which walue we map wind attributes
		int windIndex = (int)(((windSpeed*Math.cos(Math.abs((windDirection - heading) % 180))) - mt.getMinWindResistance()) / mt.getWindResistanceDiv());
		double tempWindResistance;
		//if we get nubmer that is larger than max index we use max index
		if(windIndex > mt.getWindResistanceMap().size() - 1){
			tempWindResistance = mt.getWindResistanceMap().get(mt.getWindResistanceMap().size()-1);
		}
		//if we get number that is smaller that min index we use index 0
		else if(windIndex < 0){
			tempWindResistance = mt.getWindResistanceMap().get(0);
		}
		//after checking special cases we provide mapped value with calculated index
		else{
			tempWindResistance = mt.getWindResistanceMap().get(windIndex);
		}
		
		/*
		 * the same thing happens for temperature and for consumption. Speed difference is mapped using static map function
		 */
		
		int temperatureIndex = (int)((temperature - mt.getMinTemperature()) / mt.getTemperatureDiv());
		double tempTemperature;
		if(temperatureIndex > mt.getTemperatureMap().size() - 1){
			tempTemperature = mt.getTemperatureMap().get(mt.getTemperatureMap().size() - 1);
		}
		else if(temperatureIndex < 0){
			tempTemperature = mt.getTemperatureMap().get(0);
		}
		else{
			tempTemperature = mt.getTemperatureMap().get(temperatureIndex);
		}
		
		int tempSpeedDif = speedDifStatic(speed - speedLimit);
		
		int consumptionIndex = (int)((consumption - mt.getMinConsumption()) / mt.getConsumptionDiv());
		double tempConsumption;
		if(consumptionIndex > mt.getConsumptionMap().size() - 1){
			tempConsumption = mt.getConsumptionMap().get(mt.getConsumptionMap().size() - 1);
		}
		else if(consumptionIndex < 0){
			tempConsumption = mt.getConsumptionMap().get(0);
		}
		else{
			tempConsumption = mt.getConsumptionMap().get(consumptionIndex);
		}
		
		//we set all needed values to ProcessedEntry object and return it
		pEntry.setWindResistance(tempWindResistance);
		pEntry.setTemperature(tempTemperature);
		pEntry.setSpeedDifference(tempSpeedDif);
		pEntry.setConsumption(tempConsumption);
		pEntry.setTimestamp(hour);
		pEntry.setNoPass(noPass);
		pEntry.setObstacleType(obstacleType);
		pEntry.setRoadType(roadType);
		pEntry.setWeather(weatherMap.get(weather));
		pEntry.setUserName(userName);
		pEntry.setSpeedLimit(speedLimit);
		
		return pEntry;
	}
	
	/**
	 * @param userName
	 * @return status code describing success of data processing
	 * Processes all data of given user in DbEntry collection and saves it in ProcessedEntry collection.
	 * It creates new MapTables collection with new mapping tables for discretization of data
	 */
	public int process(String userName){
		int returnValue = 0;
		
		//Initialization of all needed variables and lists
		double minWindResistance = 0;
		double maxWindResistance = 0;
		double windResistanceDiv = 0;
		List<Double> windResistanceMap = new ArrayList<Double>();
		
		double minTemperature = 0;
		double maxTemperature = 0;
		double temperatureDiv = 0;
		List<Double> temperatureMap = new ArrayList<Double>();
		
		double minConsumption = 0;
		double maxConsumption = 0;
		double consumptionDiv = 0;
		List<Double> consumptionMap = new ArrayList<Double>();
		
		try {
			openConnection();
			
			//cleans database of existing rules before creating new ones. The same happens to mapping tables
			ds.delete(ds.createQuery(ProcessedEntry.class).filter("userName =", userName));
			ds.delete(ds.createQuery(MapTables.class).filter("userName =", userName));
			
			/*
			 * next section finds minimum and maximum values of temperature, wind resistance and consumption
			 */
			minTemperature = ds.createQuery(DbEntry.class).filter("userName =", userName).order("temperature").get().getTemperature();
			maxTemperature = ds.createQuery(DbEntry.class).filter("userName =", userName).order("-temperature").get().getTemperature();
			
			Query<DbEntry> query = ds.createQuery(DbEntry.class).filter("userName =", userName);
					
			double tempResistance = 0;
			
			for(DbEntry x : query){
				tempResistance = x.getWindSpeed()*Math.cos(Math.abs((x.getWindDirection() - x.getHeading()) % 180));
				if(tempResistance > maxWindResistance){
					maxWindResistance = tempResistance;
				}
				if(tempResistance < minWindResistance){
					minWindResistance = tempResistance;
				}
			}
			
			minConsumption = ds.createQuery(DbEntry.class).filter("userName =", userName).order("consumption").get().getConsumption();
			maxConsumption = ds.createQuery(DbEntry.class).filter("userName =", userName).order("-consumption").get().getConsumption();
			
			System.out.println(minTemperature + " " + maxTemperature);
			System.out.println(minWindResistance + " " + maxWindResistance);
			System.out.println(minConsumption + " " + maxConsumption);
			
			close();
		} catch (Exception e) {
			e.printStackTrace();
			returnValue = 1;
		}
		
		/*
		 * next section gets dividers and mapping tables for wind resistance, temperature and consumption
		 * 0.0001 correction is used for avoiding out of bounds exception and multiplying/dividing is used
		 * for rounding to 2 decimal points
		 */
		
		windResistanceDiv = (maxWindResistance - minWindResistance + 0.0001) / 10;
		windResistanceMap.add(Math.round(((double)windResistanceDiv / 2 + minWindResistance) * 100.0) / 100.0);
		for(int i=0; i<9; i++){
			windResistanceMap.add(Math.round((windResistanceMap.get(i)+windResistanceDiv) * 100.0) / 100.0);
		}
		for(double x : windResistanceMap){
			System.out.print(x + " ");
		}
		System.out.println();
		
		temperatureDiv = (maxTemperature - minTemperature + 0.0001) / 5;
		temperatureMap.add(Math.round(((double)temperatureDiv / 2 + minTemperature) * 100.0) / 100.0);
		for(int i=0; i<4; i++){
			temperatureMap.add(Math.round((temperatureMap.get(i)+temperatureDiv) * 100.0) / 100.0);
		}
		for(double x : temperatureMap){
			System.out.print(x + " ");
		}
		System.out.println();
		
		consumptionDiv = (maxConsumption - minConsumption + 0.0001) / 10;
		consumptionMap.add(Math.round(((double)consumptionDiv / 2 + minConsumption) * 100.0) / 100.0);
		for(int i=0; i<9; i++){
			consumptionMap.add(Math.round((consumptionMap.get(i)+consumptionDiv) * 100.0) / 100.0);
		}
		for(double x : consumptionMap){
			System.out.print(x + " ");
		}
		System.out.println();
		
		MapTables mapT = new MapTables();
		
		//saving computed values and tables into object used for saving this user data into database
		mapT.setConsumptionDiv(consumptionDiv);
		mapT.setConsumptionMap(consumptionMap);
		mapT.setMinConsumption(minConsumption);
		mapT.setTemperatureDiv(temperatureDiv);
		mapT.setTemperatureMap(temperatureMap);
		mapT.setMinTemperature(minTemperature);
		mapT.setWindResistanceDiv(windResistanceDiv);
		mapT.setWindResistanceMap(windResistanceMap);
		mapT.setMinWindResistance(minWindResistance);
		mapT.setUserName(userName);
		
		double tempTemperature;
		double tempWindResistance;
		double tempSpeedDif;
		double tempConsumption;
		int tempTime;
		
		try {
			openConnection();
			
			ds.save(mapT);
						
			Query<DbEntry> query = ds.createQuery(DbEntry.class).filter("userName =", userName);
			
			/*
			 * for every DbEntry in database we map wind resistance, temperature, speed difference and consumption and
			 * save it into database using ProcessedEntry class
			 */
			for(DbEntry x : query){
				tempWindResistance = windResistanceMap.get((int)(((x.getWindSpeed()*Math.cos(Math.abs((x.getWindDirection()
						- x.getHeading()) % 180))) - minWindResistance) / windResistanceDiv));
				tempTemperature = temperatureMap.get((int)((x.getTemperature() - minTemperature) / temperatureDiv));
				
				tempSpeedDif = speedDifStatic(x.getSpeed() - x.getSpeedLimit());
				tempConsumption = consumptionMap.get((int)((x.getConsumption() - minConsumption) / consumptionDiv));
				
				String delims = "[:]+";
				String[] tokens = x.getTimestamp().split(delims);
				tempTime = Integer.valueOf(tokens[0]);
				
				ProcessedEntry pEntry = new ProcessedEntry();
				pEntry.setWindResistance(tempWindResistance);
				pEntry.setTemperature(tempTemperature);
				pEntry.setSpeedDifference(tempSpeedDif);
				pEntry.setConsumption(tempConsumption);
				pEntry.setTimestamp(tempTime);
				pEntry.setNoPass(x.getNoPass());
				pEntry.setObstacleType(x.getObstacleType());
				pEntry.setRoadType(x.getRoadType());
				pEntry.setSpeedLimit(x.getSpeedLimit());
				
				DBCollection coll = mongoClient.getDB("userData").getCollection("weather");
				BasicDBObject query1 = new BasicDBObject("weatherType", x.getWeather());
				DBCursor cursor = coll.find(query1);
				if(cursor.hasNext()){
					String weather = cursor.next().get("weatherGroupedType").toString();
					pEntry.setWeather(weather);
				}
				else{
					pEntry.setWeather("Unknown");
				}

				pEntry.setUserName(x.getUserName());
				
				ds.save(pEntry);
			}
			
			close();
		} catch (Exception e) {
			e.printStackTrace();
			returnValue = 2;
		}

		System.out.println("returnValue1 " + returnValue);
		return returnValue;
	}
	
	/**
	 * @param speedDif
	 * @return discrete value of continuous input of speed difference
	 */
	private int speedDifStatic(double speedDif){
		if(speedDif < -22.5){
			return -25;
		}
		else if(-22.5 <= speedDif && speedDif < -17.5){
			return -20;
		}
		else if(-17.5 <= speedDif && speedDif < -12.5){
			return -15;
		}
		else if(-12.5 <= speedDif && speedDif < -7.5){
			return -10;
		}
		else if(-7.5 <= speedDif && speedDif < -2.5){
			return -5;
		}
		else if(-2.5 <= speedDif && speedDif < 2.5){
			return 0;
		}
		else if(2.5 <= speedDif && speedDif < 7.5){
			return 5;
		}
		else if(7.5 <= speedDif && speedDif < 12.5){
			return 10;
		}
		else if(12.5 <= speedDif && speedDif < 17.5){
			return 15;
		}
		else if(17.5 <= speedDif && speedDif < 22.5){
			return 20;
		}
		else if(22.5 <= speedDif){
			return 25;
		}
		else{
			return 0;
		}
	}
	
	/**
	 * @throws UnknownHostException
	 * Opens data connection
	 */
	public void openConnection() throws UnknownHostException{
		mongoClient = new MongoClient( "localhost" , 27017 );
		morphia = new Morphia();
		ds = morphia.createDatastore((Mongo)mongoClient, "userData");
		morphia.map(ProcessedEntry.class);		
		ds.ensureIndexes(); //creates indexes from @Index annotations in your entities
		ds.ensureCaps(); //creates capped collections from @Entity
	}

	/**
	 * Closes data connection
	 */
	public void close(){
		mongoClient.close();
	}
}
