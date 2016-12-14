package com.victor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;

public class SqlLite {

	private static final Logger _log = Logger.getLogger(SqlLite.class.getName());
	private final String connectionString;

	public SqlLite(String dbName) {
		connectionString = "jdbc:sqlite:" + System.getProperty("user.home") + File.separator + dbName;
	}

	void guardaDatos(int[] datos) {
		_log.entering(SqlLite.class.getName(), "guardaDatos");
		try {

			// load the sqlite-JDBC driver using the current class loader
			Class.forName("org.sqlite.JDBC");

			Connection connection = null;
			try {
				// create a database connection
				connection = DriverManager.getConnection(connectionString);
				connection.createStatement()
						.executeUpdate("CREATE TABLE IF NOT EXISTS datos (" + "id INTEGER PRIMARY KEY,"
								+ "fecha DATETIME DEFAULT CURRENT_TIMESTAMP," + "download INTEGER," + "upload INTEGER,"
								+ "attdownrate INTEGER," + "attuprate INTEGER," + "downpower INTEGER,"
								+ "uppower INTEGER)");
				PreparedStatement st = connection.prepareStatement(
						"INSERT INTO datos (download,upload,attdownrate,attuprate,downpower,uppower) VALUES (?,?,?,?,?,?)");
				for (int i = 0; i < datos.length; i++) {
					st.setInt(i + 1, datos[i]);
				}
				st.executeUpdate();
				_log.info(String.format("%30s ---> %5d | %5d | %5d | %5d | %5d | %5d %n", new Date().toString(),
						datos[0], datos[1], datos[2], datos[3], datos[4], datos[5]));
			} catch (SQLException e) {
				// if the error message is "out of memory",
				// it probably means no database file is found
				_log.log(Level.SEVERE, null, e);
			} finally {
				try {
					if (connection != null) {
						connection.close();
					}
				} catch (SQLException e) {
					_log.log(Level.SEVERE, "Connection close failed", e);
				}
			}

		} catch (ClassNotFoundException ex) {
			_log.log(Level.SEVERE, "Driver no encontrado", ex);
		}
		_log.exiting(SqlLite.class.getName(), "guardaDatos");

	}

}
