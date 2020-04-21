package com.victor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by victormanuel on 03/12/2015.
 */
public class mySQL {

	private static final Logger _log = Logger.getGlobal();
	private String connectionString;

	public mySQL() {

		// Read connection data from preferences
		connectionString = String.format("jdbc:mysql://%1$s:%2$s/adsl?user=%3$s&password=%4$s", 
				PreferencesManager.getPreference("MySql host", "mysql_server", "localhost"),
		        PreferencesManager.getPreference("MySql port", "mysql_port", "3306"), 
		        PreferencesManager.getPreference("MySql user", "mysql_user", null),
		        PreferencesManager.getPreference("MySql pass", "mysql_password", null)).toString();

		// Check for driver
		/*
		 * try { Class.forName("com.mysql.jdbc.Driver"); } catch (ClassNotFoundException
		 * e) { _log.log(Level.SEVERE, "Driver no encontrado", e); }
		 */

	}

	public void guardaDatos(HashMap<String, BigDecimal> datos) {
		_log.entering(this.getClass().getName(), "guardaDatosMySQL");

		int ip_id = 0;
		String ip = "localhost";

//		try {

		// Class.forName("com.mysql.jdbc.Driver");

		Connection connection = null;
		PreparedStatement stmt;
		ResultSet rs;

		try {

			// create a database connection
			connection = DriverManager.getConnection(connectionString);

			// buscamos la ip externa del equipo
			try {
				URL whatismyip = new URL("http://checkip.amazonaws.com");
				BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
				ip = in.readLine(); // you get the IP as a String
			} catch (IOException e) {
				_log.log(Level.SEVERE, null, e);
			}

			String hostname = "localhost";
			try {
				Process proc = Runtime.getRuntime().exec("hostname");
				BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8));
				hostname = in.readLine();
			} catch (IOException e) {
				_log.log(Level.SEVERE, null, e);
			}

			stmt = connection.prepareStatement("SELECT id FROM ip WHERE ip = ? AND name = ?");
			stmt.setString(1, ip);
			stmt.setString(2, hostname);
			rs = stmt.executeQuery();

			if (rs.next()) {
				ip_id = rs.getInt(1);
			} else {
				rs.close();
				stmt.close();
				stmt = connection.prepareStatement("INSERT INTO ip (ip,name) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS);
				stmt.setString(1, ip);
				// stmt.setString(2, InetAddress.getLocalHost().getHostName());
				stmt.setString(2, hostname);
				stmt.executeUpdate();
				rs = stmt.getGeneratedKeys();
				if (rs.next()) {
					ip_id = rs.getInt(1);
				}
			}

			if (ip_id > 0) {
				rs.close();
				stmt.close();
				stmt = connection.prepareStatement("INSERT INTO datos (ip_id,SNR_DL,SNR_UL,Attenuation_DL,Attenuation_UL,Power_DL,Power_UL,DataRate_DL,DataRate_UL,Attainable_DL,Attainable_UL) "
				        + "VALUES (?,?,?,?,?,?,?,?,?,?,?)");
				stmt.setInt(1, ip_id);
				stmt.setBigDecimal(2, datos.get(Parameters.SNR_DL));
				stmt.setBigDecimal(3, datos.get(Parameters.SNR_UL));
				stmt.setBigDecimal(4, datos.get(Parameters.Attenuation_DL));
				stmt.setBigDecimal(5, datos.get(Parameters.Attenuation_UL));
				stmt.setBigDecimal(6, datos.get(Parameters.Power_DL));
				stmt.setBigDecimal(7, datos.get(Parameters.Power_UL));
				stmt.setBigDecimal(8, datos.get(Parameters.DataRate_DL));
				stmt.setBigDecimal(9, datos.get(Parameters.DataRate_UL));
				stmt.setBigDecimal(10, datos.get(Parameters.Attainable_DL));
				stmt.setBigDecimal(11, datos.get(Parameters.Attainable_UL));
				stmt.executeUpdate();
			}

		} catch (Exception e) {
			// if the error message is "out of memory",
			// it probably means no database file is found
			_log.log(Level.SEVERE, null, e);
			PreferencesManager.cleanAll();
//			} catch (UnknownHostException e) {
//				_log.log(Level.SEVERE,null, e);
//				preferences.get("servidor", null);
		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				// connection close failed.
				_log.log(Level.SEVERE, null, e);
			}
		}

//		} catch (ClassNotFoundException ex) {
//			_log.log(Level.SEVERE, "Driver no encontrado", ex);
//		}
		_log.exiting(this.getClass().getName(), "guardaDatosMySQL");

	}

	public List<HashMap<String, Object>> getIPList() {
		_log.entering(this.getClass().getName(), "getDatosMySQL");
		Connection connection = null;
		List<HashMap<String, Object>> listaDatos = new ArrayList<>();
		try {

			// create a database connection
			connection = DriverManager.getConnection(connectionString);

			PreparedStatement stmt = connection
			        .prepareStatement("SELECT ip.ip, name, max(time) time FROM adsl.ip INNER JOIN adsl.datos ON ip.id=datos.ip_id GROUP BY ip.ip,ip.name ORDER BY time DESC;");

			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				HashMap<String, Object> datos = new HashMap<>();
				datos.put(Parameters.IP, rs.getString(Parameters.IP));
				datos.put(Parameters.NAME, rs.getString(Parameters.NAME));
				datos.put(Parameters.TIME, rs.getTimestamp(Parameters.TIME).getTime());
				listaDatos.add(datos);
			}
			rs.close();
			stmt.close();

		} catch (Exception e) {
			// if the error message is "out of memory",
			// it probably means no database file is found
			_log.log(Level.SEVERE, null, e);
			// PreferencesManager.cleanAll();
//			} catch (UnknownHostException e) {
//				_log.log(Level.SEVERE,null, e);
//				preferences.get("servidor", null);
		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				// connection close failed.
				_log.log(Level.SEVERE, null, e);
			}
		}

//		} catch (ClassNotFoundException ex) {
//			_log.log(Level.SEVERE, "Driver no encontrado", ex);
//		}
		_log.exiting(this.getClass().getName(), "guardaDatosMySQL");
		return listaDatos;
	}

	public List<HashMap<String, Object>> getDatosList(String ip, Date fechaInicio, Date fechaFin) {
		_log.entering(this.getClass().getName(), "getDatosMySQL");
		Connection connection = null;
		List<HashMap<String, Object>> listaDatos = new ArrayList<>();
		try {

			// create a database connection
			connection = DriverManager.getConnection(connectionString);

			StringBuilder sqlWhere = new StringBuilder("WHERE 1=1");
			if (ip != null && !ip.isEmpty()) {
				sqlWhere.append(" AND ip.ip = ?");
			}
			if (fechaInicio != null) {
				sqlWhere.append(" AND cast(datos.time as date) >= ?");
			}
			if (fechaFin != null) {
				sqlWhere.append(" AND cast(datos.time as date) <= ?");
			}

			PreparedStatement stmt = connection.prepareStatement("SELECT * FROM ip INNER JOIN datos ON ip.id=datos.ip_id " + sqlWhere.toString() + " ORDER BY time DESC");

			int index = 1;
			if (ip != null && !ip.isEmpty()) {
				stmt.setString(index++, ip);
			}
			if (fechaInicio != null) {
				stmt.setDate(index++, new java.sql.Date(fechaInicio.getTime()));
			}
			if (fechaFin != null) {
				stmt.setDate(index++, new java.sql.Date(fechaFin.getTime()));
			}

			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				HashMap<String, Object> datos = new HashMap<>();
				datos.put(Parameters.SNR_DL, rs.getBigDecimal(Parameters.SNR_DL));
				datos.put(Parameters.SNR_UL, rs.getBigDecimal(Parameters.SNR_UL));
				datos.put(Parameters.Attenuation_DL, rs.getBigDecimal(Parameters.Attenuation_DL));
				datos.put(Parameters.Attenuation_UL, rs.getBigDecimal(Parameters.Attenuation_UL));
				datos.put(Parameters.Power_DL, rs.getBigDecimal(Parameters.Power_DL));
				datos.put(Parameters.Power_UL, rs.getBigDecimal(Parameters.Power_UL));
				datos.put(Parameters.DataRate_DL, rs.getBigDecimal(Parameters.DataRate_DL));
				datos.put(Parameters.DataRate_UL, rs.getBigDecimal(Parameters.DataRate_UL));
				datos.put(Parameters.Attainable_DL, rs.getBigDecimal(Parameters.Attainable_DL));
				datos.put(Parameters.Attainable_UL, rs.getBigDecimal(Parameters.Attainable_UL));
				datos.put(Parameters.IP, rs.getString(Parameters.IP));
				datos.put(Parameters.NAME, rs.getString(Parameters.NAME));
				datos.put(Parameters.TIME, rs.getTimestamp(Parameters.TIME).getTime());
				listaDatos.add(datos);
			}
			rs.close();
			stmt.close();

		} catch (Exception e) {
			// if the error message is "out of memory",
			// it probably means no database file is found
			_log.log(Level.SEVERE, null, e);
			// PreferencesManager.cleanAll();
//			} catch (UnknownHostException e) {
//				_log.log(Level.SEVERE,null, e);
//				preferences.get("servidor", null);
		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				// connection close failed.
				_log.log(Level.SEVERE, null, e);
			}
		}

//		} catch (ClassNotFoundException ex) {
//			_log.log(Level.SEVERE, "Driver no encontrado", ex);
//		}
		_log.exiting(this.getClass().getName(), "guardaDatosMySQL");
		return listaDatos;
	}
}
