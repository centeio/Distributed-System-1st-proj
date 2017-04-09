import java.io.IOException;
import java.net.*;

public class UnicastServer implements Runnable { 
	private int port;
	
	public UnicastServer(int port) {
		super();
		this.port = port;
	}


	public void run(){

		DatagramSocket socket;
		byte[] rbuf = new byte[(int) Math.pow(2,16)];
		DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);

		try {
			socket = new DatagramSocket(this.port);
			System.out.println("main server will receive packet");		
			socket.receive(packet);
			System.out.println("main server finishes receiving packet");
		} catch (SocketException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;

		}
		
		

		String received = (new String(packet.getData())).trim();
		
		//tratar dados e fazer response
		System.out.println(received);

		String[] req = received.split(":");
		
		
		if(req[0].equalsIgnoreCase("register")){
			String sbuf1 = "register received";
			packet.setData(sbuf1.getBytes());
		}		
		else if(req[0].equalsIgnoreCase("lookup")){
			String sbuf2 = "lookup received";
			packet.setData(sbuf2.getBytes());
		}
		else {
			String sbuf3 = "ERROR";
			packet.setData(sbuf3.getBytes());
		}
		
		//response
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
			socket.close();
			return;
		}
		
		socket.close();
		
	}

/*	private static String lookup(ArrayList<Vehicle> db, String plate) {
		String ret = "NOT_FOUND";
		int i = db.indexOf(plate);		
		if(i >= 0)
			ret = db.get(db.indexOf(plate)).getPlate();	
		
		return ret;
	} 

	private static int register(ArrayList<Vehicle> db, String plate, String name) {
		if(db.contains(plate)){
			return -1;
		}
		
		db.add(new Vehicle(plate, name));
		return db.size();
		
	}*/
}