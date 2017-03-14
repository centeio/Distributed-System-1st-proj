import java.io.IOException;


public class Server {
	public static void main(String[] args) throws IOException{
		
		if(args.length != 3){
			System.out.println("Usage:  Server <srvc_port> <mcast_addr> <mcast_port>");
			return;
		}
		
		Thread unicast = new Thread(new UnicastServer(Integer.parseInt(args[0])));
		unicast.start();
		System.out.println("finishes unicast");		
		
		Thread multicast = new Thread(new MulticastServer(args[0], args[1], args[2]));
		multicast.start();
		System.out.println("finishes multicast");
		

	}
}