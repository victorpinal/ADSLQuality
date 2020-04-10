package com.victor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;;

public class RESTServer {

	private static final Logger _log = Logger.getGlobal();
	private HttpServer server;
	private mySQL mysql = new mySQL();

	public RESTServer() {
		try {
			server = HttpServer.create(new InetSocketAddress("localhost", 8001), 0);
			ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
			server.createContext("/adsl/datos", new MyHttpHandlerDatos());
			server.createContext("/adsl/ip", new MyHttpHandlerIp());
			server.createContext("/adsl", new MyHttpHandlerIndex());
			server.setExecutor(threadPoolExecutor);
			server.start();
			_log.info("HTTP Server started on port 8001. http://localhost:8001/adsl/[ip|datos[?ip=|ini=|fin=]]");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private abstract class MyHttpHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange httpExchange) throws IOException {
			HashMap<String, String> requestParams = null;
			if ("GET".equals(httpExchange.getRequestMethod())) {
				requestParams = handleGetRequest(httpExchange);
			}
			handleResponse(httpExchange, requestParams);
		}

		protected abstract HashMap<String, String> handleGetRequest(HttpExchange httpExchange);

		protected abstract void handleResponse(HttpExchange httpExchange, HashMap<String, String> requestParams) throws IOException;
	}
	
	private class MyHttpHandlerIndex extends MyHttpHandler {

		protected HashMap<String, String> handleGetRequest(HttpExchange httpExchange) {
			return null;
		}

		protected void handleResponse(HttpExchange httpExchange, HashMap<String, String> requestParams) throws IOException {
			OutputStream outputStream = httpExchange.getResponseBody();
			BufferedReader br = new BufferedReader(new FileReader("ADSLQuality.html"));
			StringBuilder sb = new StringBuilder();
			while (br.ready()) {
				sb.append(br.readLine());
				sb.append(System.lineSeparator());				
			}
			br.close();
			String htmlResponse = sb.toString();
			// this line is a must
			httpExchange.sendResponseHeaders(200, htmlResponse.length());
			outputStream.write(htmlResponse.getBytes());
			outputStream.flush();
			outputStream.close();
		}

	}

	private class MyHttpHandlerIp extends MyHttpHandler {

		protected HashMap<String, String> handleGetRequest(HttpExchange httpExchange) {
			return null;
		}

		protected void handleResponse(HttpExchange httpExchange, HashMap<String, String> requestParams) throws IOException {
			OutputStream outputStream = httpExchange.getResponseBody();
			String htmlResponse = new Gson().toJson(mysql.getIPList());
			// this line is a must
			httpExchange.sendResponseHeaders(200, htmlResponse.length());
			outputStream.write(htmlResponse.getBytes());
			outputStream.flush();
			outputStream.close();
		}

	}

	private class MyHttpHandlerDatos extends MyHttpHandler {

		protected HashMap<String, String> handleGetRequest(HttpExchange httpExchange) {
			HashMap<String, String> params = new HashMap<>();
			String[] url = httpExchange.getRequestURI().toString().split("\\?");
			if (url.length > 1) {
				for (String param : url[1].split("&")) {
					String[] s = param.split("=");
					params.put(s[0], s.length > 1 ? s[1] : null);
				}
			}
			return params;
		}

		protected void handleResponse(HttpExchange httpExchange, HashMap<String, String> requestParams) throws IOException {
			OutputStream outputStream = httpExchange.getResponseBody();			
			String ip = requestParams.get("ip");
			String ini = requestParams.get("ini");
			String fin = requestParams.get("fin");
			Date iniDate = null;
			Date finDate = null;
			try {
				iniDate = new SimpleDateFormat("yyyy-MM-dd").parse(ini);
				finDate = new SimpleDateFormat("yyyy-MM-dd").parse(fin);
			} catch (Exception e) {
			}
			String htmlResponse =  new Gson().toJson(mysql.getDatosList(ip, iniDate, finDate));
			// this line is a must
			httpExchange.sendResponseHeaders(200, htmlResponse.length());
			outputStream.write(htmlResponse.getBytes());
			outputStream.flush();
			outputStream.close();
		}

	}
	
	public static void main(String[] args) {
		new RESTServer();
	}
}
