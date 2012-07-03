package org.tinkernut.apririce.commands;
import java.io.*; 
import java.util.HashSet; 
import org.tinkernut.apririce.textUtils.Parser;
import org.tinkernut.apririce.textUtils.TextBuffer;

public class ObsidCommand extends Command{
	private final String helpText = "Compatibility command for ObsidianBot. Syntax: |obsid <command> <params>";
	private HashSet<String> exeCommands = new HashSet<String>(); 
	private String path = "./src/org/tinkernut/apririce/commands/";
	void configHashSet()
	{
		//String files;
		File folder = new File(path);
		File[] listFiles = folder.listFiles();
		for (int i = 0; i < listFiles.length; i++)
		{
			System.out.println(listFiles[i].toString().trim());
			if (listFiles[i].toString().trim().endsWith(".exe"))
			{
			String exeFile = listFiles[i].toString().substring(38);
			exeCommands.add(exeFile);
			System.out.println(exeFile); 
			}
		}
	}
	public String exeExec(String exename, String channel, String rnick, String rmsg)
	{
		System.out.println("test1"); 
		try {
			System.out.println("test1"); 
				Process p = new ProcessBuilder(exename, channel, rnick, rmsg).start();
				BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String output = input.readLine().split(":")[1];
				System.out.println("test1"); 
				return output;

		} catch (IOException e) {
				// TODO Auto-generated catch block
				return e.toString();
		}
	}
	@Override
	void exec() {
		configHashSet(); 
		String channeltext = me.getChannel().toString().split("=")[1].replace("]", "").trim();
		String rnickname = me.getNick().toString();
		String rmessage = "!" + params; 
		System.out.println(channeltext); 
		System.out.println(rnickname); 
		System.out.println(rmessage); 
		if (Parser.stripArguments(params).equals("")) {
			TextBuffer.addAndDisplay(helpText, me);
		}
		for (String command : exeCommands)
		{
			System.out.println(command); 
			if (params.contains(command.replace(".exe", "")))
			{
				System.out.println(exeExec(command, channeltext, rnickname, rmessage)); 
				TextBuffer.addAndDisplay(exeExec(path + command, channeltext, rnickname, rmessage), me); 
			}
		}
	}

	@Override
	void execPriv() {
	}

}
