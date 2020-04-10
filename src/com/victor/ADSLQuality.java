/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.victor;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * @author Victor
 */
public class ADSLQuality {

	private static final Logger _log = Logger.getGlobal();
	private String _router_ip;
	private String _user;
	private String _pass;

	public ADSLQuality(String dbName, int segundosIntervalo) {		

		_log.entering(this.getClass().getName(), "ADSLQuality");

		// INIT DATABASES
		mySQL mysql = new mySQL();
		SqlLite sqlite = new SqlLite(dbName);

		// SAVED PREFERENCES
		int router_type = Integer.parseInt(PreferencesManager.getPreference("Router Type (1.Huawei HG556a|2.Linksys WAG54G2|3.Router Jazztel)", "router_type", "1"));
		_router_ip = PreferencesManager.getPreference("Router ip", "router_ip", "192.168.0.1");
		_user = PreferencesManager.getPreference("Router user", "router_user", "admin");
		_pass = PreferencesManager.getPreference("Router password", "router_password", "admin");

		_log.info(String.format("Prefrences: router_type <%d>, <%s@%s:%s>", router_type, _user, _pass, _router_ip));

		// START TIMER
		Timer t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					HashMap<String, BigDecimal> datos = null;
					switch (router_type) {
					case 1:
						datos = getDataVodafone();
						break;
					case 2:
						datos = getDataLinksys();
						break;
					case 3:
						datos = getDataJazztel_SSH2();
						break;
					}
					if (datos != null && !datos.isEmpty()) {
						sqlite.guardaDatos(datos);
						mysql.guardaDatos(datos);
						_log.info(String.format("%30s SNR %s/%s ATTENUATION %s/%s POWER %s/%s RATE %s/%s ATTAINABLE %s/%s", 
								new Date().toString(), 
								datos.get(Parameters.SNR_DL),
						        datos.get(Parameters.SNR_UL), 
						        datos.get(Parameters.Attenuation_DL), 
						        datos.get(Parameters.Attenuation_UL), 
						        datos.get(Parameters.Power_DL),
						        datos.get(Parameters.Power_UL), 
						        datos.get(Parameters.DataRate_DL), 
						        datos.get(Parameters.DataRate_UL), 
						        datos.get(Parameters.Attainable_DL),
						        datos.get(Parameters.Attainable_UL)));
					} else {
						_log.warning("Datos empty!");
					}
				} catch (HttpStatusException ex) {
					_log.log(Level.SEVERE, null, ex);
				} catch (Exception ex) {
					/*
					 * try { preferences.clear(); } catch (BackingStoreException e) {
					 * e.printStackTrace(); }
					 */
					_log.log(Level.SEVERE, null, ex);
					t.cancel();
					t.purge();
				}
			}
		}, 1000, (long) (segundosIntervalo * 1000));
		
		_log.exiting(this.getClass().getName(), "ADSLQuality");

	}

	private String getBase64login(String user, String pass) {
		String login = user + ":" + pass;
		String b64login = Base64.getEncoder().encodeToString(login.getBytes());
		return b64login;
	}

	private Document getHTML(String urlRouter) throws IOException {
		String url = String.format(urlRouter, _router_ip);
		Connection con = Jsoup.connect(url).header("Authorization", "Basic " + getBase64login(_user, _pass));
		_log.info(con.request().toString());
		try {
			return con.get();
		} catch (HttpStatusException e) {
			return con.get();
		}
	}

	private HashMap<String, BigDecimal> getDataVodafone() throws Exception {
		_log.entering(this.getClass().getName(), "getDataVodafone");
		Document doc = getHTML("http://%s/es_ES/diag.html"); // VODAFONE
		/*
		 * DATOS CON ADMINISTRADOR Element e1 =
		 * doc.getElementsContainingOwnText("SNR Margin (0.1 dB):").first().parent();
		 * Element e2 =
		 * doc.getElementsContainingOwnText("Attainable Rate (Kbps):").first().parent().
		 * parent(); guardaDatos(Integer.parseInt(e1.child(1).text()),
		 * Integer.parseInt(e1.child(2).text()), Integer.parseInt(e2.child(1).text()),
		 * Integer.parseInt(e2.child(2).text()));
		 */
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
		_log.exiting(this.getClass().getName(), "getDataVodafone");
		return datos;
	}

	private HashMap<String, BigDecimal> getDataLinksys() throws Exception {
		_log.entering(this.getClass().getName(), "getDataLinksys");
		Document doc = getHTML("http://%s/setup.cgi?next_file=adsl_driver.htm"); // CISCO LYNKSYS
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
		_log.exiting(this.getClass().getName(), "getDataLinksys");
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
	private HashMap<String, BigDecimal> getDataJazztel_HTML() throws Exception {
		_log.entering(this.getClass().getName(), "getDataJazztel_HTML");
		Document doc = getHTML("http://%s/cgi-bin/status_deviceinfo.asp"); // COMTREND ARxxxx JAZZTEL
		String DownNoiseMargin = doc.select("body > form > table.tabdata > tbody > tr:nth-child(10) > td:nth-child(5)").first().text();
		String UpNoiseMargin = doc.select("body > form > table.tabdata > tbody > tr:nth-child(10) > td:nth-child(6)").first().text();
		String DownMaxLineSpeed = doc.select("body > form > table.tabdata > tbody > tr:nth-child(12) > td:nth-child(5)").first().text();
		String UpMaxLineSpeed = doc.select("body > form > table.tabdata > tbody > tr:nth-child(12) > td:nth-child(6)").first().text();
		String DownOutputPower = doc.select("body > form > table.tabdata > tbody > tr:nth-child(14) > td:nth-child(5)").first().text();
		String UpOutputPower = doc.select("body > form > table.tabdata > tbody > tr:nth-child(14) > td:nth-child(6)").first().text();
		HashMap<String, BigDecimal> datos = new HashMap<>();
		datos.put(Parameters.SNR_DL, new BigDecimal(DownNoiseMargin.split(" ")[0].replace(".", "")));
		datos.put(Parameters.SNR_UL, new BigDecimal(UpNoiseMargin.split(" ")[0].replace(".", "")));
		datos.put(Parameters.DataRate_DL, new BigDecimal(DownMaxLineSpeed.split(" ")[0].replace(".", "")));
		datos.put(Parameters.DataRate_UL, new BigDecimal(UpMaxLineSpeed.split(" ")[0].replace(".", "")));
		datos.put(Parameters.Power_DL, new BigDecimal(DownOutputPower.split(" ")[0].replace(".", "")));
		datos.put(Parameters.Power_UL, new BigDecimal(UpOutputPower.split(" ")[0].replace(".", "")));
		_log.exiting(this.getClass().getName(), "getDataJazztel_HTML");
		return datos;
	}

	private HashMap<String, BigDecimal> getDataJazztel_SSH2() throws Exception {
		_log.entering(this.getClass().getName(), "getDataJazztel_SSH2");
		String command = "/userfs/bin/tcapi staticGet \"Info_Adsl\" "; // COMTREND ARxxxx JAZZTEL
		HashMap<String, BigDecimal> datos = new HashMap<>();
		Pattern regex = Pattern.compile("([\\d.]+).*", Pattern.DOTALL);
		Matcher m;
		SshConnectionManager2 ssh = new SshConnectionManager2(_router_ip, _user, _pass);
		ssh.open();

		m = regex.matcher(ssh.runCommand(command + "\"SNRMarginDown\""));
		if (m.find()) {
			datos.put(Parameters.SNR_DL, new BigDecimal(m.group(1)));
		}
		m = regex.matcher(ssh.runCommand(command + "\"SNRMarginUp\""));
		if (m.find()) {
			datos.put(Parameters.SNR_UL, new BigDecimal(m.group(1)));
		}
		m = regex.matcher(ssh.runCommand(command + "\"AttenDown\""));
		if (m.find()) {
			datos.put(Parameters.Attenuation_DL, new BigDecimal(m.group(1)));
		}
		m = regex.matcher(ssh.runCommand(command + "\"AttenUp\""));
		if (m.find()) {
			datos.put(Parameters.Attenuation_UL, new BigDecimal(m.group(1)));
		}
		m = regex.matcher(ssh.runCommand(command + "\"DataRateDown\""));
		if (m.find()) {
			datos.put(Parameters.DataRate_DL, new BigDecimal(m.group(1)));
		}
		m = regex.matcher(ssh.runCommand(command + "\"DataRateUp\""));
		if (m.find()) {
			datos.put(Parameters.DataRate_UL, new BigDecimal(m.group(1)));
		}
		m = regex.matcher(ssh.runCommand(command + "\"PowerDown\""));
		if (m.find()) {
			datos.put(Parameters.Power_DL, new BigDecimal(m.group(1)));
		}
		m = regex.matcher(ssh.runCommand(command + "\"PowerUp\""));
		if (m.find()) {
			datos.put(Parameters.Power_UL, new BigDecimal(m.group(1)));
		}
		m = regex.matcher(ssh.runCommand(command + "\"AttainDown\""));
		if (m.find()) {
			datos.put(Parameters.Attainable_DL, new BigDecimal(m.group(1)));
		}
		m = regex.matcher(ssh.runCommand(command + "\"AttainUp\""));
		if (m.find()) {
			datos.put(Parameters.Attainable_UL, new BigDecimal(m.group(1)));
		}

		ssh.close();
		_log.exiting(this.getClass().getName(), "getDataJazztel_SSH2");
		return datos;
	}

	/**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    	
    	// INIT LOGGER
		try {
			// This block configure the logger with handler and formatter
			FileHandler fh = new FileHandler(System.getProperty("user.home") + File.separator + ADSLQuality.class.getName() + ".log",true);
			fh.setFormatter(new SimpleFormatter());
			_log.addHandler(fh);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// INIT REST SERVER
		new RESTServer();
		
		// INIT ADSL 
    	if (args.length > 0) {
    	    new ADSLQuality("sample-debug.db", 5); // DEBUG CONSULTA CADA 5s
    	} else {
    	    new ADSLQuality("sample.db", 1800); // CONSULTA CADA 30 minutos (1800 s)
    	}    	
    }

}
