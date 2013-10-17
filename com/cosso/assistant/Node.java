package com.cosso.assistant;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Rok Pajk Kosec
 * Class used for working with map data
 */
@XmlRootElement(name = "node")
public class Node {
	private double latitude;
	private double longitude;
	private long id;
	
	public Node(){
		latitude = 0;
		longitude = 0;
		id = -1;
	}
	
	public Node(double _lat, double _long, long _id){
		latitude = _lat;
		longitude = _long;
		id = _id;
	}
	
	public void setLatitude(double _lat){
		latitude = _lat;
	}
	
	public void setLongitude(double _long){
		longitude = _long;
	}
	
	public void setId(long _id){
		id = _id;
	}
	
	public double getLatitude(){
		return latitude;
	}
	
	public double getLongitude(){
		return longitude;
	}
	
	public long getId(){
		return id;
	}
}
