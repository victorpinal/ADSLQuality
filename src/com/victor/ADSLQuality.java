/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.victor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Victor
 */
public class ADSLQuality {

    String dbName;
    int segundosIntervalo;
    mySQL mysql;

    public ADSLQuality(String dbName, int segundosIntervalo) {

        this.dbName = dbName;
        this.segundosIntervalo = segundosIntervalo;
        this.mysql = new mySQL();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    String login = "vodafone:vodafone";
                    String base64login = javax.xml.bind.DatatypeConverter.printBase64Binary(login.getBytes());
                    Document doc = Jsoup
                            .connect("http://192.168.0.1/es_ES/diag.html")
                            .header("Authorization", "Basic " + base64login)
                            .get();

                    // DATOS CON ADMINISTRADOR
//                    Element e1 = doc.getElementsContainingOwnText("SNR Margin (0.1 dB):").first().parent();
//                    Element e2 = doc.getElementsContainingOwnText("Attainable Rate (Kbps):").first().parent().parent();
//
//                    guardaDatos(Integer.parseInt(e1.child(1).text()),
//                            Integer.parseInt(e1.child(2).text()),
//                            Integer.parseInt(e2.child(1).text()),
//                            Integer.parseInt(e2.child(2).text()));

                    // DATOS SIN ADMINISTRADOR
                    int[] datos = new int[6];
                    Matcher m;
                    m = Pattern.compile(".+iStatUpNoiseMargin.+'(\\d+)'.+").matcher(doc.html());
                    if (m.find()) {
                        datos[1] = Integer.parseInt(m.group(1));
                    }
                    m = Pattern.compile(".+iStatDownNoiseMargin.+'(\\d+)'.+").matcher(doc.html());
                    if (m.find()) {
                        datos[0] = Integer.parseInt(m.group(1));
                    }
                    m = Pattern.compile(".+iStatUpMaxLineSpeed.+'(\\d+)'.+").matcher(doc.html());
                    if (m.find()) {
                        datos[3] = Integer.parseInt(m.group(1));
                    }
                    m = Pattern.compile(".+iStatDownMaxLineSpeed.+'(\\d+)'.+").matcher(doc.html());
                    if (m.find()) {
                        datos[2] = Integer.parseInt(m.group(1));
                    }
                    m = Pattern.compile(".+iStatDownOutputPower.+'(\\d+)'.+").matcher(doc.html());
                    if (m.find()) {
                        datos[4] = Integer.parseInt(m.group(1));
                    }
                    m = Pattern.compile(".+iStatUpOutputPower.+'(\\d+)'.+").matcher(doc.html());
                    if (m.find()) {
                        datos[5] = Integer.parseInt(m.group(1));
                    }
                    guardaDatos(datos);
                    mysql.guardaDatosMySQL(datos);

                } catch (IOException ex) {
                    Logger.getLogger(ADSLQuality.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }, 1000, (long) (segundosIntervalo * 1000));

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            new ADSLQuality("sample-debug.db", 5);
        } else {
            new ADSLQuality("sample.db", 1800);
        }

    }

    void guardaDatos(int[] datos) {

        try {

            // load the sqlite-JDBC driver using the current class loader
            Class.forName("org.sqlite.JDBC");

            Connection connection = null;
            try {
                // create a database connection
                connection = DriverManager.getConnection("jdbc:sqlite:" + System.getProperty("user.home") + "\\" + dbName);
                connection.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS datos (" +
                        "id INTEGER PRIMARY KEY," +
                        "fecha DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "download INTEGER," +
                        "upload INTEGER," +
                        "attdownrate INTEGER," +
                        "attuprate INTEGER," +
                        "downpower INTEGER," +
                        "uppower INTEGER)");
                PreparedStatement st = connection.prepareStatement("INSERT INTO datos (download,upload,attdownrate,attuprate,downpower,uppower) VALUES (?,?,?,?,?,?)");
                for (int i = 0; i < datos.length; i++) {
                    st.setInt(i + 1, datos[i]);
                }
                st.executeUpdate();
                System.out.format("%30s ---> %5d | %5d | %5d | %5d | %5d | %5d %n", new Date().toString(), datos[0], datos[1], datos[2], datos[3], datos[4], datos[5]);
            } catch (SQLException e) {
                // if the error message is "out of memory",
                // it probably means no database file is found
                System.err.println(e.getMessage());
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
