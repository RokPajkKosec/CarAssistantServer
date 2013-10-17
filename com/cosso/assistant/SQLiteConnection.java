package com.cosso.assistant;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author Rok Pajk Kosec
 * Class used for storing SQLite database connection details
 */
public class SQLiteConnection {
	public static Connection connection(){
        Connection connection = null;
		String driver = "org.sqlite.JDBC";
        String sqliteHome = System.getProperty("sqlite.system.home");
        String url = "jdbc:sqlite:"+sqliteHome+"mapS.db";
		try {
			Class.forName(driver).newInstance();
			connection = DriverManager.getConnection(url);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}						

		return connection;
    }
	
	public static void disconnect(Connection connection){
        try {
			if (connection != null) {
			    connection.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
