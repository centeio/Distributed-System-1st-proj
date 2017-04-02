import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

//PUTCHUNK <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>


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
			mcsocket = new MulticastSocket(port);
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
		
		MulticastSocket mc;
		try {
			mc = new MulticastSocket();

		while(true){
			
		try{
			System.out.println("will receive packet in MDB");		
			mcsocket.receive(packet);
			
			String message = new String(packet.getData(), "UTF_8");
			
			//PUTCHUNK <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
			String[] parts = message.split("[ ]+\\r\\n\\r\\n"); //<CRLF><CRLF>
			String[] header = parts[0].split("[ ]+");
			byte[] chunkData = parts[1].getBytes();
			
			//Parse header
			String messageType = header[0];
			String version = header[1];
			int senderId = Integer.parseInt(header[2]);
			String fileId = header[3];
			int chunkNo = Integer.parseInt(header[4]);
			int replicationDeg = Integer.parseInt(header[5]);
			
			//Testing if parser working
			System.out.println("MessageType: " + messageType + "*");
			System.out.println("Version: " + version + "*");
			System.out.println("SenderId: " + senderId + "*");
			System.out.println("FileId: " + fileId + "*");
			System.out.println("chunkNo: " + chunkNo + "*");
			System.out.println("ReplicationDeg: " + replicationDeg + "*");
			
			//check if parent is the sender
			if(senderId != parent.getId()){
				/*
				 * Saves information of the chunk onto a new file name:
				 * filename.partX, being x a number.
				 * Eg: text.part1 - text.part2 - text.part3
				 * 
				 * TODO: check chunkName
				 */
				/*String chunkName = header[3] + ".part" + header[4];
	
				File output = new File(chunkName);
				FileOutputStream chunk = new FileOutputStream(output);
				chunk.write(chunkData);
				chunk.flush();
				chunk.close();*/			
				
				message = "STORED";
				
				long randomTime = (0 + (int)(Math.random() * 4))*1000;
				Thread.sleep(randomTime);
				
				InetAddress address = InetAddress.getByName(parent.mc.getMcast_addr());
				packet = new DatagramPacket(message.getBytes(), message.toString().length(), address, parent.mc.getPort());
				System.out.println("sends STORED to " + parent.mc.getMcast_addr() + " port " + parent.mc.getPort());
	
				mc.send(packet);
				mc.close();
			}
			
		}catch(IOException e){
			mcsocket.close();
			return;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("ciclo");	
		}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		

	}	
	
}