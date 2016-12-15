/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.victor;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * @author Victor
 */
public class ADSLQuality {

	private static final Logger _log = Logger.getLogger(ADSLQuality.class.getName());
    private mySQL mysql;
    private SqlLite sqlite;
    private Preferences preferences = Preferences.userNodeForPackage(mySQL.class);

    public ADSLQuality(String dbName, int segundosIntervalo) {

        this.mysql = new mySQL();
        sqlite = new SqlLite(dbName);
        
        if (preferences.get("router", null) == null) {
			preferences.put("router",
					JOptionPane.showInputDialog(null, "Url estadisticas router DSL","Configuracion", JOptionPane.QUESTION_MESSAGE));
        }        

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    String login = "vodafone:vodafone";
                    String base64login = javax.xml.bind.DatatypeConverter.printBase64Binary(login.getBytes());
                    Document doc = Jsoup
                            .connect(preferences.get("router", "http://192.168.0.1/es_ES/diag.html"))
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
                    
                    sqlite.guardaDatos(datos);
                    mysql.guardaDatosMySQL(datos);

                } catch (IOException ex) {
                	preferences.put("router",null);
                    _log.log(Level.SEVERE, null, ex);
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

    

}
