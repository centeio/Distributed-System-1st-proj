import java.util.Scanner;

public class PeerProcess implements Runnable{

	@Override
	public void run() {
		Scanner scan = new Scanner(System.in);
		String request = scan.next();
		
		System.out.println("Request: " + request);
		
	}
	
	

}
