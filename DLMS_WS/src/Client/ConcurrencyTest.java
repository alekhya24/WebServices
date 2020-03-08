package Client;

import Util.Servers;

public class ConcurrencyTest {
	public static void main(String[] args) {
		String op1="";
		String op2="";
		UserClientImpl usrClient = new UserClientImpl(Servers.CON, "CONU1234");
		ManagerClientImpl mngrClient = new ManagerClientImpl(Servers.CON, "CONM1234");
		
        Runnable client1 = () -> {
            try {
            	String op= mngrClient.addItem("CONM1234", "CON3333", "Distributed", 1);
            	System.out.println(op);
            	//usrClient.findItem("CONU1234", "ADB");
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        Thread t1 = new Thread(client1);
        
        Runnable client2 = () -> {
            try {
           String op= usrClient.borrowItem("CONU1234", "CON3333", 0);
           System.out.println(op);
             //usrClient.borrowItem("MONU1234", "CON2222", 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        Thread t2 = new Thread(client2);
        Runnable client3 = () -> {
            try {
           String op= usrClient.returnItem("CONU1234", "CON3333");
           System.out.println(op);
             //usrClient.borrowItem("MONU1234", "CON2222", 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        Thread t3 = new Thread(client3);
        t1.start();
        t2.start();
        t3.start();
	}

}
