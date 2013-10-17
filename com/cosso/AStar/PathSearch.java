package com.cosso.AStar;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Stack;

import com.cosso.CBA.CBAControl;
import com.cosso.assistant.GeoCalculator;
import com.cosso.assistant.Node;
import com.cosso.assistant.SQLiteConnection;
import com.cosso.assistant.WeatherWU;

/**
 * @author Rok Pajk Kosec
 * Class that finds best path according to speed difference between driving speed and speed limit or accordint
 * to consumption of a user. Implements A star algorithm with variable cost value due to classification of
 * current circumstances (weather, road obstacles, timestamp etc.)
 */
public class PathSearch {
	private List<WeatherWU> weatherPoints;

	public static void main(String[] args) {
		//46.1481950, 14.4825784
		//46.1444573, 14.4797038
		//46.1353740, 14.4764748
		//46.1136429, 14.4654737
		//for(int i=1; i<30; i++){
			long start = System.nanoTime()/1000000;
			PathSearch p = new PathSearch();
			p.search(46.1481950, 14.4825784, 46.0546798, 14.5059942, 11, "speedDifference", "rok"); //46.0546798, 14.5059942 ; 46.1136429, 14.4654737
			long stop = System.nanoTime()/1000000;
			System.out.println("time(ms): " + (stop - start));
		//}
		//p.prepareWeatherData(46.1481950, 14.4825784, 46.0546798, 14.5059942);
	}
	
	/**
	 * Empty constructor
	 */
	public PathSearch(){
		weatherPoints = new ArrayList<WeatherWU>();
	}
	
	/**
	 * Gets weather data on poionts along the direct line from start to finish. Points are positions on every
	 * 5 kilometers and are later used in classification. This reduces calls to weather service and speeds up
	 * path search
	 */
	private void prepareWeatherData(double startLat, double startLong, double targetLat, double targetLong){
		double distance = GeoCalculator.haversine(startLat, startLong, targetLat, targetLong);
		double currentLat = startLat;
		double currentLong = startLong;
		int weatherPointsNum = (int)Math.ceil(distance)/5000;
		System.out.println(distance + " " + weatherPointsNum);
		System.out.println(currentLat + ";" + currentLong);
		double vectorLat = (targetLat - startLat) / weatherPointsNum;
		double vectorLong = (targetLong - startLong) / weatherPointsNum;
		
		WeatherWU wTemp = new WeatherWU();
		wTemp.getWeatherData(currentLat, currentLong);
		wTemp.setLatitude(currentLat);
		wTemp.setLongitude(currentLong);
		weatherPoints.add(wTemp);
		
		for(int i=1; i<=weatherPointsNum; i++){
			currentLat+=vectorLat;
			currentLong+=vectorLong;
			wTemp = new WeatherWU();
			wTemp.getWeatherData(currentLat, currentLong);
			wTemp.setLatitude(currentLat);
			wTemp.setLongitude(currentLong);
			weatherPoints.add(wTemp);
			System.out.println(currentLat + ";" + currentLong);
		}
		
		for(int i=0; i<weatherPoints.size(); i++){
			System.out.println(weatherPoints.get(i).getWeather());
		}
	}
	
	/**
	 * @param latitude
	 * @param longitude
	 * @return index of weather point in weather points list that is closest to observed location
	 */
	private int getClosestWeatherPoint(double latitude, double longitude){
		int minDistanceIndex = 0;
		double distance = Double.MAX_VALUE;
		double tempDistance = 0;
		
		for(int i=0; i<weatherPoints.size(); i++){
			tempDistance = Math.sqrt(Math.pow((latitude - weatherPoints.get(i).getLatitude()), 2) + Math.pow((longitude - weatherPoints.get(i).getLongitude()), 2));
			if(tempDistance < distance){
				distance = tempDistance;
				minDistanceIndex = i;
			}
		}
		
		return minDistanceIndex;
	}
	
	/**
	 * @param startLat
	 * @param startLong
	 * @param targetLat
	 * @param targetLong
	 * @param div
	 * @param targetClass
	 * @param userName
	 * @return path in a form of geopoints in a list
	 * Calculates best route using user data and current data. It uses A star alorithm that allows loops due to possible route that must go to
	 * the same node twice. This poses danger of long calculation time in certain cases.
	 */
	public List<Node> search(double startLat, double startLong, double targetLat, double targetLong, int div, String targetClass, String userName){
		Connection dbConnection = SQLiteConnection.connection();
		
		List<Node> returnList = new ArrayList<Node>();
		
		prepareWeatherData(startLat, startLong, targetLat, targetLong);
		
		int tempTime = 0;
		int weatherIndex = 0;
		double targetValue = 0;
		CBAControl classificator = new CBAControl();
		
		AStarNode startNode = closestAStarNode(startLat, startLong, dbConnection);
		AStarNode target = closestAStarNode(targetLat, targetLong, dbConnection);
		startNode.setH(GeoCalculator.haversine(startNode.getLatitude(), startNode.getLongitude(), target.getLatitude(), target.getLongitude()));
		
		Map<Long, AStarNode> openSet = new HashMap<Long, AStarNode>();
        PriorityQueue<AStarNode> pQueue = new PriorityQueue<AStarNode>(200, new NodeComparator());
        Map<Long, AStarNode> closeSet = new HashMap<Long, AStarNode>();        
        openSet.put(startNode.getId(), startNode);
        pQueue.add(startNode);
		
        AStarNode goal = null;
        while(openSet.size() > 0){
        	//get best candidate from open set
            AStarNode x = pQueue.poll();
            openSet.remove(x.getId());
            
            if(x.getId() == target.getId()){
                goal = x;
                break;
            }else{
            	closeSet.put(x.getId(), x);
            	x.getNeighbourEdges(dbConnection);
            	
            	//get closest weatherPoint index of list
            	weatherIndex = getClosestWeatherPoint(x.getLatitude(), x.getLongitude());
            	
            	String delims = "[:]+";
				String[] tokens = DateFormat.getTimeInstance().format(new Date(System.currentTimeMillis())).split(delims);
				tempTime = Integer.valueOf(tokens[0]);
            	
                for (AStarEdge neighbor : x.getOut()) {
                    //Node visited = closeSet.get(neighbor.getId());
                		targetValue = classificator.classify(weatherPoints.get(weatherIndex).getWindSpeed(), weatherPoints.get(weatherIndex).getWindDegrees(),
                				neighbor.getHeading(), weatherPoints.get(weatherIndex).getTemperature(), 0.0, neighbor.getSpeedLimit(), 0.0,
                				tempTime,
                				(byte)0, neighbor.getObstacle(), neighbor.getType(), weatherPoints.get(weatherIndex).getWeather(), userName, targetClass);
               	
                		/*
                		 * calculate cost (g) from start to current node. At consumption there is 0.01 correction because of
                		 * a special case if thre is no consumption data but rules still exist. In this case all rules have target
                		 * value 0.0 which leads to infinite loop in some searches
                		 */
                		double g;
                		if(targetClass.equals("speedDifference")){
                			g = x.getG() + ((3.6 * neighbor.getLength()) / (neighbor.getSpeedLimit() + targetValue));
                		}
                		else{
                			if(targetValue != 0){
                    			g = x.getG() + (neighbor.getLength() * targetValue);
                			}
                			else{
                				g = x.getG() + 0.01;
                			}
                		}
                        
                		//check if node n is in open set
                        AStarNode n = openSet.get(neighbor.getEndNode(dbConnection).getId());
                        
                        //if it is not add it to the open set
                        if (n == null) {
                        	n = new AStarNode(neighbor.getEndNode(dbConnection).getId(), neighbor.getEndNode(dbConnection).getLatitude(), neighbor.getEndNode(dbConnection).getLongitude());
                        	n.setH(GeoCalculator.haversine(n.getLatitude(), n.getLongitude(), target.getLatitude(), target.getLongitude()) / div);
                            n.setCameFrom(x);
                            n.setG(g);
                            //System.out.println(n.getId() + " : " + g + " + " + n.getH());
                            //System.out.println(n.getLatitude()+";"+n.getLongitude());
                            openSet.put(n.getId(), n);
                            pQueue.add(n);
                        }
                        //if it is, and its g is greater than this g, set g and parent
                        else if (g < n.getG()) {
	                        //Have a better route to the current node, change its parent
	                        n.setCameFrom(x);
	                        n.setH(GeoCalculator.haversine(n.getLatitude(), n.getLongitude(), target.getLatitude(), target.getLongitude()) / div);
	                        n.setG(g);
	                        //System.out.println(n.getId() + " g corrected : " + g + " + " + n.getH());
	                        //n.setH(graph.calcManhattanDistance(neighbor, target));
                    	}
                }

            }
        }
        
        if(goal != null){
            Stack<AStarNode> stack = new Stack<AStarNode>();
            List<AStarNode> list = new ArrayList<AStarNode>();
            stack.push(goal);
            AStarNode parent = goal.getCameFrom();
            
            //g of a goal is absolute time or consumption
            long g;
            if(targetClass.equals("speedDifference")){
                g = (long)(goal.getG()*1000.0);
            }
            else{
            	g = (long)(goal.getG()/100.0);
            }
            
            //create path from getting parent of every node
            while(parent != null){
                stack.push(parent);
                parent = parent.getCameFrom();
            }
            while(stack.size() > 0){
                list.add(stack.pop());
            }
            for(AStarNode n : list){
            	System.out.println(n.getLatitude()+";"+n.getLongitude()+";"+g);
            	returnList.add(new Node(n.getLatitude(), n.getLongitude(), g));
            }
        }
        
		SQLiteConnection.disconnect(dbConnection);
		
        return returnList;
	}
	
	/**
	 * @param latitude
	 * @param longitude
	 * @param dbConnection
	 * @return closest node to provided location
	 * Used to find start and target node according to provided latitude and longitude
	 */
	private AStarNode closestAStarNode(double latitude, double longitude, Connection dbConnection){
		long id = -1;
		double minDistance = 100000000; //100000 km which is more than equator
		double distance = 0;
		double latitudeMap;
		double longitudeMap;
		AStarNode returnNode = new AStarNode();
		
		PreparedStatement statement = null;
		ResultSet result = null;
		String query = "SELECT nodeId, nodeLat, nodeLong FROM node WHERE nodeLat<=" + String.valueOf(latitude+0.005) + " AND nodeLat>=" +
						String.valueOf(latitude-0.005) + " AND nodeLong<=" + String.valueOf(longitude+(0.005*90/latitude)) +
						" AND nodeLong>=" + String.valueOf(longitude-(0.005*90/latitude));
		
		try {
			statement = dbConnection.prepareStatement(query);
			result = statement.executeQuery();
			while(result.next()){
				latitudeMap = result.getDouble("nodeLat");
				longitudeMap = result.getDouble("nodeLong");
				if((distance = GeoCalculator.haversine(latitude, longitude, latitudeMap, longitudeMap)) < minDistance){
					id = result.getLong("nodeId");
					minDistance = distance;
					returnNode.setId(id);
					returnNode.setLatitude(latitudeMap);
					returnNode.setLongitude(longitudeMap);
				}
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
		
		return returnNode;
	}
	
	/**
	 * @author Rok Pajk Kosec
	 * Implementation of comparator used in priority queue for finding best candidate in open set
	 */
	public class NodeComparator implements Comparator<AStarNode> {

		@Override
	    public int compare(AStarNode first, AStarNode second) {
	        if(first.getF() < second.getF()){
	            return -1;
	        }else if(first.getF() > second.getF()){
	            return 1;
	        }else{
	            return 0;
	        }
	    }
	}
}
