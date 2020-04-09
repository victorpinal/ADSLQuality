package com.victor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SshConnectionManager2 {

    private Session session;

    private String username = "";
    private String password = "";
    private String hostname = "";

    public SshConnectionManager2() {
    }

    public SshConnectionManager2(String hostname, String username, String password) {
	this.hostname = hostname;
	this.username = username;
	this.password = password;
    }

    public void open() throws JSchException {
	open(this.hostname, this.username, this.password);
    }

    public void open(String hostname, String username, String password) throws JSchException {

	JSch jSch = new JSch();

	session = jSch.getSession(username, hostname, 22);
	Properties config = new Properties();
	config.put("StrictHostKeyChecking", "no"); // not recommended
	session.setConfig(config);
	session.setPassword(password);

	System.out.println("Connecting SSH to " + hostname + " - Please wait for few seconds... ");
	session.connect();
	System.out.println("Connected!");
    }

    public String runCommand(String command) throws JSchException, IOException {

	String ret = "";

	if (!session.isConnected())
	    throw new RuntimeException("Not connected to an open session.  Call open() first!");

	ChannelExec channel = null;
	channel = (ChannelExec) session.openChannel("exec");

	channel.setCommand(command);
	channel.setInputStream(null);

	PrintStream out = new PrintStream(channel.getOutputStream());
	InputStream in = channel.getInputStream(); // channel.getInputStream();

	channel.connect();

	// you can also send input to your running process like so:
	// String someInputToProcess = "something";
	// out.println(someInputToProcess);
	// out.flush();

	ret = getChannelOutput(channel, in);

	channel.disconnect();

	System.out.println("Finished sending commands!");

	return ret;
    }

    private String getChannelOutput(Channel channel, InputStream in) throws IOException {

	byte[] buffer = new byte[1024];
	StringBuilder strBuilder = new StringBuilder();

	String line = "";
	while (true) {
	    while (in.available() > 0) {
		int i = in.read(buffer, 0, 1024);
		if (i < 0) {
		    break;
		}
		strBuilder.append(new String(buffer, 0, i));
		System.out.println(line);
	    }

	    if (line.contains("logout")) {
		break;
	    }

	    if (channel.isClosed()) {
		break;
	    }
	    try {
		Thread.sleep(1000);
	    } catch (Exception ee) {
	    }
	}

	return strBuilder.toString();
    }

    public void close() {
	session.disconnect();
	System.out.println("Disconnected channel and session");
    }

    public static void main(String[] args) {
	SshConnectionManager2 ssh = new SshConnectionManager2();
        try {
            ssh.open();
            String ret = ssh.runCommand("ls -l");

            System.out.println(ret);
            ssh.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}