package com.victor;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SqlLite {

	private static final Logger _log = Logger.getGlobal();
	private final String connectionString;

	public SqlLite(String dbName) {
		connectionString = "jdbc:sqlite:" + System.getProperty("user.home") + File.separator + dbName;
	}

	void guardaDatos(HashMap<String, BigDecimal> datos) {
		_log.entering(this.getClass().getName(), "guardaDatos");
		try {

			// load the sqlite-JDBC driver using the current class loader
			Class.forName("org.sqlite.JDBC");

			Connection connection = null;
			try {
				// create a database connection
				connection = DriverManager.getConnection(connectionString);
				connection.createStatement()
						.executeUpdate("CREATE TABLE IF NOT EXISTS datos (" + "id INTEGER PRIMARY KEY,"
								+ "fecha DATETIME DEFAULT CURRENT_TIMESTAMP," + "SNR_DL NUMERIC," + "SNR_UL NUMERIC,"
								+ "Attenuation_DL NUMERIC," + "Attenuation_UL NUMERIC," + "Power_DL NUMERIC,"
								+ "Power_UL NUMERIC," + "DataRate_DL NUMERIC," + "DataRate_UL NUMERIC,"
								+ "Attainable_DL NUMERIC," + "Attainable_UL NUMERIC)");
				PreparedStatement stmt = connection.prepareStatement(
						"INSERT INTO datos (SNR_DL,SNR_UL,Attenuation_DL,Attenuation_UL,Power_DL,Power_UL,DataRate_DL,DataRate_UL,Attainable_DL,Attainable_UL) "
						+ "VALUES (?,?,?,?,?,?,?,?,?,?)");
				stmt.setBigDecimal(1, datos.get(Parameters.SNR_DL));
				stmt.setBigDecimal(2, datos.get(Parameters.SNR_UL));
				stmt.setBigDecimal(3, datos.get(Parameters.Attenuation_DL));
				stmt.setBigDecimal(4, datos.get(Parameters.Attenuation_UL));
				stmt.setBigDecimal(5, datos.get(Parameters.Power_DL));
				stmt.setBigDecimal(6, datos.get(Parameters.Power_UL));
				stmt.setBigDecimal(7, datos.get(Parameters.DataRate_DL));
				stmt.setBigDecimal(8, datos.get(Parameters.DataRate_UL));
				stmt.setBigDecimal(9, datos.get(Parameters.Attainable_DL));
				stmt.setBigDecimal(10, datos.get(Parameters.Attainable_UL));
				stmt.executeUpdate();				
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
		_log.exiting(this.getClass().getName(), "guardaDatos");

	}

}
