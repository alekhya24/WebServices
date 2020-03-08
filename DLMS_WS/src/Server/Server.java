package Server;

import java.io.File;
import java.util.HashMap;

import javax.xml.ws.Endpoint;
import Util.Constants;
import Util.Servers;

public class Server {
	static HashMap<String,ServerImpl> serverData;
	static ServerImpl serverCON,serverMCG,serverMON;

	public static void main(String args[]) {
		serverSetup();
		
		Endpoint endPointCon = Endpoint.publish("http://localhost:2121/CON", serverCON);
		Endpoint endPointMcg = Endpoint.publish("http://localhost:2122/MCG", serverMCG);
		Endpoint endPointMon = Endpoint.publish("http://localhost:2123/MON", serverMON);

		if (endPointCon.isPublished())
			System.out.println("CON Web Service is published!");
		if (endPointMcg.isPublished())
			System.out.println("MCG Web Service is published!");
		if (endPointMon.isPublished())
			System.out.println("MON Web Service is published!");

	}
	private static void serverSetup() {
		new File(Constants.LOG_DIR).mkdirs();
		new File(Constants.LOG_DIR+Servers.CON.getserverName().toString()).mkdirs();
		new File(Constants.LOG_DIR+Servers.MCG.getserverName().toString()).mkdir();
		new File(Constants.LOG_DIR+Servers.MON.getserverName().toString()).mkdir();	
		
		serverCON=new ServerImpl(Servers.CON);
		serverMCG=new ServerImpl(Servers.MCG);
		serverMON=new ServerImpl(Servers.MON);

		serverData = new HashMap<>();
		serverData.put("CON",serverCON);
		serverData.put("MCG",serverMCG);
		serverData.put("MON",serverMON);
	}
}
