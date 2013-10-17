package com.cosso.assistant;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Rok Pajk Kosec
 * Class for parsing road data recieved in XML form
 */
public class RoadData extends DefaultHandler implements Runnable{
    private String temp;
    private double latitude;
    private double longitude;
    private int id;
    private String category;
    private int noPass;
    private boolean inBounds;
    private long obstacleEdge;
    private String [] tokens;
    private	String delims = "[,]";
    private RoadDataMatch map;
            
    //Database connection
    private static Connection dbConnection;
    
    //URL string of Slovenian road data web source
//    private String url = "kazipot1.promet.si/kazipot/services/dataexport/exportDogodki.ashx?format=XML&version=1.0.0&reportType=FULL&language=EN&sortOrder=VELJAVNOSTODDESC&icons=NONE&crsId=EPSG:4326";
    private String XmlFileName = "http://kazipot1.promet.si/kazipot/services/dataexport/exportDogodki.ashx?format=GEORSS&version=1.0.0&reportType=FULL&language=EN&sortOrder=VELJAVNOSTODDESC&icons=NONE";
    
    private void parseData(){
		dbConnection = SQLiteConnection.connection();
		parseDocument();
		SQLiteConnection.disconnect(dbConnection);
    }
    
    /**
     * Parses document
     */
    private void parseDocument() {
        // parse
        SAXParserFactory factory = SAXParserFactory.newInstance();
        cleanDB();
        try {
            SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource(XmlFileName), this);
        } catch (ParserConfigurationException e) {
            System.out.println("ParserConfig error");
        } catch (SAXException e) {
            System.out.println("SAXException : xml not well formed");
        } catch (IOException e) {
            System.out.println("IO error");
        }
    }
    
    /**
     * @param table
     * @param attributes
     * @param values
     * Insert road data into map data SQLite database
     */
	private void insertDB(String table, String attributes, String values){
		PreparedStatement statement = null;
		
		try {			
			statement = dbConnection.prepareStatement("INSERT INTO " + table + "(" + attributes +") VALUES (" + values + ")");			
			statement.executeUpdate();
			
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
	 * @param wayId
	 * @return returns list of obstacles
	 * Returns obstacles on a way provided with way id from map data
	 */
	public static String[] getObstacleByWay(long wayId){
		String obstacle = "No obstacle";
		String noPass = "0";
		String[] returnResult = new String[2];
		
		PreparedStatement statement = null;
		ResultSet result = null;
		String query = "SELECT obstacleType, obstacleNoPass FROM obstacle NATURAL JOIN edge WHERE wayId = " + String.valueOf(wayId);
		
    	dbConnection = SQLiteConnection.connection();

		try {
			statement = dbConnection.prepareStatement(query);
			result = statement.executeQuery();
			if(result.next()){
				obstacle = result.getString("obstacleType");
				noPass = result.getString("obstacleNoPass");
			}

		} catch (Exception e) {
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
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
        SQLiteConnection.disconnect(dbConnection);
		
        returnResult[0] = obstacle;
        returnResult[1] = noPass;
        
		return returnResult;
	}
	
	/**
	 * @return ArrayList of all obstacles
	 */
	public static ArrayList<String> getObstacles(){
		PreparedStatement statement = null;
		ResultSet result = null;
		String query = "SELECT * FROM obstacle";
		ArrayList<String> entrys = new ArrayList<String>();
		String resultString = "";
		
    	dbConnection = SQLiteConnection.connection();

		try {
			statement = dbConnection.prepareStatement(query);
			result = statement.executeQuery();
			while(result.next()){
				resultString = result.getString("obstacleId");
				resultString = resultString + ", " + result.getString("edgeId");
				resultString = resultString+ ", " + result.getString("obstacleType");
				resultString = resultString+ ", " + result.getString("obstacleNoPass");
				entrys.add(resultString);
			}
		} catch (Exception e) {
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
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
        SQLiteConnection.disconnect(dbConnection);
		
		return entrys;
	}
	
	/**
	 * Removes all obstacle data. Used before every new check
	 */
	private void cleanDB(){
		PreparedStatement statement = null;
		try {
			statement = dbConnection.prepareStatement("DELETE FROM obstacle");
			statement.executeUpdate();
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
	 * sets temp substring
	 */
	@Override
    public void characters(char[] buffer, int start, int length) {
        temp = new String(buffer, start, length);
	}

	/**
	 * Finds start element in XML data
	 */
    @Override
    public void startElement(String s, String s1, String elementName, Attributes attributes) throws SAXException {
    	temp = "";
    	
    	if(elementName.equalsIgnoreCase("entry")){
    		category = "";
    		latitude = 0;
    		longitude = 0;
    		id = 0;
    	}
    	if(elementName.equalsIgnoreCase("category")){
    		category = attributes.getValue("term");
    		if(category.equals("Road closure")){
    			noPass = 1;
    		}
    		else{
    			noPass = 0;
    		}
    	}
    	
    }
    /**
     * Fins end element in XML data
     */
    @Override
    public void endElement(String s, String s1, String element) throws SAXException {
    	
    	if(element.equalsIgnoreCase("georss:point")){
    		tokens = temp.split(delims);
    		latitude = Double.valueOf(tokens[0]);
    		longitude = Double.valueOf(tokens[1]);
    		if(latitude < 46.2072206 && latitude > 45.9824379 && longitude < 14.6263829 && longitude > 14.4035334){
    			inBounds = true;
    		}
    		else{
    			inBounds = false;
    		}
    	}
    	if(element.equalsIgnoreCase("id")){
    		id = Integer.valueOf(temp);
    	}
    	if(element.equalsIgnoreCase("entry") && inBounds){
        	map = new RoadDataMatch();
        	try {
				obstacleEdge = map.getClosestEdge(latitude, longitude);
			} catch (Exception e) {
				e.printStackTrace();
			}
        	
    		this.insertDB("obstacle", "obstacleId, edgeId, obstacleType, obstacleNoPass",
        			String.valueOf(id) + "," + String.valueOf(obstacleEdge)+ ",\"" + category + "\"," + String.valueOf(noPass) );
        	        	
    	}
    }

	@Override
	public void run() {
		parseData();
	}

}
