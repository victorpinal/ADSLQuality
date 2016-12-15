package com.victor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.nio.charset.StandardCharsets;


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
		
		if (preferences.get("servidor", null) == null || preferences.get("contrasenha", null) == null) {
			preferences.put("servidor",
					JOptionPane.showInputDialog(null, "Servidor MySQL","Configuracion", JOptionPane.QUESTION_MESSAGE));
			preferences.put("puerto",
					JOptionPane.showInputDialog(null, "Puerto", "Configuracion", JOptionPane.QUESTION_MESSAGE));
			preferences.put("usuario",
					JOptionPane.showInputDialog(null, "Usuario","Configuracion", JOptionPane.QUESTION_MESSAGE));
			preferences.put("contrasenha",
					JOptionPane.showInputDialog(null, "Contrasenha", "Configuracion", JOptionPane.QUESTION_MESSAGE));
		}

		connectionString = String.format("jdbc:mysql://%1$s:%2$s/adsl?user=%3$s&password=%4$s",
				preferences.get("servidor", "localhost"), 
				preferences.get("puerto", "3306"),
				preferences.get("usuario", null), 
				preferences.get("contrasenha", null)).toString();

		//Check for driver
		/*
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			_log.log(Level.SEVERE, "Driver no encontrado", e);
		}
		*/

	}

	void guardaDatosMySQL(int[] datos) {

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
							"INSERT INTO datos (ip_id,download,upload,attdownrate,attuprate,downpower,uppower) VALUES (?,?,?,?,?,?,?)");
					stmt.setInt(1, ip_id);
					for (int i = 0; i < datos.length; i++) {
						stmt.setInt(i + 2, datos[i]);
					}
					stmt.executeUpdate();
				}

			} catch (SQLException e) {
				// if the error message is "out of memory",
				// it probably means no database file is found
				_log.log(Level.SEVERE,null, e);
				 preferences.remove("servidor");
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
