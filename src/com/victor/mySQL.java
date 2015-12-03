package com.victor;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * Created by victormanuel on 03/12/2015.
 */
public class mySQL {

    Preferences preferences = Preferences.userNodeForPackage(mySQL.class);
    String connectionString = "";

    public mySQL() {

        //Load connection data
        if (preferences.get("servidor", null) == null) {
            preferences.put("servidor", JOptionPane.showInputDialog(null, "Configuración", "Servidor MySQL", JOptionPane.QUESTION_MESSAGE));
            preferences.put("puerto", JOptionPane.showInputDialog(null, "Configuración", "Puerto", JOptionPane.QUESTION_MESSAGE));
            preferences.put("usuario", JOptionPane.showInputDialog(null, "Configuración", "Usuario", JOptionPane.QUESTION_MESSAGE));
            preferences.put("contraseña", JOptionPane.showInputDialog(null, "Configuración", "Contraseña", JOptionPane.QUESTION_MESSAGE));
        }

        connectionString = new java.util.Formatter().format("jdbc:mysql://%1$s:%2$s/adsl?user=%3$s&password=%4$s",
                preferences.get("servidor", "localhost"),
                preferences.get("puerto", "3360"),
                preferences.get("usuario", null),
                preferences.get("contraseña", null)).toString();

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            Logger.getLogger(mySQL.class.getName()).log(Level.SEVERE, null, e);
        }

    }

    void guardaDatosMySQL(int[] datos) {

        int ip_id = 0;
        String ip = "localhost";

        try {

            Class.forName("com.mysql.jdbc.Driver");

            Connection connection = null;
            PreparedStatement stmt;
            ResultSet rs;

            try {

                // create a database connection
                connection = DriverManager.getConnection(connectionString);

                //buscamos la ip externa del equipo
                try {
                    URL whatismyip = new URL("http://checkip.amazonaws.com");
                    BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
                    ip = in.readLine(); //you get the IP as a String
                } catch (IOException e) {
                    e.printStackTrace();
                }

                stmt = connection.prepareStatement("SELECT id FROM ip WHERE ip = ? AND name = ?");
                stmt.setString(1, ip);
                stmt.setString(2, InetAddress.getLocalHost().getHostName());
                rs = stmt.executeQuery();

                if (rs.next()) {
                    ip_id = rs.getInt(1);
                } else {
                    rs.close();
                    stmt.close();
                    stmt = connection.prepareStatement("INSERT INTO ip (ip,name) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS);
                    stmt.setString(1, ip);
                    stmt.setString(2, InetAddress.getLocalHost().getHostName());
                    stmt.executeUpdate();
                    rs = stmt.getGeneratedKeys();
                    if (rs.next()) {
                        ip_id = rs.getInt(1);
                    }
                }

                if (ip_id > 0) {
                    rs.close();
                    stmt.close();
                    stmt = connection.prepareStatement("INSERT INTO datos (ip_id,download,upload,attdownrate,attuprate,downpower,uppower) VALUES (?,?,?,?,?,?,?)");
                    stmt.setInt(1, ip_id);
                    for (int i = 0; i < datos.length; i++) {
                        stmt.setInt(i + 2, datos[i]);
                    }
                    stmt.executeUpdate();
                }

            } catch (SQLException e) {
                // if the error message is "out of memory",
                // it probably means no database file is found
                System.err.println(e.getMessage());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (connection != null) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    // connection close failed.
                    System.err.println(e.getMessage());
                }
            }

        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ADSLQuality.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
