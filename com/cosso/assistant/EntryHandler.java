package com.cosso.assistant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Rok Pajk Kosec
 * Class used for handling raw data and for saving processed data into MySQL database
 */
public class EntryHandler {
	
	//MySQL database connection
	private Connection connection;

	public EntryHandler(){

	}

	/**
	 * @param table
	 * @param attributes
	 * @param values
	 * @throws SQLException
	 * Saves data into database
	 */
	public void insertDB(String table, String attributes, String values) throws SQLException {
		PreparedStatement statement = null;

		statement = connection.prepareStatement("INSERT INTO " + table + "(" + attributes + ") VALUES (" + values + ")");
		statement.executeUpdate();

		if (statement != null) {
			statement.close();
		}

	}
	
	/**
	 * @param entry
	 * Reciecs Entry class and appends weather and road data. If weather flag is false it uses weather data from previous entry
	 */
	@SuppressWarnings("resource")
	public void addEntry(Entry entry){
		WeatherWU weather = new WeatherWU();;
		
		PreparedStatement statement = null;
		ResultSet result = null;
		ResultSet result1 = null;
		
		String query;
		String[] roadData;
		String obstacle;
		int obstacleId = 0;
		int userId = 0;
		int roadTypeId = 0;
		int weatherId = 0;
		
		roadData = RoadData.getObstacleByWay(entry.getWayId());
		obstacle = roadData[0];
		
		if(obstacle.equals("No freight traffic")){
			obstacle = "No obstacle";
		}
		
		try {
			query = "SELECT obstacleId FROM obstacle WHERE obstacleType = \"" + obstacle + "\"";
			statement = connection.prepareStatement(query);
			result = statement.executeQuery();
			if(result.next()){
				obstacleId = result.getInt("obstacleId");
			}
			
			query = "SELECT userId FROM user WHERE userName = \"" + entry.getUsername() + "\"";
			statement = connection.prepareStatement(query);
			result = statement.executeQuery();
			if(result.next()){
				userId = result.getInt("userId");
			}
			
			query = "SELECT roadTypeId FROM roadType WHERE roadTypeName = \"" + entry.getRoadType() + "\"";
			statement = connection.prepareStatement(query);
			result = statement.executeQuery();
			if(result.next()){
				roadTypeId = result.getInt("roadTypeId");
			}
			
			if(entry.getWeatherDataCheck()){
				weather.getWeatherData(entry.getLatitude(), entry.getLongitude());
				query = "SELECT weatherId FROM weather WHERE weatherType=\"" + weather.getWeather() + "\"";
				statement = connection.prepareStatement(query);
				result = statement.executeQuery();
				if(result.next()){
					weatherId = result.getInt("weatherId");
				}
			}
			else{
				query = "SELECT * FROM userData WHERE userId = " + userId + " ORDER BY  userDataId DESC LIMIT 1";
				statement = connection.prepareStatement(query);
				result = statement.executeQuery();
				if(result.next()){
					weatherId = result.getInt("weatherId");
					
					weather.setTemperature(result.getDouble("temperature"));
					weather.setWindDegrees(result.getInt("windDirection"));
					weather.setWindSpeed(result.getDouble("windSpeed"));
				}
			}
			
			insertDB("userData", "obstacleId, roadTypeId, userId, weatherId, windSpeed, windDirection, temperature, speedLimit, latitude, longitude, speed, consumption, heading", 
					obstacleId + "," + roadTypeId + "," + userId + "," + weatherId + ", " + weather.getWindSpeed() + "," + weather.getWindDegrees() + ","
					+ weather.getTemperature() + "," + entry.getSpeedLimit() + "," + entry.getLatitude() + "," + entry.getLongitude() + ","
					+ entry.getSpeed() + "," + entry.getConsumption() + "," + entry.getHeading());
			
		}
		catch (Exception e) {
			e.printStackTrace();
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
	}
	
	/**
	 * @throws Exception
	 * Opens database connection
	 */
	public void openConnection() throws Exception{
		connection = DatabaseConnection.connection();
	}

	/**
	 * @throws SQLException
	 * Closes database connection
	 */
	public void close() throws SQLException{
		DatabaseConnection.disconnect(connection);
	}

}
