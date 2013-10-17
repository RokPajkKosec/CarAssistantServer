package com.cosso.AStar;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.cosso.assistant.SQLiteConnection;

/**
 * @author Rok Pajk Kosec
 * Class that represents node on map data with added atributes for calculation of
 * best route using A star algorithm
 */
public class AStarNode {
	//Node id
	private long id;
	//Node latitude
	private double latitude;
	//Node longitude
	private double longitude;
	//Node id of predecessor node along path
	private AStarNode cameFrom;
	//g value (path cost from start to this node)
	private double g;
	//h value (estimated cost to target node - air distance in this case)
	private double h;
	//list of edges that start or end in this node
	private List<AStarEdge> out;

	/**
	 * Empty constructor
	 */
	public AStarNode(){
		this.out = new ArrayList<AStarEdge>();
	}
	
	/**
	 * @param id
	 * @param latitude
	 * @param longitude
	 * Constructor that sets latitude, longitde and node id
	 */
	public AStarNode(long id, double latitude, double longitude) {
		this.out = new ArrayList<AStarEdge>();
		this.id = id;
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public static void main(String[] args){
		AStarNode n = new AStarNode(1362804906, 46.1480625, 14.4826823);
		Connection dbConnection = SQLiteConnection.connection();
		n.getNeighbourEdges(dbConnection);
		SQLiteConnection.disconnect(dbConnection);
	}

	/**
	 * @param dbConnection
	 * Gathers all edges from map data that have starting or ending point in this node and adds them into
	 * out array
	 */
	public void getNeighbourEdges(Connection dbConnection){

		PreparedStatement statement = null;
		ResultSet result = null;
		String query = "";
		
		long edgeId;
		String obstacleType = "";
		byte obstacleNoPass = 0;
		
		String query1 = "";
		PreparedStatement statement1 = null;
		ResultSet result1 = null;
		
		int wayMaxSpeed = 0;
		
		try {
			query = "SELECT edgeId, edgeHeading, edgeLength, endId, wayTypeName, wayMaxSpeed, wayTypeDefSpeed " +
					"FROM edge NATURAL JOIN way NATURAL JOIN wayType WHERE startId=" + id + 
					" AND endId!=" + id;
			
			statement = dbConnection.prepareStatement(query);
			result = statement.executeQuery();
			while(result.next()){
				edgeId = result.getLong("edgeId");

				query1 = "SELECT obstacleType, obstacleNoPass " +
						"FROM edge NATURAL JOIN obstacle WHERE edgeId =" + edgeId;
				statement1 = dbConnection.prepareStatement(query1);
				result1 = statement1.executeQuery();
				if(result1.next()){
					obstacleType = result1.getString("obstacleType");
					obstacleNoPass = result1.getByte("obstacleNoPass");
				}
				
				if(obstacleNoPass == 1){
					continue;
				}
				
				if(result.getInt("wayMaxSpeed") == 0){
					wayMaxSpeed = result.getInt("wayTypeDefSpeed");
				}
				else{
					wayMaxSpeed = result.getInt("wayMaxSpeed");
				}
				
				out.add(new AStarEdge(edgeId, id, result.getLong("endId"), result.getDouble("edgeLength"), 
						result.getInt("edgeHeading"), result.getString("wayTypeName"), wayMaxSpeed, obstacleType));
			}
        	result.close();
        	
        	query = "SELECT edgeId, edgeHeading, edgeLength, startId, wayTypeName, wayMaxSpeed, wayTypeDefSpeed " +
					"FROM edge NATURAL JOIN way NATURAL JOIN wayType WHERE endId=" + id +
					" AND wayOneWay=0 AND startId!=" + id;
			
			statement = dbConnection.prepareStatement(query);
			result = statement.executeQuery();
			while(result.next()){
				edgeId = result.getLong("edgeId");

				query1 = "SELECT obstacleType, obstacleNoPass " +
						"FROM edge NATURAL JOIN obstacle WHERE edgeId =" + edgeId;
				statement1 = dbConnection.prepareStatement(query1);
				result1 = statement1.executeQuery();
				if(result1.next()){
					obstacleType = result1.getString("obstacleType");
					obstacleNoPass = result1.getByte("obstacleNoPass");
				}
				
				if(obstacleNoPass == 1){
					continue;
				}
				
				if(result.getInt("wayMaxSpeed") == 0){
					wayMaxSpeed = result.getInt("wayTypeDefSpeed");
				}
				else{
					wayMaxSpeed = result.getInt("wayMaxSpeed");
				}
				
				out.add(new AStarEdge(edgeId, id, result.getLong("startId"), result.getDouble("edgeLength"), 
						result.getInt("edgeHeading"), result.getString("wayTypeName"), wayMaxSpeed, obstacleType));
			}
        	result.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		finally{
			try {
	            if (statement != null) {
	                statement.close();
	            }
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * @return f value of this node for A star algorithm
	 */
	public double getF(){
		return g + h;
	}

	public void addOutEdge(AStarEdge outEdge){
		out.add(outEdge);
	}
	
	public void removeOutEdge(AStarEdge outEdge){
		out.remove(outEdge);
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<AStarEdge> getOut() {
		return out;
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

	public AStarNode getCameFrom() {
		return cameFrom;
	}

	public void setCameFrom(AStarNode cameFrom) {
		this.cameFrom = cameFrom;
	}

	public double getG() {
		return g;
	}

	public void setG(double g) {
		this.g = g;
	}

	public double getH() {
		return h;
	}

	public void setH(double h) {
		this.h = h;
	}
	
}
