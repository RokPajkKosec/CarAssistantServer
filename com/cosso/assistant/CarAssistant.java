package com.cosso.assistant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.cosso.AStar.PathSearch;
import com.cosso.CBA.CBAControl;

/**
 * @author Rok Pajk Kosec
 *	Main class that runs service on server. Server used is Tomcat 7
 */
@Path("/assistant")
public class CarAssistant implements ServletContextListener{

	//private EntryHandler handler; //used for MySQL if needed
	private CBAControl cba;
	private PathSearch pathSearch;
	private int insertResponse;
	private int rulesResponse;
	private int loginResponse;
	private EntryHandlerMDB handlerMongo;
	
	/**
	 * @param name
	 * @param password
	 * @return registration status
	 * Responses to register request. Checks if username exists and creates new user in
	 * database via Login class. If user exists or there is an error returns 0, if OK returns 1
	 */
	@GET
	@Path("/register")
	@Produces(MediaType.TEXT_PLAIN)
	public String register(@QueryParam("name") String name, @QueryParam("pass") String password) {
		Login login = new Login();
		
		if(login.register(name, password)){
			loginResponse = 1;
		}
		else{
			loginResponse = 0;
		}
		
		return String.valueOf(loginResponse);
	}
	
	/**
	 * @param name
	 * @param password
	 * @return login status
	 * Responses to login request. Checks if username and password exist in database. If login
	 * is successful returns 1, else returns 0
	 */
	@GET
	@Path("/login")
	@Produces(MediaType.TEXT_PLAIN)
	public String login(@QueryParam("name") String name, @QueryParam("pass") String password) {
		Login login = new Login();
		
		if(login.userExists(name, password)){
			loginResponse = 1;
		}
		else{
			loginResponse = 0;
		}
		
		return String.valueOf(loginResponse);
	}
	
	/**
	 * @param address
	 * @return list of results that are gathered by Google GeoCode service
	 * Responses to geocode request. Used to redirect geocode to Google because of GeoCode request
	 * number constraint and mobile network proxy settings (could be bypassed if mobile network
	 * does not use proxy servers)
	 */
	@GET
	@Path("/geoCode")
	@Produces(MediaType.APPLICATION_JSON)
	public String geoCode(@QueryParam("address") String address) {
		String returnJSON = "";
		
		String url = "http://maps.google.com/maps/api/geocode/json?address=" + address + "&sensor=false";
		 
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			
			int responseCode = con.getResponseCode();
			
			System.out.println("\nSending 'GET' request to URL : " + url);
			System.out.println("Response Code : " + responseCode);
	 
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
	 
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			
			returnJSON = response.toString();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return returnJSON;
	}
	
	/**
	 * @param startLat
	 * @param startLong
	 * @param targetLat
	 * @param targetLong
	 * @param name
	 * @param targetClass
	 * @return list of geopoints in Node object form
	 * Responses to get path request. Route is calculated via PathSearch class
	 */
	@GET
	@Path("/getPath")
	@Produces(MediaType.APPLICATION_JSON)
	public Node[] getPath(@QueryParam("startLat") String startLat, @QueryParam("startLong") String startLong,
			@QueryParam("targetLat") String targetLat, @QueryParam("targetLong") String targetLong,
			@QueryParam("name") String name, @QueryParam("targetClass") String targetClass) {
		
		List<Node> path = new ArrayList<Node>();
		Node[] pathArray;
		
		System.out.println(startLat + " " + startLong + " " + targetLat + " " + targetLong + " " + targetClass + " " + name);

		//  za izracun poti... / done
		pathSearch = new PathSearch();
		path = pathSearch.search(Double.valueOf(startLat), Double.valueOf(startLong), Double.valueOf(targetLat), Double.valueOf(targetLong),
				11, targetClass, name);
		
		System.out.println("return");
		
		pathArray = path.toArray(new Node[path.size()]);
		return pathArray;
	}
	
	/**
	 * @param name
	 * @param targetClass
	 * @return response code of rules generation
	 * Responses to get rules request. Rules are calculated via CBAControl class
	 */
	@GET
	@Path("/getRules")
	@Produces(MediaType.TEXT_PLAIN)
	public String getRules(@QueryParam("name") String name, @QueryParam("targetClass") String targetClass) {
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		cba = new CBAControl();
		rulesResponse = cba.prepareData(name, targetClass, 0.1, 0.3);
		
		System.out.println("control " + name + " " + targetClass);
		
		return String.valueOf(rulesResponse);
	}
	
	/**
	 * @param entry1
	 * @return response code of success of adding entry to database
	 * Adds weather and road data, agregates entry values and adds data into database. Main database is
	 * MongoDB, and backup database is MySQL. Operations are made by EntryHandlerMDB for MongoDB and by
	 * EntryHandler for MySQL
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addJSONEntry(Entry entry1) {
				
		//handler = new EntryHandler();
		handlerMongo = new EntryHandlerMDB();
		
		try {
			//handler.openConnection();
			handlerMongo.openConnection();
			//handler.addEntry(entry1);
			insertResponse = handlerMongo.addEntry(entry1);
			//handler.close();
			handlerMongo.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return Response.status(insertResponse).build();
	}
	
	/**
	 * Initializes path to SQLite database that is used for storing map data
	 */
	public void contextInitialized(ServletContextEvent arg0) {
		System.out.print("init: ");
		System.setProperty("sqlite.system.home", arg0.getServletContext().getRealPath("/"));
		System.out.println(arg0.getServletContext().getRealPath("/"));
	}

	/**
	 * Destroys context that is used for storing SQLite database path
	 */
	public void contextDestroyed(ServletContextEvent arg0) {
		System.out.println("destroy");
	}
}