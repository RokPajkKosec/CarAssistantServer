package com.cosso.assistant;
import java.net.UnknownHostException;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;

/**
 * @author Rok Pajk Kosec
 * Class used for checking if login data is correct and for adding new user into database
 */
public class Login {
	private MongoClient mongoClient;
	private Morphia morphia;
	private Datastore ds;
	
	public static void main(String[] args) {
		System.out.println(new Login().userExists("rok", "rok"));
		System.out.println(new Login().register("kosec", "pajk"));
	}
	
	public Login(){
		
	}
	
	/**
	 * @param userName
	 * @param password
	 * @return boolean value of registration success
	 * Checks if username exists in database. If it does returnes false, else adds new user
	 * into database and returnes true
	 */
	public boolean register(String userName, String password){
		if(userNameExists(userName)){
			return false;
		}
		
		try {
			openConnection();
			
			DB db = mongoClient.getDB("userData");
			DBCollection coll1 = db.getCollection("users");
			
			BasicDBObject obj = new BasicDBObject("userName", userName);
			obj.append("userPass", password);
			
			coll1.insert(obj);
			
			close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/**
	 * @param userName
	 * @return boolean value
	 * Checks if username exists. If it does returns true, else returns false
	 */
	public boolean userNameExists(String userName){
		boolean found = false;
		
		try {
			openConnection();
			
			DB db = mongoClient.getDB("userData");
			DBCollection coll1 = db.getCollection("users");
			DBCursor cursor1 = coll1.find(new BasicDBObject("userName", userName)).sort(new BasicDBObject("_id", -1)).limit(1);
			
			if(cursor1.hasNext()){
				found = true;
			}
			
			close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		return found;
	}
	
	/**
	 * @param userName
	 * @param password
	 * @return boolean
	 * Checks if provided username and password combination exists in database. If it does returns true, else returns false
	 */
	public boolean userExists(String userName, String password){
		boolean found = false;
		
		try {
			openConnection();
			
			DB db = mongoClient.getDB("userData");
			DBCollection coll1 = db.getCollection("users");
			DBCursor cursor1 = coll1.find(new BasicDBObject("userName", userName), new BasicDBObject("userPass", password)).sort(new BasicDBObject("_id", -1)).limit(1);
			
			if(cursor1.hasNext()){
				found = true;
			}
			
			close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		return found;
	}
	
	/**
	 * @throws UnknownHostException
	 * Opens database connection
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