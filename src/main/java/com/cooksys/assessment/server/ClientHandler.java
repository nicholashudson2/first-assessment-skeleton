package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
//import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);
	private Socket socket;
	ObjectMapper mapper;
	BufferedReader reader;
	PrintWriter writer;
	Message message;
	String response;
	
	private static Map<String, ClientHandler> clientMap = new HashMap<>();

	public ClientHandler(Socket socket) {
		super();
		this.socket = socket;
	}
	
	
	public void run() {
		try {

			mapper = new ObjectMapper();
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			while (!socket.isClosed()) {
				String raw = reader.readLine();
				message = mapper.readValue(raw, Message.class);
				
				if(message.getCommand().contains("@")) {
					message.setTarget(message.getCommand().replace("@", ""));
					message.setCommand("@");
				}

				switch (message.getCommand()) {
					case "connect":
						log.info("user <{}> connected", message.getUsername());
						clientMap.put(message.getUsername(), this);
						break;
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						this.socket.close();
						break;
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						sendMessage(message);
						break;
					case "@":
						log.info("user <{}> whispered message <{}> to user <{}>", message.getUsername(), message.getContents(), message.getTarget());
						clientMap.get(message.getTarget()).sendMessage(message);
						break;
					case "broadcast":
						log.info("user <{}> shouted message <{}> to all users", message.getUsername(), message.getContents());
						clientMap.forEach((k,v) -> v.sendMessage(message));
						break;
				}
			}

		} catch (IOException e) {
			e.printStackTrace();;
			log.error("Something went wrong :/", e);
		}
	}
	
	public void sendMessage(Message message) {
		try {
			response = mapper.writeValueAsString(message);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
//		writer.write(message.getTimestamp().toString());
//		writer.flush();
		writer.write(response);
		writer.flush();
	}

}
