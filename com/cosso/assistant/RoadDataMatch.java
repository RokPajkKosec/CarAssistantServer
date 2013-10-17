package com.cosso.assistant;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * @author Rok Pajk Kosec
 * Class for matching gathered road data to map structure
 */
public class RoadDataMatch {
    private Connection dbConnection;

    private Node nodeFix;
		
	public RoadDataMatch(){
		nodeFix = new Node();
	}
	
	private static double square(double expression){
		return expression * expression;
	}
	
	/**
	 * @param roadLatitude
	 * @param roadLongitude
	 * @return id of closes edge to obstacle GPS coordinates
	 */
	public long getClosestEdge(double roadLatitude, double roadLongitude){
		nodeFix.setLatitude(roadLatitude);
		nodeFix.setLongitude(roadLongitude);
		nodeFix.setId(0);
		dbConnection = SQLiteConnection.connection();
		ArrayList<Node> nodes = closestNodes();
		String query = "";
		String query1 = "";
		PreparedStatement statement = null;
		ResultSet result = null;
		PreparedStatement statement1 = null;
		ResultSet result1 = null;
		double startLat = 0;
		double startLong = 0;
		double endLat = 0;
		double endLong = 0;;
		double minDistance = Double.MAX_VALUE;
		double d;
		long closestEdge = 0;
		
		try {
			for(Node n : nodes){
				query = "SELECT edgeId, startId, endId, edgeHeading FROM edge WHERE startId=" + String.valueOf(n.getId()) + " OR endId=" + String.valueOf(n.getId());
				statement = dbConnection.prepareStatement(query);
				result = statement.executeQuery();
				while(result.next()){
					query1 = "SELECT nodeLat, nodeLong FROM node WHERE nodeId=" + result.getString("startId");
					statement1 = dbConnection.prepareStatement(query1);
					result1 = statement1.executeQuery();
					if(result1.next()){
						startLat = result1.getDouble("nodeLat");
						startLong = result1.getDouble("nodeLong");
					}
					query1 = "SELECT nodeLat, nodeLong FROM node WHERE nodeId=" + result.getString("endId");
					statement1 = dbConnection.prepareStatement(query1);
					result1 = statement1.executeQuery();
					if(result1.next()){
						endLat = result1.getDouble("nodeLat");
						endLong = result1.getDouble("nodeLong");
					}
					/*
					 * if angle between fix, start point of edge and end point of edge is more than 90 degrees, distance between location fix
					 * and edge is distance between location fix and start point of edge
					 */
					if(Math.cos(Math.toRadians(GeoCalculator.initialBearing(startLat, startLong, nodeFix.getLatitude(), nodeFix.getLongitude()) - 
							result.getDouble("edgeHeading"))) <= 0){
						d = GeoCalculator.haversine(startLat, startLong, nodeFix.getLatitude(), nodeFix.getLongitude());
					}
					/*
					 * if angle between fix, end point of edge and start point of edge is mode than 90 degrees, distance between location fix
					 * and edge is distance between location fix and end point of edge
					 */
					else if(Math.cos(Math.toRadians(GeoCalculator.initialBearing(endLat, endLong, nodeFix.getLatitude(), nodeFix.getLongitude()) - 
							(result.getDouble("edgeHeading")+180))%360) <= 0){
						d = GeoCalculator.haversine(endLat, endLong, nodeFix.getLatitude(), nodeFix.getLongitude());
					}
					/*
					 * If above conditions are not met, than distance between location fix and edge is perpendicular distance between them
					 */
					else{
						d = Math.abs((nodeFix.getLongitude() * (startLat - endLat) - nodeFix.getLatitude() * (startLong - endLong) + 
								(startLong * endLat - endLong * startLat))/(Math.sqrt(square(startLong - endLong) + square(startLat - endLat))))*100000;
					}
					//if current edge is closer then assign this as new closest edge
					if(d < minDistance){
						minDistance = d;
						closestEdge = result.getLong("edgeId");
					}
				}
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		finally{
			try {
	            if (statement != null) {
	                statement.close();
	            }
	            if (statement1 != null) {
	                statement1.close();
	            }
	            if (result != null){
	            	result.close();
	            }
	            if (result1 != null){
	            	result1.close();
	            }
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		SQLiteConnection.disconnect(dbConnection);
		
		return closestEdge;
	}
	
	/**
	 * @return arrayList of closest nodes to GPS coordinates
	 */
	private ArrayList<Node> closestNodes(){
		long id = -1;
		double latitudeMap;
		double longitudeMap;
		ArrayList<Node> nodes = new ArrayList<Node>();

		PreparedStatement statement = null;
		ResultSet result = null;
		String query = "SELECT nodeId, nodeLat, nodeLong FROM node WHERE nodeLat<=" + String.valueOf(nodeFix.getLatitude()+0.005) + " AND nodeLat>=" +
						String.valueOf(nodeFix.getLatitude()-0.005) + " AND nodeLong<=" + String.valueOf(nodeFix.getLongitude()+(0.005*90/nodeFix.getLatitude())) +
						" AND nodeLong>=" + String.valueOf(nodeFix.getLongitude()-(0.005*90/nodeFix.getLatitude()));
		
		try {
			statement = dbConnection.prepareStatement(query);
			result = statement.executeQuery();
			while(result.next()){
				latitudeMap = result.getDouble("nodeLat");
				longitudeMap = result.getDouble("nodeLong");
				id = result.getLong("nodeId");
				nodes.add(new Node(latitudeMap, longitudeMap, id));

			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		finally{
			try {
	            if(statement != null){
	                statement.close();
	            }
	            if(result != null){
	            	result.close();
	            }
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return nodes;
	}
}
