import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MDB implements Runnable {
	private int port;
	private String mcast_addr;
	public Thread t;
	private MulticastSocket mcsocket;
	public Peer parent;

	public MDB(String mcastaddr, String mcastport, Peer parent){
		super();
		this.parent = parent;
		port = Integer.parseInt(mcastport);
		mcast_addr = mcastaddr;
		t = new Thread(this);
		t.start();

	}
				
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getMcast_addr() {
		return mcast_addr;
	}

	public void setMcast_addr(String mcast_addr) {
		this.mcast_addr = mcast_addr;
	}

	public Thread getT() {
		return t;
	}

	public void setT(Thread t) {
		this.t = t;
	}

	public MulticastSocket getMcsocket() {
		return mcsocket;
	}

	public void setMcsocket(MulticastSocket mcsocket) {
		this.mcsocket = mcsocket;
	}	
	
	@Override
	public void run() {
		try{
			System.out.println("Ready to receive packet in MDB");
			
			mcsocket = new MulticastSocket(port);
			mcsocket.setTimeToLive(1);
			mcsocket.joinGroup(InetAddress.getByName(mcast_addr));
			
			while(true){	
				byte[] rbuf = new byte[(int) Math.pow(2,16)];
				DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
				
				mcsocket.receive(packet);
							
				String data = new String(packet.getData(), "ISO-8859-1");
								
				String[] data_split = data.split(" \\r\\n\\r\\n");
				
				String[] header = data_split[0].split(" ");
				byte[] body = data_split[1].trim().getBytes("ISO-8859-1");
										
				String messageType = header[0];
				String version = header[1];
				int senderId = Integer.parseInt(header[2]); //peer initiator
				String fileId = header[3];
				int chunkNo = Integer.parseInt(header[4]);
				int replicationDeg = Integer.parseInt(header[5]);
				
				/**
				 * Se nao for o initiator, cria um novo backup e adiciona a queue
				 */
				if(this.parent.getId() != senderId){
					System.out.println("Received PUTCHUNK from peer " + senderId);
					Backup b = new Backup(fileId, body, chunkNo, this.parent.getId(), replicationDeg, Backup.State.SAVECHUNK);
					
					//b.setPeerInitiator(peerInitiator);
					this.parent.queue.add(b);
				}	
			}
		}
		catch(IOException e){
			System.out.println("Try another address...");
			return;
		}
	}
}