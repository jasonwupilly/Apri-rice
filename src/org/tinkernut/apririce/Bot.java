package org.tinkernut.apririce;

import java.io.BufferedReader;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.tinkernut.apririce.commands.AnnounceCommand;
import org.tinkernut.apririce.commands.Command;
import org.tinkernut.apririce.commands.DefineCommand;
import org.tinkernut.apririce.commands.HelpCommand;
import org.tinkernut.apririce.commands.JoinCommand;
import org.tinkernut.apririce.commands.LogCommand;
import org.tinkernut.apririce.commands.NickCommand;
import org.tinkernut.apririce.commands.NickServCommand;
import org.tinkernut.apririce.commands.QuitCommand;
import org.tinkernut.apririce.commands.leaveCommand;
import org.tinkernut.apririce.textUtils.Parser;
import jerklib.ConnectionManager;
import jerklib.Profile;
import jerklib.events.IRCEvent;
import jerklib.events.JoinCompleteEvent;
import jerklib.events.JoinEvent;
import jerklib.events.MessageEvent;
import jerklib.events.QuitEvent;
import jerklib.events.IRCEvent.Type;
import jerklib.listeners.IRCEventListener;

public class Bot implements IRCEventListener, Runnable {
	/**
	 * Globals
	 */
	public String channelName;
	public boolean isLogging = false;
	private boolean isUsingPassword = false;
	private final String CMD_START = "|";
	private String ircServer;
	public ConnectionManager con;
	private HashMap<String, Command> commandsMap;
	private BufferedWriter bWriter;
	private BufferedReader bReader;
	private String botName;
	public LinkedList<User> userList;
	private String password = "";
	//Global instance commands
	private Command announceCommand;

	ExecutorService privateExecutorService = Executors.newCachedThreadPool();
	ExecutorService publicExecutorService = Executors.newCachedThreadPool();	

	/**
	 * Class constructor
	 */
	public Bot(String server, String channel, String botName) {
		// Initialize globals		
		commandsMap = new HashMap<String, Command>();

		announceCommand = new AnnounceCommand();

		userList = new LinkedList<User>();

		ircServer = server;
		channelName = channel;
		this.botName = botName;

		// TODO: Create storage

		File configFile = new File(channelName + "_config.txt");

		try {

			if (configFile.exists()) {
				bReader = new BufferedReader(new FileReader(configFile));
				
				String nick = bReader.readLine();
				password = bReader.readLine();
				isUsingPassword = true;

				if (nick == null) {
					nick = this.botName;
					System.out.println("No nick specified in " + configFile.getName() + ". Using default.");			
				}else {
					this.botName = nick;
				}

				if (password == null) {
					password = "";
					isUsingPassword = false;
					System.out.println("No password specified in " + configFile.getName() + ". Not using password.");					
				}

				con = new ConnectionManager(new Profile(this.botName));
			}else {			
				// Bot profile (nick)
				System.out.println("No " + configFile.getName() + " in directory. Creating "+ configFile.getName() +" and using default nick and password (no password by default).");
				con = new ConnectionManager(new Profile(this.botName));

				configFile.createNewFile();
			}
		} catch (IOException e) {
			System.out.println("File " + configFile.getName() + " inaccessible.");
		}

	}

	//Thread start
	public void run() {
		// Request Connection to server
		try {
			con.requestConnection(ircServer).addIRCEventListener(this);
		}
		catch (Exception e) {
			System.out.println("Failed to connect to channel. Exiting.");
			return;
		}
	}
	/**
	 * Event handler
	 */
	public void receiveEvent(IRCEvent e) {
		System.out.println(e.getRawEventData());

		Type type = e.getType();
		// Connection to server successful
		if (type == Type.CONNECT_COMPLETE) {
			e.getSession().join(channelName);

			// Connection to channel successful
		} else if (type == Type.JOIN_COMPLETE) {
			JoinCompleteEvent jce = (JoinCompleteEvent) e;
			
			if (isUsingPassword) {
				jce.getSession().sayPrivate("nickserv", "identify " + password);
			}

			//TODO: Rewrite user logging, saving, etc,.

			// User successfuly joins channel
		} else if (type == Type.JOIN) {
			JoinEvent je = (JoinEvent) e;
			// Create User instance variable for each new user in userList
			if (!userList.contains(new User(je.getNick()))) {
				userList.add(new User(je.getNick().toLowerCase()));
			}
			// User successfuly leaves channel
		} else if (type == Type.QUIT) {
			QuitEvent qe = (QuitEvent) e;

			// Message successfuly recieved in channel
		} else if (type == Type.CHANNEL_MESSAGE) {
			// Logging.
			if (isLogging) {
				try {
					bWriter = new BufferedWriter(new FileWriter("log.txt", true));
					bWriter.write(e.getRawEventData());
					bWriter.newLine();
					bWriter.close();
				} catch (IOException e1) {
					System.out.println("Error. Could not open log file.");
				}
			}

			MessageEvent me = (MessageEvent) e;
			//Check and execute any commands
			if (me.getMessage().startsWith(CMD_START)) {
				String commandString = Parser.stripCommand(me.getMessage());

				//Local instance commands
				Command helpCommand = new HelpCommand();
				Command defineCommand = new DefineCommand();
				Command logCommand = new LogCommand();
				Command quitCommand = new QuitCommand();
				Command nickCommand = new NickCommand();
				Command leaveCommand = new leaveCommand();
				Command joinCommand = new JoinCommand();
				//I doubt this works yet -> Command userCommand = new UserCommand();

				//Put identifier and associated command
				commandsMap.put("help", helpCommand);
				commandsMap.put("define", defineCommand);
				commandsMap.put("announce", announceCommand);
				commandsMap.put("log", logCommand);
				commandsMap.put("quit", quitCommand);
				commandsMap.put("nick", nickCommand);
				commandsMap.put("leave", leaveCommand);
				commandsMap.put("join", joinCommand);
				//I doubt this works yet -> commandsMap.put("user", userCommand);

				if (commandsMap.containsKey(commandString)) {
					commandsMap.get(commandString).init(Parser.stripArguments(me.getMessage()), me, this);

					publicExecutorService.execute(commandsMap.get(commandString));
				}else {
					me.getChannel().say("Not a command.");
				}
			}
			// Private message successfuly recieved
		} else if (type == Type.PRIVATE_MESSAGE) {
			// Logging.
			if (isLogging) {
				try {
					bWriter = new BufferedWriter(new FileWriter("log.txt", true));
					bWriter.write(e.getRawEventData());
					bWriter.newLine();
					bWriter.close();
				} catch (IOException e1) {
					System.out.println("Error. Could not open log file.");
				}
			}

			MessageEvent me = (MessageEvent) e;
			if (me.getMessage().startsWith(CMD_START)) {			
				//Check and execute any commands
				String commandString = Parser.stripCommand(me.getMessage());

				//Local instance commands
				Command helpCommand = new HelpCommand();
				Command nickServCommand = new NickServCommand();
				Command logCommand = new LogCommand();
				Command quitCommand = new QuitCommand();
				Command nickCommand = new NickCommand();
				Command joinCommand = new JoinCommand();
				//Probably doesn't work! -> Command userCommand = new UserCommand();

				//Put identifier and associated command
				commandsMap.put("nickserv", nickServCommand);
				commandsMap.put("log", logCommand);
				commandsMap.put("help", helpCommand);
				commandsMap.put("quit", quitCommand);
				commandsMap.put("nick", nickCommand);
				commandsMap.put("join", joinCommand);
				// Probably doesn't work! I mean totally doesn't work yet!!! -> commandsMap.put("user", userCommand);

				if (commandsMap.containsKey(commandString)) {
					commandsMap.get(commandString).initPriv(Parser.stripArguments(me.getMessage()), me, this);

					privateExecutorService.execute(commandsMap.get(commandString));
				}
				else {
					me.getSession().sayPrivate(me.getNick(), "Not a command.");
				}
			}
		}
	}
}