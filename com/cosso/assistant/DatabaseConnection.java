package com.cosso.assistant;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author Rok Pajk Kosec
 * Class used for storing MySQL database connection details
 */
public class DatabaseConnection {
		
	public static Connection connection() throws Exception {
        Connection connection = null;
		String driver = "com.mysql.jdbc.Driver";
        String url = "jdbc:mysql://localhost:3306/userdata";
        String user = "";
        String password = "";
		Class.forName(driver).newInstance();
		connection = DriverManager.getConnection(url, user, password);
		return connection;
    }
	
	public static void disconnect(Connection connection) throws SQLException{
        if (connection != null) {
            connection.close();
        }
	}
	
	public static void main(String[] args) {
		//for testing use
    }
}
