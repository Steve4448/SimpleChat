package client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;

public class EmotionThemeParser {
	public static HashMap<String, URL> LoadEmotes(InputStream themeStream) throws Exception {
		HashMap<String, URL> hash = new HashMap<String, URL>();
		BufferedReader themeReader = new BufferedReader(new InputStreamReader(themeStream));
		String inData = null;
		boolean pastStart = false;
		while((inData = themeReader.readLine()) != null) {
			if(inData.contains("[") && inData.contains("]"))
				pastStart = true;
			if(pastStart)
				if(inData.contains(".gif") || inData.contains(".png") || inData.contains(".jpg") || inData.contains(".ico")) {
					String[] args = inData.split(" ");
					int filenameOffset = 0;
					for(int i = 0; i < args.length; i++)
						if(args[i].contains(".gif") || args[i].contains(".png") || args[i].contains(".jpg") || args[i].contains(".ico")) {
							filenameOffset = i;
							break;
						}
					for(int i = filenameOffset + 1; i < args.length; i++)
						if(!args[i].equals(" ") && !args[i].equals(""))
							hash.put(args[i], Client.loader.getResource("resource/" + args[filenameOffset]));
				}
		}
		return hash;
	}
}
