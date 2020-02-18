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
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

/**
 * Created by victormanuel on 03/12/2015.
 */
public class mySQL {

	private static final Logger _log = Logger.getLogger(mySQL.class.getName());
	private String connectionString;
	private Preferences preferences = Preferences.userNodeForPackage(mySQL.class);
	
	public mySQL() {

		// Read connection data from preferences
		
		if (preferences.get("mysql_server", null) == null || preferences.get("mysql_password", null) == null) {
			preferences.put("mysql_server",
					(String)JOptionPane.showInputDialog(null, "Servidor MySQL","Configuracion", JOptionPane.QUESTION_MESSAGE,null,null,"localhost"));
			preferences.put("mysql_port",
					(String)JOptionPane.showInputDialog(null, "Puerto", "Configuracion", JOptionPane.QUESTION_MESSAGE,null,null,"3306"));
			preferences.put("mysql_user",
					JOptionPane.showInputDialog(null, "Usuario","Configuracion", JOptionPane.QUESTION_MESSAGE));
			preferences.put("mysql_password",
					JOptionPane.showInputDialog(null, "Contrasenha", "Configuracion", JOptionPane.QUESTION_MESSAGE));
		}

		connectionString = String.format("jdbc:mysql://%1$s:%2$s/adsl?user=%3$s&password=%4$s",
				preferences.get("mysql_server", "localhost"), 
				preferences.get("mysql_port", "3306"),
				preferences.get("mysql_user", null), 
				preferences.get("mysql_password", null)).toString();

		//Check for driver
		/*
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			_log.log(Level.SEVERE, "Driver no encontrado", e);
		}
		*/

	}

	void guardaDatosMySQL(HashMap<String, BigDecimal> datos) {

		int ip_id = 0;
		String ip = "localhost";

//		try {

			//Class.forName("com.mysql.jdbc.Driver");

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
					_log.log(Level.SEVERE,null, e);
				}

				String hostname = "localhost";
				try {
					Process proc = Runtime.getRuntime().exec("hostname");
                        		BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream(),StandardCharsets.UTF_8));
					hostname = in.readLine();
				} catch(IOException e) {
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
					stmt = connection.prepareStatement("INSERT INTO ip (ip,name) VALUES (?,?)",
							Statement.RETURN_GENERATED_KEYS);
					stmt.setString(1, ip);
					//stmt.setString(2, InetAddress.getLocalHost().getHostName());
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
					stmt = connection.prepareStatement(
							"INSERT INTO datos (ip_id,SNR_DL,SNR_UL,Attenuation_DL,Attenuation_UL,Power_DL,Power_UL,DataRate_DL,DataRate_UL) "
							+ "VALUES (?,?,?,?,?,?,?,?,?)");
					stmt.setInt(1, ip_id);
					stmt.setBigDecimal(2, datos.get(Parameters.SNR_DL));
					stmt.setBigDecimal(3, datos.get(Parameters.SNR_UL));
					stmt.setBigDecimal(4, datos.get(Parameters.Attenuation_DL));
					stmt.setBigDecimal(5, datos.get(Parameters.Attenuation_UL));
					stmt.setBigDecimal(6, datos.get(Parameters.Power_DL));
					stmt.setBigDecimal(7, datos.get(Parameters.Power_UL));
					stmt.setBigDecimal(8, datos.get(Parameters.DataRate_DL));
					stmt.setBigDecimal(9, datos.get(Parameters.DataRate_UL));
					stmt.executeUpdate();
				}

			} catch (Exception e) {
				// if the error message is "out of memory",
				// it probably means no database file is found
				_log.log(Level.SEVERE,null, e);
				 preferences.remove("mysql_server");
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
					_log.log(Level.SEVERE,null, e);
				}
			}

//		} catch (ClassNotFoundException ex) {
//			_log.log(Level.SEVERE, "Driver no encontrado", ex);
//		}

	}

}
