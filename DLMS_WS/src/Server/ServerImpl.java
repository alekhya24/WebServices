package Server;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import Model.Item;
import UDP.ServerUDP;
import UDP.UDPRequestProvider;
import Util.LogManager;
import Util.Servers;


//@SOAPBinding(style = SOAPBinding.Style.RPC)
@WebService(endpointInterface = "Server.IServerInterface", portName = "DlmsPort", serviceName = "DlmsService")
public class ServerImpl implements IServerInterface {

	public LogManager logManager;
	public ServerUDP serverUDP;
	public String IPaddress;
	public HashMap<String, Item> dataMap=new HashMap<>();
	public HashMap<String,List<String>> borrowMap=new HashMap<>();
	public HashMap<String,Queue<String>> waitingListMap=new HashMap<>();
	public String location;

	public ServerImpl() {
		super();
	}
	public ServerImpl(Servers libraryLocation) {
		super();
		location=libraryLocation.toString();
		logManager=new LogManager(libraryLocation.getserverName().toString().toUpperCase());
		serverUDP = new ServerUDP(libraryLocation, logManager.logger, this);
		serverUDP.start();
	}
	
	@Override
	public synchronized String addItem(String managerID, String itemId, String itemName, int quantity) {
		logManager.logger.info("Date & Time: " + LocalDateTime.now() +"; REQUEST TYPE:Add Item; REQUEST PARAMETERS: managerId:"+managerID+",itemId:"+itemId+",itemName:"+itemName+",quantity:"+quantity);
		String result="";
			Item item=new Item();
			Item existingItem=null;
			List<String> data=	dataMap.entrySet()
		              .stream()
		              .filter(entry -> Objects.equals(entry.getValue().ItemName, itemName))
		              .map(Map.Entry::getKey)
		              .collect(Collectors.toList());
			String originalItemId=data.stream()
					  .filter(s -> location.equals(s.substring(0, 3)))
					  .findAny()
					  .orElse(null);
			existingItem=originalItemId!=null?dataMap.get(originalItemId):null;
			if(existingItem!=null)
			{
				itemId=existingItem.ItemId;
			existingItem.setInStock(existingItem.InStock+quantity);
	        assignWaitlistingMembers(itemId);
			result="Item "+ existingItem.ItemName +" already exists.Updated existing item quantity.Updated quantity = "+existingItem.InStock;
			}
			else
			{
			String key =itemId;
			item.ItemId=itemId;
			item.ItemName=itemName;
			item.InStock=quantity;
			dataMap.put(key,item);
			result="Item is added " + item.ItemName + " with key: " + key;
			}
			logManager.logger.info("AddItem Server Response: "+result);
			logManager.logger.info("AddItem Request Successfully completed");
			return result.trim();
	}

	@Override
	public synchronized String removeItem (String managerID,String itemID,int quantity) 
	{
		logManager.logger.info("Date & Time: " + LocalDateTime.now() +"; REQUEST TYPE:Remove Item; REQUEST PARAMETERS: managerId:"+managerID+",itemId:"+itemID+",quantity:"+quantity);
		String result="";
		Item existingItem=dataMap.get(itemID);
		if(existingItem==null)
		{
			logManager.logger.info("Request Failed");
			result= "Item doesn't exist";
		}
		else
		{
		int existingItemQty=existingItem.getInStock();
		 if(quantity<0)
			{
				RemoveFromBorrowList(itemID);
				dataMap.remove(itemID);
				logManager.logger.info("RemoveItem Request Successfully completed");
				result= "Success";
			}
		 else if(quantity<=existingItemQty)
		{
		if(existingItemQty>0)
		{
			int qty=existingItemQty-quantity;
			existingItem.setInStock(qty);
			logManager.logger.info("RemoveItem Request Successfully completed");
			result= "Success";
		}
		else if(existingItemQty==0)
		{
			dataMap.remove(itemID);
			logManager.logger.info("RemoveItem Request Successfully completed");
			result= "Success";
		}
		}
		else
		{
			logManager.logger.info("RemoveItem Request Failed");
			result= "Quantity entered is incorrect";
		}
	}	
		return result.trim();
	}
	
	private void RemoveFromBorrowList(String itemID) 
	{
		ArrayList<String> userIds = new ArrayList<>();
		for (Entry<String, List<String>> entry : borrowMap.entrySet()) {
	        if (entry.getValue().contains(itemID)) {
	            userIds.add(entry.getKey());
	        }
	    }
		for (String id : userIds) {
			returnItem(id, itemID);
		}

	}
	@Override
	public synchronized String listItemAvailability(String managerID)  {
		logManager.logger.info("Date & Time: " + LocalDateTime.now() +"REQUEST TYPE:List Item Availability; REQUEST PARAMETERS: managerId:"+managerID);
		ArrayList<String> output=new ArrayList<>();
		Collection<Item> data=dataMap.values();
		for (Item item : data) {
			if(item.ItemId.substring(0,3).toString().equals(managerID.substring(0, 3).toString()))
			{
			output.add(item.ItemId+" "+item.ItemName+" "+ item.InStock);
			}
		}
        String finalData= (String)output.stream().collect(Collectors.joining(","));
        String result=finalData.equals(",")?"No records found":finalData.replaceAll(",*$", "");
		logManager.logger.info("ListItemAvailability ServerResponse : "+output.toString());
		logManager.logger.info("ListItemAvailability Request Successfully Completed");
		return result.trim();
	}
	
	private String getCurrentServerItemDetails(String itemName){
		String output = "";
		for (Map.Entry<String, Item> entry : this.dataMap.entrySet()) {
			if(entry.getValue().ItemName.equals(itemName))
			{
			output=entry.getKey()+" " +entry.getValue().InStock;
			}
		}
		return output;
	}
	
	@Override
	public synchronized String findItem (String userID,String itemName)
	{
		logManager.logger.info("Date & Time: " + LocalDateTime.now() +"REQUEST TYPE:find Item; REQUEST PARAMETERS: userId:"+userID+",itemName:"+itemName);
        ArrayList<String> recordCount =new ArrayList<>();
        UDPRequestProvider[] req = new UDPRequestProvider[2];
        int counter = 0;
        for (Servers loc : Servers.values()) {
            if (loc.toString()== this.location) {
            	if(getCurrentServerItemDetails(itemName)!="")
            	{
                recordCount.add(getCurrentServerItemDetails(itemName).trim());
            	}
            } else {
                try {
                	String data="findItem:"+userID+":"+itemName;
                	logManager.logger.info("FindItem Request redirecting through UDP to: "+ loc.toString());
                	req[counter] = new UDPRequestProvider(Server.serverData.get(loc.toString()),data);
                } catch (IOException e) {
                    logManager.logger.log(Level.SEVERE, e.getMessage());
                    logManager.logger.info("FindItem Request Failed");
                }
                req[counter].start();
                
                counter++;
            }
        }
        for (UDPRequestProvider request : req) {
            try {
                request.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(request.getReturnValue()!=null)
            {
            recordCount.add(request.getReturnValue().trim());
            }
        }
        String finalData= (String)recordCount.stream().collect(Collectors.joining(","));
        String result=finalData.equals(",")?"No records found":finalData.replaceAll(",*$", "");
        result=result.startsWith(",")?result.substring(1):result;
        logManager.logger.info("FindItem Server Response :"+ result);
        logManager.logger.info("FindItem Request Successfully Completed");
        
        return result.trim();
	}
	
	@Override
	public synchronized String returnItem (String userID,String itemID) 
	{
		logManager.logger.info("Date & Time: " + LocalDateTime.now() +"REQUEST TYPE:return Item; REQUEST PARAMETERS: userId:"+userID+",itemID:"+itemID);
		String result="";
		boolean isReturned=false;
        UDPRequestProvider[] req = new UDPRequestProvider[1];
        int counter = 0;
        for (Servers loc : Servers.values()) {
            if (loc.toString()== this.location && loc.toString().equals(itemID.substring(0, 3)) && !isReturned) {
        		isReturned=true;        			
            	if(borrowMap.containsKey(userID))
        		{
            		List<String> keyData=borrowMap.get(userID);
        			if(keyData.contains(itemID))
        			{
        				if(keyData.size()>1)
        			{
        				borrowMap.get(userID).remove(itemID);
        			}
        			else
        			{
        		borrowMap.remove(userID);
        			}
        		Item originalData=dataMap.get(itemID);
        		originalData.setInStock(++originalData.InStock);
        		result="Item returned successfully";
        			}
        			else
            		{
            			result="No such item borrowed.Please try again";
            		}
        		}
        		else
        		{
        			result="No such item borrowed.Please try again";
        		}
            	logManager.logger.info(result);
            }
            else
            {  
            if(!isReturned)
    		{
            	try {

            		if(loc.toString().equals(itemID.substring(0, 3)))
            		{
            			isReturned=true;
            			String data="returnItem:"+userID+":"+itemID;
            			logManager.logger.info("ReturnItem Request redirecting through UDP to: "+ loc.toString());
                	req[counter] = new UDPRequestProvider(Server.serverData.get(loc.toString()),data);
                    req[counter].start(); 
                    counter++;
            		}

                } catch (IOException e) {
                	
                    logManager.logger.log(Level.SEVERE, e.getMessage());
                    logManager.logger.info("ReturnItem Request Failed");
                }
            }
    		}
        }
            if(counter>0)
            {
            for (UDPRequestProvider request : req) {
                try {
                    request.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                result=request.getReturnValue();
            }
            }
        logManager.logger.info("ReturnItem Server Response:"+ result);
        logManager.logger.info("ReturnItem Request Successfully Completed");

        assignWaitlistingMembers(itemID);
		return result.trim();
	}
	
	private void assignWaitlistingMembers(String itemID) 
	{
		logManager.logger.info("Date & Time: " + LocalDateTime.now() +"REQUEST TYPE:Assign Waitlisting members; REQUEST PARAMETERS: itemID:"+itemID);
        UDPRequestProvider[] req = new UDPRequestProvider[1];
        String result="No waiting list members for "+ itemID;
        int counter=0;
	        for (Servers loc : Servers.values()) {
			if(loc.toString()==this.location)
        	{
			Queue<String> waitingListUsers= waitingListMap.get(itemID);
			if(waitingListUsers!=null)
			{
				String userId=waitingListUsers.poll();
				if(userId!=null)
				{
				Item existingItem=dataMap.get(itemID);
				int existingItemCount=existingItem.getInStock();
				if(existingItemCount>0)
				{
					existingItem.setInStock(--existingItemCount);
				List<String> existingItemList=borrowMap.get(userId);
				if(existingItemList==null)
				{
					List<String> itemList=new ArrayList<>();
					itemList.add(itemID);
					existingItemList=itemList;
				}
				else
				{
				existingItemList.add(itemID);
				}
				borrowMap.put(userId, existingItemList);
				result=userId+ " borrowed book "+itemID+" successfully";
			logManager.logger.info("AssignWaitingListmembers Request Successfully Completed");
				}
			}
			}
		}
		else
		{
			 if(loc.toString().equals(itemID.substring(0, 3)))
			 {
			 try {

				 Queue<String> waitingItemsList= Server.serverData.get(loc.toString()).waitingListMap.get(itemID);
				 if(waitingItemsList!=null)
				 {
             	String data="assignWaitListItem:"+itemID;
             	logManager.logger.info("AssignWaitingListmembers Request redirecting through UDP to: "+ loc.toString());
             	req[counter] = new UDPRequestProvider(Server.serverData.get(loc.toString()),data);
                req[counter].start();
                counter++;
				 }

             } catch (IOException e) {
                 logManager.logger.severe(e.getMessage());
             }
         }
		 }
	        }
	     	if(counter>0)
        	{
	        for (UDPRequestProvider request : req) {
                try {
                    request.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                result=request.getReturnValue();

            }
        	}
	     	logManager.logger.info("AssignWaitingListMembers Server Response: "+ result);
            System.out.println(result);
	}
	
	@Override
	public synchronized String borrowItem(String userID, String itemID,int isWaitlisted)  {
		String result="";
        UDPRequestProvider[] req = new UDPRequestProvider[1];
		logManager.logger.info("Date & Time: " + LocalDateTime.now() +"REQUEST TYPE:borrow Item; REQUEST PARAMETERS: userId:"+userID+",itemID:"+itemID);
        int counter = 0;
		if(isWaitlisted==0)
		{
		boolean isBorrowed=false;
        for (Servers loc : Servers.values()) {
        	if(loc.toString()==this.location)
        	{
		if(userID.substring(0, 3).equals(itemID.substring(0, 3)))
		{
			if(!checkIfUserAlreadyBorrowedSameItem(userID,itemID))
			{
			Item existingItem=dataMap.get(itemID);
			if(existingItem!=null)
			{
			int existingItemCount=existingItem.getInStock();

			if(existingItemCount>0)
			{
				isBorrowed=true;
				existingItem.InStock=--existingItemCount;
				List<String> existingItemList=borrowMap.get(userID);
				if(existingItemList==null)
				{
					List<String> itemList=new ArrayList<>();
					itemList.add(itemID);
					existingItemList=itemList;
				}
				else
				{
				existingItemList.add(itemID);
				}
				borrowMap.put(userID, existingItemList);
				result=userID + " borrowed "+ itemID+ " successfully";
			}
			else
			{
				result="Failed.Try adding to waiting list";
			}}
			else
			{
				result="No Item exists with item Id: "+itemID;
			}
			}
			else
			{
				result="User already borrowed the item.";
			}
			
		}
        	}
		else
		{
			 if(!isBorrowed)
			 {
					if(loc.toString().equals(itemID.substring(0, 3)))
            		{
			 try {

							isBorrowed=true;
             	String data="borrowItem:"+userID+":"+itemID;
             	logManager.logger.info("BorrowItem Request redirecting through UDP to: "+ loc.toString());
             	req[counter] = new UDPRequestProvider(Server.serverData.get(loc.toString()),data);
				 }

              catch (IOException e) {
                 logManager.logger.log(Level.SEVERE, e.getMessage());
             }
             req[counter].start();
             counter++;
         }
			 }
		}
		}
        	if(counter>0)
        	{
        	   for (UDPRequestProvider request : req) {
                   try {
                       request.join();
                   } catch (InterruptedException e) {
                       e.printStackTrace();
                   }
                  result=request.getReturnValue();//request.getReturnValue();
               }
        	}
        	logManager.logger.info("BorrowItem Server Response:"+result);
        	logManager.logger.info("BorrowItem Request Successfully Completed");
		}
		else
		{
		result=AddToWaitingList(userID,itemID);
		}
		return result.trim();
	}

	private boolean checkIfUserAlreadyBorrowedSameItem(String userId,String itemId)
	{
		boolean result = false;
		List<String> borrowedItems =borrowMap.get(userId);
		if(borrowedItems!=null && borrowedItems.contains(itemId))
		{
			result=true;
		}
		return result;
	}
	
	
	private String AddToWaitingList(String userID,String itemID)
	{
		logManager.logger.info("Date & Time: " + LocalDateTime.now() +"REQUEST TYPE:borrow Item; REQUEST PARAMETERS: userId:"+userID+",itemID:"+itemID);
		String result="";
        UDPRequestProvider[] req = new UDPRequestProvider[1];
        int counter=0;
		
		for (Servers loc : Servers.values()) {
        	if(loc.toString()==this.location)
        	{
		if(userID.substring(0, 3).equals(itemID.substring(0, 3)))
		{
			Queue<String> waitingListQueue=waitingListMap.get(itemID);
		if(waitingListQueue==null)
		{
			Queue<String> itemQueue=new LinkedList<>();
			itemQueue.add(userID);
			waitingListQueue=itemQueue;
		}
		else
		{
		waitingListQueue.add(userID);
		}	
		waitingListMap.put(itemID, waitingListQueue);
		result="user "+userID+ " is added to the waiting list";
		}
    		}
        	else
        	{
				if(loc.toString().equals(itemID.substring(0, 3)))
        		{
        		 try {
        				logManager.logger.info("AddToWaitingList Request redirecting through UDP to: "+ loc.toString());
          	String data="addToWaitingList:"+userID+":"+itemID;
          	
          	req[counter] = new UDPRequestProvider(Server.serverData.get(loc.toString()),data);
				 }

           catch (IOException e) {
              logManager.logger.log(Level.SEVERE, e.getMessage());
          }
          req[counter].start();
          counter++;
        	}
            }
		 }
		if(counter>0)
    	{
    	   for (UDPRequestProvider request : req) {
               try {
                   request.join();
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
              result=request.getReturnValue();
           }
    	}
		logManager.logger.info("AddToWaitingList Server Response:"+result);
    	logManager.logger.info("AddToWaitingList Request Successfully Completed");
		return result;
	}	
	
	
	@Override
	public synchronized String exchangeItem(String userID, String oldItemId,String newItemId)  {
		String result="";
		String response1="";
		String response2="";
		logManager.logger.info("Date & Time: " + LocalDateTime.now() +"REQUEST TYPE:exchange Item; REQUEST PARAMETERS: userId:"+userID+",olditemID:"+oldItemId+",newItemId:"+newItemId);

		boolean userBorrowed = checkIfUserBorrowed(userID,oldItemId);
		boolean itemExists = checkIfItemExists(newItemId);
if(userBorrowed && itemExists)
{
	 response2 = returnItem(userID, oldItemId);
	System.out.println(response2);
	 response1 = borrowItem(userID, newItemId, 0);
	System.out.println(response1);
}
String failureResponse="";
if(!userBorrowed)
{
	failureResponse+="User did not borrowed the old book";
}
if(!itemExists)
{
	failureResponse+="Item isn't available";
}
result=response1!=""&&response2!=""?response1 +"\n"+response2:failureResponse.trim();
        	logManager.logger.info("ExchangeItem Server Response:"+result);
        	logManager.logger.info("ExchangeItem Request Successfully Completed");
		return result.trim();
	}
	
	private boolean checkIfItemExists(String itemId)
	{
		boolean result = false;
		 UDPRequestProvider[] req = new UDPRequestProvider[1];
		 int counter=0;
    	   for (Servers loc : Servers.values()) {
			if(loc.toString()==this.location)
			{
				if(loc.toString().equals(itemId.substring(0, 3)))
				{
					Item existingItem = dataMap.get(itemId);
					if(existingItem!=null)
					{
						int count=existingItem.InStock;
						if(count>0)
						{
							result=true;
						}
					}
				}
			}
				else
				{
					if(loc.toString().equals(itemId.substring(0, 3)))
	        		{
	        		 try {
	        				logManager.logger.info("ExchangeItem CheckIfItemExists Request redirecting through UDP to: "+ loc.toString());
	          	String data="checkIfItemExists:"+itemId;
	          	
	          	req[counter] = new UDPRequestProvider(Server.serverData.get(loc.toString()),data);
					 }

	           catch (IOException e) {
	              logManager.logger.log(Level.SEVERE, e.getMessage());
	          }
	          req[counter].start();
	          counter++;
	        	}	
				}
       }
       String op="";
			if(counter>0)
	    	{
	    	   for (UDPRequestProvider request : req) {
	               try {
	                   request.join();
	               } catch (InterruptedException e) {
	                   e.printStackTrace();
	               }
	             op=request.getReturnValue();
	           }
	    	}
            if(op.trim().equals("Success"))
            {
          	  result=true;
            }
			logManager.logger.info("ExchangeItem CheckIfItemExists Server Response:"+result);
	    	logManager.logger.info("ExchangeItem CheckIfItemExists Request Successfully Completed");
		return result;
	}
	
	private boolean checkIfUserBorrowed(String userId,String itemId)
	{
		boolean result = false;
		 UDPRequestProvider[] req = new UDPRequestProvider[1];
		 int counter=0;
        for (Servers loc : Servers.values()) {
			if(loc.toString()==this.location)
			{
				if(userId.substring(0, 3).equals(itemId.substring(0, 3)))
				{
					List<String> borrowedItems =borrowMap.get(userId);
					if(borrowedItems!=null && borrowedItems.contains(itemId))
					{
						result=true;
					}
				}
			}
			else
			{
				if(loc.toString().equals(itemId.substring(0, 3)))
        		{
        		 try {
        				logManager.logger.info("ExchangeItem CheckIfUserBorrowed Request redirecting through UDP to: "+ loc.toString());
          	String data="checkIfUserBorrowed:"+userId+":"+itemId;
          	
          	req[counter] = new UDPRequestProvider(Server.serverData.get(loc.toString()),data);
				 }

           catch (IOException e) {
              logManager.logger.log(Level.SEVERE, e.getMessage());
          }
          req[counter].start();
          counter++;
        	}	
			}
		}
        String op="";
		if(counter>0)
    	{
    	   for (UDPRequestProvider request : req) {
               try {
                   request.join();
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
            op=request.getReturnValue();
           }
    	}
        if(op.trim().equals("Success"))
        {
      	  result=true;
        }
		logManager.logger.info("ExchangeItem CheckIfUserBorrowed Server Response:"+result);
    	logManager.logger.info("ExchangeItem CheckIfUserBorrowed Request Successfully Completed");
		return result;
	}

}
