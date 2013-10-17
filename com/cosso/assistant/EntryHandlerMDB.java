package com.cosso.assistant;

import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.util.Date;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;

/**
 * @author Rok Pajk Kosec
 * Class used for handling raw data and for saving processed data into MongoDB database
 */
public class EntryHandlerMDB {

	private MongoClient mongoClient;
	private Morphia morphia;
	private Datastore ds;

	public EntryHandlerMDB(){
		
	}
	
	/**
	 * @param entry
	 * @return code indication success status
	 * Reciecs Entry class and appends weather and road data. If weather flag is false it uses weather data from previous entry.
	 * For saving data into database it uses form mapped from DbEntry class
	 */
	public int addEntry(Entry entry){
		DbEntry dbEntry = new DbEntry();
		
		//set dbEntry with fields from entry
		dbEntry.setConsumption(entry.getConsumption());
		dbEntry.setHeading(entry.getHeading());
		dbEntry.setLatitude(entry.getLatitude());
		dbEntry.setLongitude(entry.getLongitude());
		dbEntry.setSpeed(entry.getSpeed());
		dbEntry.setSpeedLimit(entry.getSpeedLimit());
		dbEntry.setUserName(entry.getUsername());
		dbEntry.setRoadType(entry.getRoadType());
				
		WeatherWU weather = new WeatherWU();
		
		PreparedStatement statement = null;
		ResultSet result = null;
		ResultSet result1 = null;
		
		String obstacle;
		String noPass;
		String[] roadData;
		
		roadData = RoadData.getObstacleByWay(entry.getWayId()); //ql ker je itak v sqlite
		obstacle = roadData[0];
		noPass = roadData[1];
		
		if(obstacle.equals("No freight traffic")){
			obstacle = "No obstacle";
		}
		
		//set dbEntry road data
		dbEntry.setObstacleType(obstacle);
		dbEntry.setNoPass(Byte.valueOf(noPass));
		
		try {	
			DB db = ds.getDB();
			DBCollection coll = db.getCollection("users");
			BasicDBObject query1 = new BasicDBObject("userName", entry.getUsername()).append("userPass", entry.getUserpass());
			DBCursor cursor = coll.find(query1);
			
			if(!cursor.hasNext()) {
				return 288;
			}
			
			cursor.close();

			if(entry.getWeatherDataCheck()){
				weather.getWeatherData(entry.getLatitude(), entry.getLongitude());
			}
			else{
				coll = db.getCollection("dbEntry");
				cursor = coll.find(new BasicDBObject("userName", entry.getUsername())).sort(new BasicDBObject("_id", -1)).limit(1);
				if(cursor.hasNext()){
					DBObject object = cursor.next();
					weather.setWeather(object.get("weather").toString());
					weather.setTemperature(Double.valueOf(object.get("temperature").toString()));
					weather.setWindDegrees(Integer.valueOf(object.get("windDirection").toString()));
					weather.setWindSpeed(Double.valueOf(object.get("windSpeed").toString()));
				}
			}
			
			//set dbEntry weather data
			dbEntry.setWeather(weather.getWeather());
			dbEntry.setTemperature(weather.getTemperature());
			dbEntry.setWindDirection(weather.getWindDegrees());
			dbEntry.setWindSpeed(weather.getWindSpeed());
			
			dbEntry.setTimestamp(DateFormat.getTimeInstance().format(new Date(System.currentTimeMillis())).substring(0, 5));
			
			//insert dbEntry to mongodb
			ds.save(dbEntry);			
		}
		catch (Exception e) {
			e.printStackTrace();
			return 299;
		}
		finally{
			try {
	            if (statement != null) {
	                statement.close();
	            }
	            if (result != null){
	            	result.close();
	            }
	            if (result1 != null){
	            	result1.close();
	            }	            
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return 201;
	}
	
	/**
	 * @throws UnknownHostException
	 * Opens database connection and mapes DbEntry class
	 */
	public void openConnection() throws UnknownHostException{
		mongoClient = new MongoClient( "localhost" , 27017 );
		morphia = new Morphia();
		ds = morphia.createDatastore((Mongo)mongoClient, "userData");
		morphia.map(DbEntry.class);		
		ds.ensureIndexes(); //creates indexes from @Index annotations in your entities
		ds.ensureCaps(); //creates capped collections from @Entity
	}

	/**
	 * Closes database connection
	 */
	public void close(){
		mongoClient.close();
	}

}