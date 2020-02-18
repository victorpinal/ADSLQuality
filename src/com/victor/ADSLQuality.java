/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.victor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * @author Victor
 */
public class ADSLQuality {

	private static final Logger _log = Logger.getLogger(ADSLQuality.class.getName());
    private Preferences preferences = Preferences.userNodeForPackage(ADSLQuality.class);

    public ADSLQuality(String dbName, int segundosIntervalo) {

        //INIT DATABASES
    	mySQL mysql = new mySQL();
        SqlLite sqlite = new SqlLite(dbName);     
        
        //SAVED PREFERENCES
        String router_ip = getRouterIp();
        Integer router_type = getRouterType();   
        
        //START TIMER
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                	HashMap<String, BigDecimal> datos = new HashMap<>();
                    switch (router_type) {
					case 1:		
						datos = getDataVodafone(router_ip);
						break;
					case 2:
						datos = getDataLinksys(router_ip);
						break;
					}
                    
                    sqlite.guardaDatos(datos);
                    mysql.guardaDatosMySQL(datos);

                } catch (Exception ex) {
                	//preferences.clear();
                	_log.log(Level.SEVERE, null, ex);
                	t.cancel();
                	t.purge();
                }
            }
        }, 1000, (long) (segundosIntervalo * 1000));

    }
    
    private int getRouterType() {
        Integer router_type = preferences.getInt("router_type", 0);
        if (router_type==0) {
        	router_type = Integer.valueOf(JOptionPane.showInputDialog(null,"Type router (1.Huawei HG556a|2.Linksys WAG54G2)","Configuracion", JOptionPane.QUESTION_MESSAGE,
        			null,null,"1").toString());        	        
			preferences.putInt("router_type", router_type);
        }
        return router_type;
    }
    
    private String getRouterIp() {
    	String router_ip = preferences.get("router_ip", null);
        if (router_ip == null) {
        	router_ip = JOptionPane.showInputDialog(null, "IP router","Configuracion", JOptionPane.QUESTION_MESSAGE,
					null,null,"192.168.0.1").toString();
			preferences.put("router_ip", router_ip);
        }
        return router_ip;
    }
    
    private String getBase64login() {
    	String router_user = preferences.get("router_user", null);        
    	String router_password = preferences.get("router_password", null);
        if (router_user == null || router_password == null) {
        	router_user = JOptionPane.showInputDialog(null, "Router user","Configuracion", JOptionPane.QUESTION_MESSAGE,
					null,null,"admin").toString();        	
        	router_password = JOptionPane.showInputDialog(null, "Router password","Configuracion", JOptionPane.QUESTION_MESSAGE,
					null,null,"password").toString();
			preferences.put("router_user", router_user);					
			preferences.put("router_password", router_password);					
        }
        String login = router_user+":"+router_password;
        String b64login = Base64.getEncoder().encodeToString(login.getBytes());
        System.out.println("router login b64:" + b64login);
        return b64login;
    }
    
    private HashMap<String, BigDecimal> getDataVodafone(String ip) throws Exception {
    	
    	
    	String path = "/es_ES/diag.html"; //VODAFONE 
        Document doc = Jsoup
                .connect("http://"+ip+path)
                .header("Authorization", "Basic " + getBase64login())
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
        HashMap<String, BigDecimal> datos = new HashMap<>();                      
        Matcher m1 = Pattern.compile("iStatUpNoiseMargin.+'([\\d\\.]+)'.+").matcher(doc.html());
        if (m1.find()) {
            datos.put(Parameters.SNR_UL, new BigDecimal(m1.group(1)));
            encontrado = true;
        }
        Matcher m2 = Pattern.compile("iStatDownNoiseMargin.+'([\\d\\.]+)'.+").matcher(doc.html());
        if (m2.find()) {
        	datos.put(Parameters.SNR_DL, new BigDecimal(m2.group(1)));
            encontrado = true;
        }
        Matcher m3 = Pattern.compile("iStatUpMaxLineSpeed.+'([\\d\\.]+)'.+").matcher(doc.html());
        if (m3.find()) {
        	datos.put(Parameters.DataRate_UL, new BigDecimal(m3.group(1)));
            encontrado = true;
        }
        Matcher m4 = Pattern.compile("iStatDownMaxLineSpeed.+'([\\d\\.]+)'.+").matcher(doc.html());
        if (m4.find()) {
        	datos.put(Parameters.DataRate_DL, new BigDecimal(m4.group(1)));
            encontrado = true;
        }
        Matcher m5 = Pattern.compile("iStatDownOutputPower.+'([\\d\\.]+)'.+").matcher(doc.html());
        if (m5.find()) {
        	datos.put(Parameters.Power_DL, new BigDecimal(m5.group(1)));
            encontrado = true;
        }
        Matcher m6 = Pattern.compile("iStatUpOutputPower.+'([\\d\\.]+)'.+").matcher(doc.html());
        if (m6.find()) {
        	datos.put(Parameters.Power_UL, new BigDecimal(m6.group(1)));
            encontrado = true;
        }
        
        if (!encontrado) {
        	throw new Exception("Route path invalid");
        }
        
        return datos;
    }
    
private HashMap<String, BigDecimal> getDataLinksys(String ip) throws Exception {
    	
        String path = "/setup.cgi?next_file=adsl_driver.htm"; //CISCO LYNKSYS 
        Document doc = Jsoup
                .connect("http://"+ip+path)
                .header("Authorization", "Basic " + getBase64login())
                .get();                    

        boolean encontrado = false;
        BigDecimal divisor_MB = new BigDecimal(1024);
        HashMap<String, BigDecimal> datos = new HashMap<>();                   
        Matcher m1 = Pattern.compile("DSL Noise Margin.+?([\\d.]+).+?([\\d.]+)").matcher(doc.html());
        if (m1.find()) {
        	System.out.println(m1.group());
        	datos.put(Parameters.SNR_UL, new BigDecimal(m1.group(2)));
        	datos.put(Parameters.SNR_DL, new BigDecimal(m1.group(1)));
            encontrado = true;
        }
        Matcher m3 = Pattern.compile("DSL Upstream Rate.+?(\\d+)").matcher(doc.html());
        if (m3.find()) {        
        	System.out.println(m3.group());
        	datos.put(Parameters.DataRate_UL, new BigDecimal(m3.group(1)).divide(divisor_MB,1,RoundingMode.FLOOR));
            encontrado = true;
        }
        Matcher m4 = Pattern.compile("DSL Downstream Rate.+?(\\d+)").matcher(doc.html());
        if (m4.find()) {
        	System.out.println(m4.group());
        	datos.put(Parameters.DataRate_DL, new BigDecimal(m4.group(1)).setScale(3).divide(divisor_MB,1,RoundingMode.FLOOR));
            encontrado = true;
        }
        Matcher m5 = Pattern.compile("DSL Transmit Power.+?([\\d.]+).+?([\\d.]+)").matcher(doc.html());
        if (m5.find()) {
        	System.out.println(m5.group());
        	datos.put(Parameters.Power_UL, new BigDecimal(m5.group(2)));
        	datos.put(Parameters.Power_DL, new BigDecimal(m5.group(1)));
            encontrado = true;
        }
        Matcher m6 = Pattern.compile("DSL Attenuation.+?([\\d.]+).+?([\\d.]+)").matcher(doc.html());
        if (m6.find()) {
        	System.out.println(m6.group());
        	datos.put(Parameters.Attenuation_UL, new BigDecimal(m6.group(2)));
        	datos.put(Parameters.Attenuation_DL, new BigDecimal(m6.group(1)));
            encontrado = true;
        }
        if (!encontrado) {
        	throw new Exception("Route path invalid");
        }
        
        return datos;
    }
    
    private HashMap<String, BigDecimal> getDataJazztel(String ip) throws Exception {
      
        Map<String, String> headers = new HashMap<>();        
        headers.put("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        headers.put("Accept-Encoding","gzip, deflate");
        headers.put("Accept-Language","es");
        headers.put("Authorization", "Basic " + getBase64login());
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
        //InternetExplorer exp = new net.sf.jiffie.InternetExplorer();            
        /*exp.addNavigationListener(new NavigationListener() {
			
			@Override
			public void progressChange(int arg0, int arg1) {
				System.out.println("progressChange " + arg0 + "-" + arg1);
				if (arg0+arg1==0) {
					try {
						exp.getDocument(true);
					} catch (JiffieException e) {
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
        */        
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
