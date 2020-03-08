package Client;

import java.io.File;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import Server.IServerInterface;
import Util.Constants;
import Util.LogClient;
import Util.Servers;

public class UserClientImpl {
	Util.LogClient logClient = null;
	static IServerInterface serverInterface;
	URL url;
	Service service;
	UserClientImpl(Servers server, String UserId)
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
		boolean usrID = new File(Constants.LOG_DIR+folder+"\\"+UserId).mkdir();
		logClient = new LogClient(folder+"\\"+UserId+"\\",UserId);
	}
	catch(Exception ex)
	{
		System.out.println("Exception:"+ ex.getMessage());
	}
	}
	
	public String borrowItem(String userId, String itemId,int isWaitlisted)
	{
		logClient.logger.info("UserClient: Initiating Borrow Item");
		String result = serverInterface.borrowItem(userId, itemId,isWaitlisted);
		logClient.logger.info("Success");
		return result;
	}
	
	public String findItem(String userId,String itemName)
	{
		logClient.logger.info("UserClient: Initiating Find Item");
		String result=serverInterface.findItem(userId, itemName);
		logClient.logger.info(result);
		return result;
	}
	
	public String returnItem(String userId,String itemId)
	{
		logClient.logger.info("UserClient: Initiating Return Item");
		String result=serverInterface.returnItem(userId, itemId);
		logClient.logger.info(result);
		return result;
	}
	
	public String exchangeItem(String userId,String oldItemId,String newItemId)
	{
		logClient.logger.info("UserClient: Initiating Exchange Item");
		String result=serverInterface.exchangeItem(userId, oldItemId, newItemId);
		logClient.logger.info(result);
		return result;
	}
}
