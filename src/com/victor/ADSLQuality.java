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
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * @author Victor
 */
public class ADSLQuality {

    private static final Logger _log = Logger.getLogger(ADSLQuality.class.getName());
    private Preferences preferences = Preferences.userNodeForPackage(ADSLQuality.class);

    public ADSLQuality(String dbName, int segundosIntervalo) {

	// INIT DATABASES
	mySQL mysql = new mySQL();
	SqlLite sqlite = new SqlLite(dbName);

	// SAVED PREFERENCES
	int router_type = Integer.parseInt(getPreference("Type router (1.Huawei HG556a|2.Linksys WAG54G2|3.Router Jazztel)", "router_type", "1"));
	String router_ip = getPreference("IP router", "router_ip", "192.168.0.1");
	String user = getPreference("Router user", "router_user", "admin");
	String pass = getPreference("Router password", "router_password", "admin");

	// START TIMER
	Timer t = new Timer();
	t.schedule(new TimerTask() {
	    @Override
	    public void run() {
		try {
		    HashMap<String, BigDecimal> datos = null;
		    switch (router_type) {
		    case 1:
			datos = getDataVodafone(router_ip, user, pass);
			break;
		    case 2:
			datos = getDataLinksys(router_ip, user, pass);
			break;
		    case 3:
			datos = getDataJazztel_SSH2(router_ip, user, pass);
			break;
		    }
		    if (datos != null && !datos.isEmpty()) {
			sqlite.guardaDatos(datos);
			mysql.guardaDatosMySQL(datos);
		    }
		} catch (HttpStatusException ex) {
		    _log.log(Level.SEVERE, null, ex);
		} catch (Exception ex) {
		    try {
			preferences.clear();
		    } catch (BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		    }
		    _log.log(Level.SEVERE, null, ex);
		    t.cancel();
		    t.purge();
		}
	    }
	}, 1000, (long) (segundosIntervalo * 1000));

    }

    private String getPreference(String message, String name, String def_value) {
	String value = preferences.get(name, null);
	if (value == null) {
	    value = JOptionPane.showInputDialog(null, message, "Configuracion", JOptionPane.QUESTION_MESSAGE, null, null, def_value).toString();
	    preferences.put(name, value);
	}
	return value;
    }

    private String getBase64login(String user, String pass) {
	String login = user + ":" + pass;
	String b64login = Base64.getEncoder().encodeToString(login.getBytes());
	System.out.println("router login b64:" + b64login);
	return b64login;
    }

    private HashMap<String, BigDecimal> getDataVodafone(String ip, String user, String pass) throws Exception {

	String path = "/es_ES/diag.html"; // VODAFONE
	Document doc = Jsoup.connect("http://" + ip + path).header("Authorization", "Basic " + getBase64login(user, pass)).get();

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

    private HashMap<String, BigDecimal> getDataLinksys(String ip, String user, String pass) throws Exception {

	String path = "/setup.cgi?next_file=adsl_driver.htm"; // CISCO LYNKSYS
	Document doc = null;
	try {
	    doc = Jsoup.connect("http://" + ip + path).header("Authorization", "Basic " + getBase64login(user, pass)).get();
	} catch (HttpStatusException e) {
	    doc = Jsoup.connect("http://" + ip + path).header("Authorization", "Basic " + getBase64login(user, pass)).get();
	}

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
	    datos.put(Parameters.DataRate_UL, new BigDecimal(m3.group(1)).divide(divisor_MB, 1, RoundingMode.FLOOR));
	    encontrado = true;
	}
	Matcher m4 = Pattern.compile("DSL Downstream Rate.+?(\\d+)").matcher(doc.html());
	if (m4.find()) {
	    System.out.println(m4.group());
	    datos.put(Parameters.DataRate_DL, new BigDecimal(m4.group(1)).setScale(3).divide(divisor_MB, 1, RoundingMode.FLOOR));
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

    /**
     * Esta opción para el router de jazztel no funciona porque se rompe el server
     * BOA HTML
     * 
     * @param ip
     * @return
     * @throws Exception
     */
    private HashMap<String, BigDecimal> getDataJazztel_HTML(String ip, String user, String pass) throws Exception {

	String path = "/cgi-bin/status_deviceinfo.asp"; // CISCO LYNKSYS
	Document doc = null;
	try {
	    doc = Jsoup.connect("http://" + ip + path).header("Authorization", "Basic " + getBase64login(user, pass)).get();
	} catch (HttpStatusException e) {
	    doc = Jsoup.connect("http://" + ip + path).header("Authorization", "Basic " + getBase64login(user, pass)).get();
	}

	String DownNoiseMargin = doc.select("body > form > table.tabdata > tbody > tr:nth-child(10) > td:nth-child(5)").first().text();
	String UpNoiseMargin = doc.select("body > form > table.tabdata > tbody > tr:nth-child(10) > td:nth-child(6)").first().text();
	String DownMaxLineSpeed = doc.select("body > form > table.tabdata > tbody > tr:nth-child(12) > td:nth-child(5)").first().text();
	String UpMaxLineSpeed = doc.select("body > form > table.tabdata > tbody > tr:nth-child(12) > td:nth-child(6)").first().text();
	String DownOutputPower = doc.select("body > form > table.tabdata > tbody > tr:nth-child(14) > td:nth-child(5)").first().text();
	String UpOutputPower = doc.select("body > form > table.tabdata > tbody > tr:nth-child(14) > td:nth-child(6)").first().text();
	int[] datos = new int[6];
	datos[0] = Integer.parseInt(DownNoiseMargin.split(" ")[0].replace(".", ""));
	datos[1] = Integer.parseInt(UpNoiseMargin.split(" ")[0].replace(".", ""));
	datos[2] = Integer.parseInt(DownMaxLineSpeed.split(" ")[0].replace(".", ""));
	datos[3] = Integer.parseInt(UpMaxLineSpeed.split(" ")[0].replace(".", ""));
	datos[4] = Integer.parseInt(DownOutputPower.split(" ")[0].replace(".", ""));
	datos[5] = Integer.parseInt(UpOutputPower.split(" ")[0].replace(".", ""));
	return null;
    }

    private HashMap<String, BigDecimal> getDataJazztel_SSH2(String ip, String user, String pass) throws Exception {

	String command = "/userfs/bin/tcapi staticGet \"Info_Adsl\" ";
	HashMap<String, BigDecimal> datos = new HashMap<>();
	Pattern regex = Pattern.compile("([\\d.]+).*",Pattern.DOTALL);
	Matcher m;
	SshConnectionManager2 ssh = new SshConnectionManager2(ip, user, pass);
	ssh.open();
	m = regex.matcher(ssh.runCommand(command + "\"SNRMarginDown\""));
	if (m.find()) { datos.put(Parameters.SNR_DL, new BigDecimal(m.group(1))); }
	m = regex.matcher(ssh.runCommand(command + "\"SNRMarginUp\""));
	if (m.find()) { datos.put(Parameters.SNR_UL, new BigDecimal(m.group(1))); }
	m = regex.matcher(ssh.runCommand(command + "\"AttenDown\""));
	if (m.find()) { datos.put(Parameters.Attenuation_DL, new BigDecimal(m.group(1))); }
	m = regex.matcher(ssh.runCommand(command + "\"AttenUp\""));
	if (m.find()) { datos.put(Parameters.Attenuation_UL, new BigDecimal(m.group(1))); }
	m = regex.matcher(ssh.runCommand(command + "\"DataRateDown\""));
	if (m.find()) { datos.put(Parameters.DataRate_DL, new BigDecimal(m.group(1))); }
	m = regex.matcher(ssh.runCommand(command + "\"DataRateUp\""));
	if (m.find()) { datos.put(Parameters.DataRate_UL, new BigDecimal(m.group(1))); }
	m = regex.matcher(ssh.runCommand(command + "\"PowerDown\""));
	if (m.find()) { datos.put(Parameters.Power_DL, new BigDecimal(m.group(1))); }
	m = regex.matcher(ssh.runCommand(command + "\"PowerUp\""));
	if (m.find()) { datos.put(Parameters.Power_UL, new BigDecimal(m.group(1))); }
	m = regex.matcher(ssh.runCommand(command + "\"AttainDown\""));
	if (m.find()) { datos.put(Parameters.Attainable_DL, new BigDecimal(m.group(1))); }
	m = regex.matcher(ssh.runCommand(command + "\"AttainUp\""));
	if (m.find()) { datos.put(Parameters.Attainable_UL, new BigDecimal(m.group(1))); }
	ssh.close();
	return datos;
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
