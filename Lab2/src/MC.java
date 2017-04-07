import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

//STORED	<Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
//GETCHUNK	<Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
//DELETE	<Version> <SenderId> <FileId> <CRLF><CRLF>
//REMOVED	<Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
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
				System.out.println("will receive packet in MC ");		
				mcsocket.receive(packet);
				
				String message = new String(packet.getData(), "UTF-8");
				message = message.trim();

				String[] parts = message.split(" "); //<CRLF><CRLF>
				
				//Parse message
				String type = parts[0];
				String version = parts[1];
				int senderId = Integer.parseInt(parts[2]);
				String fileId = parts[3];
				int chunkNo;
				
				if(!type.equals("DELETE")){
					chunkNo = Integer.parseInt(parts[4]);
				}
				
				System.out.println(fileId);
				System.out.println("Received " + type + " message from: \n\t\taddress:" + mcast_addr + "\n\t\tport: " + port);
				
				if(senderId != this.parent.getId()){
					switch(type){
					case "STORED":
						//TODO alterar estado no ficheiro em causa
						break;
					case "GETCHUNK":
						break;
					case "DELETE":
						this.parent.queue.add(new Delete(fileId));
						break;
					case "REMOVED":
						break;
					}
				}
			}catch(IOException e){
				mcsocket.close();
				return;
			}
		}

	}	
	
}