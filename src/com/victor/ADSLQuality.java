/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.victor;

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
        	String routerPath = JOptionPane.showInputDialog(null, "Url estadisticas router DSL","Configuracion", JOptionPane.QUESTION_MESSAGE,
					null,null,"http://192.168.0.1/es_ES/diag.html").toString();
			preferences.put("router", routerPath);					
        }        

        Timer t = new Timer();
        t.schedule(new TimerTask() {
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
                    boolean encontrado = false;
                    int[] datos = new int[6];                      
                    Matcher m1 = Pattern.compile("iStatUpNoiseMargin.+'([\\d\\.]+)'.+").matcher(doc.html());
                    if (m1.find()) {
                        datos[1] = Integer.parseInt(m1.group(1).replace(".",""));
                        encontrado = true;
                    }
                    Matcher m2 = Pattern.compile("iStatDownNoiseMargin.+'([\\d\\.]+)'.+").matcher(doc.html());
                    if (m2.find()) {
                        datos[0] = Integer.parseInt(m2.group(1).replace(".",""));
                        encontrado = true;
                    }
                    Matcher m3 = Pattern.compile("iStatUpMaxLineSpeed.+'([\\d\\.]+)'.+").matcher(doc.html());
                    if (m3.find()) {
                        datos[3] = Integer.parseInt(m3.group(1).replace(".",""));
                        encontrado = true;
                    }
                    Matcher m4 = Pattern.compile("iStatDownMaxLineSpeed.+'([\\d\\.]+)'.+").matcher(doc.html());
                    if (m4.find()) {
                        datos[2] = Integer.parseInt(m4.group(1).replace(".",""));
                        encontrado = true;
                    }
                    Matcher m5 = Pattern.compile("iStatDownOutputPower.+'([\\d\\.]+)'.+").matcher(doc.html());
                    if (m5.find()) {
                        datos[4] = Integer.parseInt(m5.group(1).replace(".",""));
                        encontrado = true;
                    }
                    Matcher m6 = Pattern.compile("iStatUpOutputPower.+'([\\d\\.]+)'.+").matcher(doc.html());
                    if (m6.find()) {
                        datos[5] = Integer.parseInt(m6.group(1).replace(".",""));
                        encontrado = true;
                    }
                    
                    if (!encontrado) {
                    	throw new Exception("Route path invalid");
                    }
                    
                    sqlite.guardaDatos(datos);
                    mysql.guardaDatosMySQL(datos);

                } catch (Exception ex) {
                	preferences.remove("router");
                	_log.log(Level.SEVERE, null, ex);
                	t.cancel();
                	t.purge();
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
