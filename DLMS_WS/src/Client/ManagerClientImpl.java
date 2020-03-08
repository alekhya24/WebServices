package Client;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import Server.IServerInterface;
import Util.Constants;
import Util.LogClient;
import Util.Servers;

public class ManagerClientImpl {

	Util.LogClient logClient = null;
	static IServerInterface serverInterface;
	URL url;
	Service service;

	ManagerClientImpl(Servers server, String managerId)
 {
			String folder="";
			try
			{
				QName qname = new QName("http://Server/","DlmsService");
				if (server == Servers.CON) {
					folder=Servers.CON.getserverName().toString();
					url = new URL("http://localhost:2121/CON?wsdl");
					
				} else if (server == Servers.MCG) {
					folder=Servers.MCG.getserverName().toString();
					url = new URL("http://localhost:2122/MCG?wsdl");

				} else if (server == Servers.MON) {
					folder=Servers.MON.getserverName().toString();
					url = new URL("http://localhost:2123/MON?wsdl");
				}
				service = Service.create(url,qname);
				serverInterface = service.getPort(IServerInterface.class);
			boolean mgrID = new File(Constants.LOG_DIR+folder +"\\"+managerId).mkdir();
			logClient = new LogClient(folder+"\\"+managerId+"\\",managerId);
			}
			catch(Exception ex)
			{
				System.out.println("Exception:"+ ex.getMessage());
			}
			
	}
	
	
	public String addItem(String managerId,String itemId,String itemName,int quantity)
	{
		logClient.logger.info("ManagerClient: Initiating Add Item");
		String result=serverInterface.addItem(managerId, itemId, itemName, quantity);
		logClient.logger.log(Level.INFO, result);
		return result;
	}
	
	public String listItemAvailability(String managerId)
	{
		logClient.logger.info("ManagerClient: Initiating listItemAvailability");	
		String output=serverInterface.listItemAvailability(managerId);
			logClient.logger.log(Level.INFO, output.toString());
		return output;
	}
	
	public String removeItem(String managerId,String itemId,int quantity)
	{
		logClient.logger.info("ManagerClient: Initiating Remove Item");
		String result=serverInterface.removeItem(managerId, itemId, quantity);
logClient.logger.log(Level.INFO, result);
		return result;
	}
}
