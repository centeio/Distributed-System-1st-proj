import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Peer implements PeerObj {
	private String id;
	private Registry registry;
		
	public String getId() {	return id;}
	public void setId(String id) {this.id = id;}
	public Registry getRegistry() {return registry;}
	
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}
	
	public Peer(String id) throws RemoteException {
		super();
		this.id = id;
	    PeerObj stub = (PeerObj) UnicastRemoteObject.exportObject(this, 0);

	    // Bind the remote object's stub in the registry
	    this.registry = LocateRegistry.getRegistry();
	    this.registry.rebind(this.id, stub);
	}
	
	public static void main(String[] args) throws IOException{
		
		if(args.length != 3){
			System.out.println("Usage: Peer <mcast_addr> <mcast_port> <name>");
			return;
		}		
		
	    Peer obj = new Peer(args[2]);	    
		
		Thread mc = new Thread(new MC(args[0], args[1]));
		Thread mdb = new Thread(new MDB(args[0], args[1]));
		Thread mdr = new Thread(new MDR(args[0], args[1]));

		mc.start();
		mdb.start();
		mdr.start();
					
	}
	
	@Override
	public String initOp(String operation, String file) throws RemoteException { //Restore and delete
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String initOp(String operation, int space) throws RemoteException { //Reclaim
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String initOp(String operation, String file, int repdegree) throws RemoteException { //Backup
		// TODO Auto-generated method stub
		return null;
	}




}
