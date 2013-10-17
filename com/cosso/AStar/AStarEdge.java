package com.cosso.AStar;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Rok Pajk Kosec
 * Class that represents edge on map data with added atributes for calculation of
 * best route using A star algorithm
 */
public class AStarEdge {
	//Edge id
	private long id;
	//Start node id
	private long start;
	//End node id
	private long end;
	//Lenght
	private double length;
	//Heading of this edge
	private int heading;
	//Way type of this edge
	private String type;
	//Speed limit on this edge
	private int speedLimit;
	//Obstacle on this edge if any
	private String obstacle;
	
	/**
	 * Empty constructor
	 */
	public AStarEdge(){
		this.id = -1;
		this.start = -1;
		this.end = -1;
		this.length = 0;
		this.heading = 0;
		this.type = "";
		this.speedLimit = 0;
		this.obstacle = "";
	}

	/**
	 * @param id
	 * @param start
	 * @param end
	 * @param length
	 * @param heading
	 * @param type
	 * @param speedLimit
	 * @param obstacle
	 * Construcstor that sets all the parameters
	 */
	public AStarEdge(long id, long start, long end, double length, int heading,
			String type, int speedLimit, String obstacle) {
		super();
		this.id = id;
		this.start = start;
		this.end = end;
		this.length = length;
		this.heading = heading;
		this.type = type;
		this.speedLimit = speedLimit;
		this.obstacle = obstacle;
	}
	
	/**
	 * @param dbConnection
	 * @return AStarNode object that represents end node of this edge
	 */
	public AStarNode getEndNode(Connection dbConnection){
		AStarNode n = null;
		
		PreparedStatement statement = null;
		ResultSet result = null;
		String query = "";
		
		try {
			query = "SELECT nodeId, nodeLat, nodeLong " +
					"FROM node WHERE nodeId=" + end;
			
			statement = dbConnection.prepareStatement(query);
			result = statement.executeQuery();
			while(result.next()){
				n = new AStarNode(result.getLong("nodeId"), result.getDouble("nodeLat"), result.getDouble("nodeLong"));
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
		
		return n;
	}
	
	/**
	 * @param dbConnection
	 * @return AStarNode object that represents start node of this edge
	 */
	public AStarNode getStartNode(Connection dbConnection){
		AStarNode n = null;
		
		PreparedStatement statement = null;
		ResultSet result = null;
		String query = "";
		
		try {
			query = "SELECT nodeId, nodeLat, nodeLong " +
					"FROM node WHERE nodeId=" + start;
			
			statement = dbConnection.prepareStatement(query);
			result = statement.executeQuery();
			while(result.next()){
				n = new AStarNode(result.getLong("nodeId"), result.getDouble("nodeLat"), result.getDouble("nodeLong"));
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
		
		return n;
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

	public double getLength() {
		return length;
	}

	public void setLength(double length) {
		this.length = length;
	}

	public int getHeading() {
		return heading;
	}

	public void setHeading(int heading) {
		this.heading = heading;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getSpeedLimit() {
		return speedLimit;
	}

	public void setSpeedLimit(int speedLimit) {
		this.speedLimit = speedLimit;
	}

	public String getObstacle() {
		return obstacle;
	}

	public void setObstacle(String obstacle) {
		this.obstacle = obstacle;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

}