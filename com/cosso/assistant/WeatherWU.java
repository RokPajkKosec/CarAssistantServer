package com.cosso.assistant;

import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Rok Pajk Kosec
 * Class for parsing weather data from weatherunderground web service
 */
public class WeatherWU {
	//current city
	private String city;
	//weather type
	private String weather;
	//temperature at location
	private double temperature;
	//wind speed at locatiobn
	private double windSpeed;
	//wind direction in N S W E notation
	private String windDirection;
	//wind direction in bearing degrees notation
	private int windDegrees;
	//latitude
	private double latitude;
	//longitude
	private double longitude;
	
	/**
	 * Constructor
	 */
	public WeatherWU(){
		setCity("");
		setWeather("");
		setTemperature(0);
		setWindSpeed(0);
		setWindDirection("");
		setWindDegrees(-1);
	}

	/**
	 * @param lat
	 * @param lon
	 * Gathers and parses weather data on location with provided latitude and longitude
	 */
	public void getWeatherData(double lat, double lon){	
		try{
			String url = "http://api.wunderground.com/api/c0aaa510ac0e8558/conditions/q/"+lat+","+lon+".xml";

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new URL(url).openStream());
			doc.getDocumentElement().normalize();

			NodeList nodes = doc.getElementsByTagName("display_location");
	
			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element element = (Element) node;
					setCity(getValue("city", element));
				}
			}
			
			nodes = doc.getElementsByTagName("current_observation");

			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element element = (Element) node;
					setWeather(getValue("weather", element));
					setTemperature(Double.valueOf(getValue("temp_c", element)));
					setWindSpeed(Double.valueOf(getValue("wind_kph", element)));
					setWindDirection(getValue("wind_dir", element));
					setWindDegrees(Integer.valueOf(getValue("wind_degrees", element)));
				}
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static String getValue(String tag, Element element) {
		NodeList nodes = element.getElementsByTagName(tag).item(0).getChildNodes();
		if(element.getElementsByTagName(tag).item(0).getChildNodes().getLength() == 0){
			return "null";
		}
		Node node = (Node) nodes.item(0);
		return node.getNodeValue();
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getWeather() {
		return weather;
	}

	public void setWeather(String weather) {
		this.weather = weather;
	}

	public double getTemperature() {
		return temperature;
	}

	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}

	public double getWindSpeed() {
		return windSpeed;
	}

	public void setWindSpeed(double windSpeed) {
		this.windSpeed = windSpeed;
	}

	public String getWindDirection() {
		return windDirection;
	}

	public void setWindDirection(String windDirection) {
		this.windDirection = windDirection;
	}

	public int getWindDegrees() {
		return windDegrees;
	}

	public void setWindDegrees(int windDegrees) {
		this.windDegrees = windDegrees;
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

}