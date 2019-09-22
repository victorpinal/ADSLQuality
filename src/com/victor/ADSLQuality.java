/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.victor;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import net.sf.jiffie.IWebBrowser2;
import net.sf.jiffie.InternetExplorer;
import net.sf.jiffie.JiffieException;
import net.sf.jiffie.NavigationListener;
import net.sf.jiffie.WindowListener;

/**
 * @author Victor
 */
public class ADSLQuality {

	private static final Logger _log = Logger.getLogger(ADSLQuality.class.getName());
    private mySQL mysql;
    private SqlLite sqlite;
    private String ip_router;
    private Integer type_router;
    private Preferences preferences = Preferences.userNodeForPackage(ADSLQuality.class);

    public ADSLQuality(String dbName, int segundosIntervalo) {

        mysql = new mySQL();
        sqlite = new SqlLite(dbName);        
        
        ip_router = preferences.get("router", null);        
        if (ip_router == null) {
        	ip_router = JOptionPane.showInputDialog(null, "IP router","Configuracion", JOptionPane.QUESTION_MESSAGE,
					null,null,"192.168.0.1").toString();
        	
			preferences.put("router", ip_router);					
        }
        
        type_router = preferences.getInt("type",0);
        if (type_router==0) {
        	type_router = Integer.valueOf(JOptionPane.showInputDialog(null,"Type router (1.Vodafone|2.Jazztel)","Configuracion", JOptionPane.QUESTION_MESSAGE,
        			null,null,"1").toString());
        	preferences.putInt("type", type_router);
        }

        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                	int[] datos = new int[6];
                    switch (type_router) {
					case 1:		
						datos = getDataVodafone(ip_router);
						break;
					case 2:
						datos = getDataJazztel(ip_router);
						break;
					}
                    
                    sqlite.guardaDatos(datos);
                    mysql.guardaDatosMySQL(datos);

                } catch (Exception ex) {
                	//preferences.remove("router");
                	_log.log(Level.SEVERE, null, ex);
                	t.cancel();
                	t.purge();
                }
            }
        }, 1000, (long) (segundosIntervalo * 1000));

    }
    
    private int[] getDataVodafone(String ip) throws Exception {
    	String login = "vodafone:vodafone";
        String base64login = javax.xml.bind.DatatypeConverter.printBase64Binary(login.getBytes());
        Document doc = Jsoup
                .connect("http://"+ip+"/es_ES/diag.html")
                .header("Authorization", "Basic " + base64login)
                .get();                    

        // DATOS CON ADMINISTRADOR
//        Element e1 = doc.getElementsContainingOwnText("SNR Margin (0.1 dB):").first().parent();
//        Element e2 = doc.getElementsContainingOwnText("Attainable Rate (Kbps):").first().parent().parent();
//
//        guardaDatos(Integer.parseInt(e1.child(1).text()),
//                Integer.parseInt(e1.child(2).text()),
//                Integer.parseInt(e2.child(1).text()),
//                Integer.parseInt(e2.child(2).text()));

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
        
        return datos;
    }
    
    private int[] getDataJazztel(String ip) throws Exception {
    	String user = "admin"; //preferences.get("user", null);        
    	String pass = "vistorr"; preferences.get("pass", null);
        if (user == null) {
        	user = JOptionPane.showInputDialog(null, "User","Configuracion", JOptionPane.QUESTION_MESSAGE,
					null,null,"user").toString();        	
			preferences.put("user", user);					
        }
        if (pass == null) {
        	pass = JOptionPane.showInputDialog(null, "Pass","Configuracion", JOptionPane.QUESTION_MESSAGE,
					null,null,"user").toString();        	
			preferences.put("pass", pass);					
        }
    	String login = user+":"+pass;
        String base64login = javax.xml.bind.DatatypeConverter.printBase64Binary(login.getBytes());
        
        Map<String, String> headers = new HashMap<>();        
        headers.put("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        headers.put("Accept-Encoding","gzip, deflate");
        headers.put("Accept-Language","es");
        headers.put("Authorization", "Basic " + base64login);
        headers.put("Connection","keep-alive");
        headers.put("Host",ip);
        headers.put("Upgrade-Insecure-Requests","1");
        /*
        Connection con = Jsoup
        		.connect("http://"+ip+"/")        		
        		.headers(headers)
        		.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.108 Safari/537.36");        		
        Response resp = con.execute();
        Document doc = resp.parse();
        resp = con.url("http://"+ip+"/cgi-bin/status.asp")
                //.headers(headers)                
                //.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.108 Safari/537.36")
                //.cookie("SESSIONID", resp.cookie("SESSIONID"))
                .execute();
         */
        InternetExplorer exp = new net.sf.jiffie.InternetExplorer();            
        exp.addNavigationListener(new NavigationListener() {
			
			@Override
			public void progressChange(int arg0, int arg1) {
				System.out.println("progressChange " + arg0 + "-" + arg1);
				if (arg0+arg1==0) {
					try {
						exp.getDocument(true);
					} catch (JiffieException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
			@Override
			public boolean newWindow3(int arg0, String arg1, String arg2) {
				System.out.println("newWindow3");
				return false;
			}
			
			@Override
			public boolean newWindow2(InternetExplorer arg0) {
				System.out.println("newWindow2");
				return false;
			}
			
			@Override
			public boolean navigateError(String arg0, String arg1, int arg2) {
				System.out.println("navigateError");
				return false;
			}
			
			@Override
			public void navigateComplete(String arg0) {
				System.out.println("navigateComplete");
				
			}
			
			@Override
			public boolean fileDownload(boolean arg0) {
				System.out.println("fileDownload");
				return false;
			}
			
			@Override
			public void downloadComplete() {
				System.out.println("downloadComplete");
				
			}
			
			@Override
			public void downloadBegin() {
				System.out.println("downloadBegin");
				
			}
			
			@Override
			public void documentComplete(String arg0) {
				System.out.println("documentComplete");
								
			}
			
			@Override
			public boolean beforeNavigate(String arg0, String arg1) {
				System.out.println("beforeNavigate " + arg0 + "-" + arg1);
				return false;
			}
		});
        exp.navigate("http://"+ip,true);        
        /*
        String DownNoiseMargin = doc.select("body > form > table.tabdata > tbody > tr:nth-child(10) > td:nth-child(5)").first().text();
        String UpNoiseMargin = doc.select("body > form > table.tabdata > tbody > tr:nth-child(10) > td:nth-child(6)").first().text();
        String DownMaxLineSpeed = doc.select("body > form > table.tabdata > tbody > tr:nth-child(12) > td:nth-child(5)").first().text();
        String UpMaxLineSpeed = doc.select("body > form > table.tabdata > tbody > tr:nth-child(12) > td:nth-child(6)").first().text();        
        String DownOutputPower = doc.select("body > form > table.tabdata > tbody > tr:nth-child(14) > td:nth-child(5)").first().text();
        String UpOutputPower = doc.select("body > form > table.tabdata > tbody > tr:nth-child(14) > td:nth-child(6)").first().text();
        int[] datos = new int[6];
        datos[0] = Integer.parseInt(DownNoiseMargin.split(" ")[0].replace(".",""));
        datos[1] = Integer.parseInt(UpNoiseMargin.split(" ")[0].replace(".",""));
        datos[2] = Integer.parseInt(DownMaxLineSpeed.split(" ")[0].replace(".",""));
        datos[3] = Integer.parseInt(UpMaxLineSpeed.split(" ")[0].replace(".",""));
        datos[4] = Integer.parseInt(DownOutputPower.split(" ")[0].replace(".",""));
        datos[5] = Integer.parseInt(UpOutputPower.split(" ")[0].replace(".",""));
        */
        //exp.getd
        return null;
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
