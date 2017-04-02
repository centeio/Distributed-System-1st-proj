import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

//STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
//GETCHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
//DELETE <Version> <SenderId> <FileId> <CRLF><CRLF>
//REMOVED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>


public class MC implements Runnable {
	private int port;
	private String mcast_addr;
	public Thread t;
	private MulticastSocket mcsocket;
	public Peer parent;


	public MC(String mcastaddr, String mcastport, Peer parent){
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
			mcsocket = new MulticastSocket(port);
			mcsocket.setTimeToLive(1);
			mcsocket.setSoTimeout(10000);	
			mcsocket.joinGroup(InetAddress.getByName(mcast_addr));
		}
		catch(IOException e){
			System.out.println("Try another address...");
			return;
		}
		
		while(true){
			
		try{
			byte[] rbuf = new byte[(int) Math.pow(2,16)];
			DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
			System.out.println("will receive packet in MC ");		
			mcsocket.receive(packet);
			
			//TODO alterar estado no ficheiro em causa	
			
			System.out.println("will receive packet in MC " + packet.getData());		

		}catch(IOException e){
			mcsocket.close();
			return;
		}
		System.out.println("ciclo mc");	
		}

	}	
	
}