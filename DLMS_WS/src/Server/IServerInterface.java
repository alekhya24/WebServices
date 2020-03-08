package Server;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;;

@WebService(name="IServerInterface")
@SOAPBinding(style = Style.RPC)
public interface IServerInterface {
	//Operations performed by Manager
	public String addItem(String managerID,String itemID,String itemName,int quantity);
	public String removeItem (String managerID,String itemID,int quantity) ;
	public String listItemAvailability (String managerID);
	
	//Operations performed by user
	public String borrowItem (String userID,String itemID,int isWaitlisted);
	public String findItem (String userID,String itemName);
	public String returnItem (String userID,String itemID);
	public String exchangeItem (String userID, String oldItemID, String newItemID);

}
