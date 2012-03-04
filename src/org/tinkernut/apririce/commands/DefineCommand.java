// TODO: Finish DefineCommand
package org.tinkernut.apririce.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import jerklib.events.MessageEvent;
import org.tinkernut.apririce.Bot;
import org.tinkernut.apririce.User;
import org.tinkernut.apririce.commands.Command;
import org.tinkernut.apririce.textUtils.Parser;
import org.tinkernut.apririce.textUtils.TextBuffer;

public class DefineCommand extends Command {
	public void run() {
		Map<String, String> urlMap = new HashMap<String, String>();
		urlMap.put("urban", "http://www.urbandictionary.com/define.php?term=");
		
		if (urlMap.containsKey(params) && Parser.stripAguments(params) != "") {
			try {
				//Establish connection and download HTML source
				URLConnection urlConnection = new URL(urlMap.get(params.substring(0, params.indexOf(' ')))+Parser.stripAguments(params)).openConnection();
				BufferedReader bReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
				
				//Put whole page source into string
				String HTMLSource = "";
				
				while((bReader.readLine()) != null) {
					HTMLSource += bReader.readLine();
				}
				HTMLSource = HTMLSource.replace("\n\t", " ");
				
				//Extract definition
				int start = HTMLSource.indexOf("<div class=\"definition\">")+"<div class=\"definition\">".length();
				int end = HTMLSource.indexOf("</div>", start);
				String definition = HTMLSource.substring(start, end);
				new TextBuffer().addAndDisplay(definition, me);
			} catch (IOException e) {
				new TextBuffer().addAndDisplay("Unable to establish connection.", me);
			}
		}
		else {
			new TextBuffer().addAndDisplay("Input something to define", me);
		}
	}

	protected void execPriv(final Bot bot, final User sender, final String params, final MessageEvent me) {

	}
}
