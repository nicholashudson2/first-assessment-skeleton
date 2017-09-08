package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);
	private Socket socket;
	String timeStampFormat = "yyyy-MM-dd HH:mm:ss";
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(timeStampFormat);
	private static Map<String, ClientHandler> clientMap = new HashMap<>();

	ObjectMapper mapper;
	BufferedReader reader;
	PrintWriter writer;
	Message message;
	String response;
	String date;
	String targetName;
	ClientHandler targetHandler;

	public ClientHandler(Socket socket) {
		super();
		this.socket = socket;
	}

	public void run() {
		try {

			ObjectMapper mapper = new ObjectMapper();
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			while (!socket.isClosed()) {
				String raw = reader.readLine();
				message = mapper.readValue(raw, Message.class);
				date = simpleDateFormat.format(new Date());

				if (message.getCommand().contains("@")) {
					targetName = message.getCommand().replace("@", "");
					targetHandler = clientMap.get(targetName);
					message.setCommand("@");
				}

				switch (message.getCommand()) {
				case "connect":
					log.info("user <{}> connected", message.getUsername());
					clientMap.put(message.getUsername(), this);
					if (clientMap.containsKey(message.getUsername()))
						message.setContents(date + ": <" + message.getUsername() + "> has connected");
					clientMap.forEach((k, v) -> v.sendMessage(message));
					break;
				case "disconnect":
					log.info("user <{}> disconnected", message.getUsername());
					clientMap.remove(message.getUsername());
					if (!clientMap.containsKey(message.getUsername()))
						message.setContents(date + ": <" + message.getUsername() + "> has disconnected");
					clientMap.forEach((k, v) -> v.sendMessage(message));
					this.socket.close();
					break;
				case "users":
					message.setContents(date + " : currently connected users:");
					sendMessage(message);
					for (String s : clientMap.keySet()) {
						message.setContents("\n<" + s + ">");
						sendMessage(message);
					}
					break;
				case "echo":
					log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
					message.setContents(date + " <" + message.getUsername() + "> (echo): " + message.getContents());
					sendMessage(message);
					break;
				case "@":
					log.info("user <{}> whispered message <{}> to user <{}>", message.getUsername(),
							message.getContents(), targetName);
					message.setContents(date + " <" + message.getUsername() + "> (whisper): " + message.getContents());
					(clientMap.get(targetName)).sendMessage(message);
					break;
				case "broadcast":
					log.info("user <{}> shouted message <{}> to all users", message.getUsername(),
							message.getContents());
					message.setContents(date + " <" + message.getUsername() + "> (all): " + message.getContents());
					clientMap.forEach((k, v) -> v.sendMessage(message));
					break;
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
			log.error("Something went wrong :/", e);
		} catch (NullPointerException e) {
			e.printStackTrace();
			log.error("Null Pointer Error :/");
		}
	}

	public void sendMessage(Message message) {
		writer.write(message.getContents());
		writer.flush();
	}

	public static ClientHandler[] getClientHandlers() {
		ClientHandler[] clientHandlers = new ClientHandler[clientMap.size()];
		int index = 0;
		for (Object key : clientMap.keySet()) {
			clientHandlers[index] = clientMap.get(key);
			++index;
		}
		return clientHandlers;
	}

}
