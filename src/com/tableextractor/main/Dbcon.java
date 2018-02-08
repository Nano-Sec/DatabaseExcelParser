package com.tableextractor.main;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.apache.commons.dbutils.DbUtils;

/**
 * Represents a connection to the specified database
 * @author Abhinav Upadhyay
 *
 */
public class Dbcon {
	static String url = "jdbc:mysql://localhost:3306/";
    final static String driver = "com.mysql.jdbc.Driver";
    final static String usr = "root";
    final static String pwd = "root";
    /**
     * Establishes a connection to the database on port 3306. Cannot be instantiated.
     * @return SQL Connection object
     * @throws SQLException
     */
    public static Connection getCon(String dbname) throws SQLException{
    	url += dbname;
    	DbUtils.loadDriver(driver);
    	Connection conn = DriverManager.getConnection(url, usr, pwd);
    	return conn;
    }

}
