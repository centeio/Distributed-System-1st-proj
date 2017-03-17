import java.rmi.RemoteException;
import java.util.Scanner;

public class PeerInitiator implements Runnable {

	public PeerInitiator() throws RemoteException {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		Scanner scan = new Scanner(System.in);
		String request = scan.next();
		
		System.out.println("Request: " + request);
		
	}
	
	

}
