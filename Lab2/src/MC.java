import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Hashtable;

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
		this.port = Integer.parseInt(mcastport);
		this.mcast_addr = mcastaddr;
		this.t = new Thread(this);
		this.t.start();
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
			System.out.println("Ready to receive packet in MC");

			mcsocket = new MulticastSocket(port);
			mcsocket.setTimeToLive(1);	
			mcsocket.joinGroup(InetAddress.getByName(mcast_addr));
		
			while(true){
				byte[] rbuf = new byte[(int) Math.pow(2,16)];
				DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
			
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
				
				switch(type){
					case "STORED":
						chunkNo = Integer.parseInt(parts[4]);
							
						Backup b = this.parent.getChunk(this.parent.getBackups(), fileId, chunkNo);
						//b.setStoredMessages(b.getStoredMessages() + 1);
						this.parent.chunkStored(fileId, chunkNo, senderId);
						
						break;
					case "GETCHUNK":
						//

						chunkNo = Integer.parseInt(parts[4]);
						if(this.parent.getId() != senderId){
							if(this.parent.protocols.get(fileId) != null){
								for(Backup br : this.parent.protocols.get(fileId)){
									if(br.getChunkNo() == chunkNo){
										Restore r = new Restore(senderId, br.getChunk(), fileId, chunkNo, Restore.State.SENDCHUNK);
										this.parent.queue.add(r);
									}
								}
							}
						}
								
						break;
					case "DELETE":
						if(senderId != parent.getId())
							this.parent.queue.add(new Delete(fileId));
						break;
					case "REMOVED":
						chunkNo = Integer.parseInt(parts[4]);	
						System.out.println("Chunk number " + chunkNo + " removed");
						if(senderId != this.parent.getId()){
							this.parent.chunkRemoved(fileId, chunkNo, senderId);
						}
						
						break;
				}
			}
		}catch(IOException e){
			mcsocket.close();
			return;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}