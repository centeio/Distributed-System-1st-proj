import java.io.IOException;
import java.net.*;

public class Client {
	public static void main(String[] args) throws IOException{
		
		if(args.length != 2){
			System.out.println("Usage: Client <mcast_addr> <mcast_port>");
			return;
		}		
		
		MulticastSocket mcsocket;

		try{	
			mcsocket = new MulticastSocket(Integer.parseInt(args[1]));
			mcsocket.setTimeToLive(1);
			mcsocket.setSoTimeout(10000);	
			mcsocket.joinGroup(InetAddress.getByName(args[0]));
		}
		catch(SocketException e){
			System.out.println("Try another address...");
			return;
		}
		
		byte[] rbuf = new byte[(int) Math.pow(2,16)];
		DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
		
		Thread peerp = new Thread(new PeerProcess());
		peerp.start();
		
		while(true){
			
		try{
			System.out.println("will receive packet");		
			mcsocket.receive(packet);
		}catch(SocketTimeoutException e){
			mcsocket.close();
			return;
		}
		System.out.println("will leave group");	
		}

			
	}
}
