import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

//PUTCHUNK <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>


public class MDB implements Runnable {
	private int mcast_port;
	private String mcast_addr;

	public MDB(String mcastport, String mcastaddr){
		super();
		this.mcast_port = Integer.parseInt(mcastport);
		this.mcast_addr = mcastaddr;
	}
	
	@Override
	public void run() {
		MulticastSocket mcsocket;

		try{	
			mcsocket = new MulticastSocket(mcast_port);
			mcsocket.setTimeToLive(1);
			mcsocket.setSoTimeout(10000);	
			mcsocket.joinGroup(InetAddress.getByName(mcast_addr));
		}
		catch(IOException e){
			System.out.println("Try another address...");
			return;
		}
		
		byte[] rbuf = new byte[(int) Math.pow(2,16)];
		DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
		
		while(true){
			
		try{
			System.out.println("will receive packet in MDB");		
			mcsocket.receive(packet);
		}catch(IOException e){
			mcsocket.close();
			return;
		}
		System.out.println("will leave group");	
		}

	}	
	
}