import java.io.IOException;
import java.net.*;

public class Client {
	public static void main(String[] args) throws IOException{
		
		if(args.length < 4 || args.length > 5){
			System.out.println("Usage: Client <mcast_addr> <mcast_port> <oper> <opnd> * ");
			return;
		}		
		
		String sbuf = "";
		if(args[2].trim().equalsIgnoreCase("register")){
			if(args.length == 5)
				sbuf = args[2].trim() + ":" + args[3].trim() + ":" + args[4].trim();
			else
				System.out.println("Usage: REGISTER <plate number> <owner name>");
		}
		else if(args[2].trim().equalsIgnoreCase("lookup")){
			if(args.length == 4)
				sbuf = args[2].trim() + ":" + args[3].trim();
			else
				System.out.println("Usage: LOOKUP <plate number>");
		}
		else{
			System.out.println("Usage: REGISTER <plate number> <owner name> or LOOKUP <plate number>");
			return;
		}

		
		//Read advertisement (multicast: <mcast_addr> <mcast_port>: <srvc_addr> <srvc_port>)
		
		MulticastSocket mcsocket = new MulticastSocket(Integer.parseInt(args[1]));
		mcsocket.setSoTimeout(10000);
		mcsocket.joinGroup(InetAddress.getByName(args[0]));
		
		byte[] rbuf = new byte[(int) Math.pow(2,16)];
		DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
		
		try{
			System.out.println("will receive packet");		

			mcsocket.receive(packet);
		}catch(SocketTimeoutException e){
			mcsocket.close();
			return;
		}
		System.out.println("will leave group");		

		mcsocket.leaveGroup(InetAddress.getByName(args[0]));
		System.out.println("leaves group");		

		mcsocket.close();
		System.out.println("closes msocket");		

		
		String received = (new String(packet.getData())).trim();
		
		String[] info = received.split(":");
		
		int port = Integer.parseInt(info[3]);
		InetAddress serverAddress = packet.getAddress();

		// send request		
		packet = new DatagramPacket(sbuf.getBytes(), sbuf.length(), serverAddress, port);
		DatagramSocket socket = new DatagramSocket(port);
		System.out.println("will send packet");		

		socket.send(packet);
		
		System.out.println("packet sent");		

		//receive response
		rbuf = new byte[(int) Math.pow(2,16)];
		packet = new DatagramPacket(rbuf, rbuf.length);
		socket.receive(packet);
		
		// display response
		String answer = (new String(packet.getData())).trim();
		System.out.println("Echoed Message: " + answer);
		socket.close();
		
			
	}
}
