import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class Client {

	public static void main(String[] args) throws IOException{
		
		if(args.length != 4){
			if(args.length != 3){
				System.out.println("Usage: Client <peer_ap> <operation> <opnd_1> <rep degree>");
				return;
			}
		}
		else{
			if(!args[1].toLowerCase().equals("backup"))
				return;
		}
		
		String peerID = args[0];
		String operation = args[1].toLowerCase();
		String file = args[2];

		try {
			
		    Registry registry = LocateRegistry.getRegistry(null);
		    PeerObj stub = (PeerObj) registry.lookup(peerID);

			switch(operation){
			case "backup":
				int rep = Integer.parseInt(args[3]);
				stub.backup(file, rep);
				
				break;
			case "reclaim":
				int space = Integer.parseInt(args[2]);
				stub.reclaim(space);
				
				break;
			case "delete":
				stub.delete(file);
				
				break;
			case "restore":
				stub.restore(file);
				
				break;
			default:
				System.out.println("Operation not supported");
				
				return;
			}
		    System.out.println("response: ");
		} catch (Exception exc) {
		    System.err.println("Client exception: ID not found " + peerID);
		    return;
		}
			

	}
			
	
}
