import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Operator implements Runnable{
	
	private Peer peer;
	private static ArrayList<byte[]> chunks;
	private int currChunk;
	
	
	public Operator(Peer peer) {
		super();
		this.peer = peer;
		this.currChunk = 0;
	}
	
	public static void divideFileIntoChunks(File file){
		try{
			chunks = new ArrayList<byte[]>();
			
			FileInputStream stream = new FileInputStream(file);
			MulticastSocket socket = new MulticastSocket();

			byte[] chunkData;
			long filelength = file.length();
			int chunkMaxSize = 1024 * 64;
			int readLength = chunkMaxSize;
			
			while(filelength > 0){
				if(filelength < chunkMaxSize){
					readLength = (int)filelength;
				}
				
				chunkData = new byte[readLength];
				
				int bytesRead = stream.read(chunkData, 0, readLength);
				filelength -= bytesRead;
				
				if(chunkData.length != bytesRead){
					System.out.println("Error reading chunk");
					break;
				}
				chunks.add(chunkData);
				
				chunkData = null;
			}
			
			stream.close();
			stream = null;
			socket.close();
		}catch(FileNotFoundException e){
			System.out.println("File " + file.getName() + " not found");
			return;
		}catch(SecurityException e){
			System.out.println("Denied reading file " + file.getName());
			return;
		} catch (IOException e) {
			System.out.println("Error closing stream of file " + file.getName());
			return;
		}
	}
	
	/**
	 * Encodes a given String according to the MessageDigest SHA-256 algorithm
	 * 
	 * @param base String to encode
	 * @return Encoded String
	 */
	static public String sha256(String base) {
	    try{
	        MessageDigest digest = MessageDigest.getInstance("SHA-256");
	        byte[] hash = digest.digest(base.getBytes("UTF-8"));
	        StringBuffer hexString = new StringBuffer();

	        for (int i = 0; i < hash.length; i++) {
	            String hex = Integer.toHexString(0xff & hash[i]);
	            if(hex.length() == 1) hexString.append('0');
	            hexString.append(hex);
	        }

	        return hexString.toString();
	    } catch(Exception ex){
	       throw new RuntimeException(ex);
	    }
	}

	@Override
	public void run() {
		//TODO access peer's blocking queue		
		while(true){
			try {
				Object protocol = peer.queue.take();
				
				//work 
				if(protocol instanceof Delete){
					Delete del = (Delete) protocol;
					
					if(del.state == Delete.State.DELETEFILE){
						String message = del.getMessage();
						MulticastSocket socket = new MulticastSocket();
						InetAddress address = InetAddress.getByName(peer.mc.getMcast_addr());
						DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), address, peer.mc.getPort());
						socket.send(packet);
						
						System.out.println("Sending DELETE message to: \n\t\taddress:" + peer.mc.getMcast_addr() + "\n\t\tport: " + peer.mc.getPort());
						
						del.updateState();
					}
					else if(del.state == Delete.State.DELETECHUNKS ){
						final File folder = peer.getDirectory();
						final String filename = del.getFileId();
						final File[] files = folder.listFiles( new FilenameFilter(){

							@Override
							public boolean accept(File dir, String name) {
								return name.matches( filename + ".*" );
							}
							
						});
						for ( final File file : files ) {
						    if ( !file.delete() ) {
						        System.err.println( "Can't remove " + file.getAbsolutePath() );
						    }
						}
						del.updateState();
					}
				}else if(protocol instanceof Backup){
					Backup bkup = (Backup) protocol;

					if(bkup.state == Backup.State.SENDCHUNK){
						String message_header = bkup.getPutchunk(currChunk + 1);
						byte[] message_header_bytes = message_header.getBytes();
						byte[] message_body = chunks.get(currChunk);
						byte[] putchunk = new byte[message_header_bytes.length + message_body.length];
						
						for(int i = 0; i < putchunk.length; i++){
							if(i < message_header_bytes.length){
								putchunk[i] = message_header_bytes[i];
							}else{
								putchunk[i] = message_body[i-message_header_bytes.length];
							}
						}
						
						MulticastSocket socket = new MulticastSocket();
						InetAddress address = InetAddress.getByName(this.peer.mdb.getMcast_addr());
						DatagramPacket packet = new DatagramPacket(putchunk, putchunk.length, address, this.peer.mdb.getPort());
						socket.send(packet);
						
						System.out.println("Sending PUTCHUNK message");
						System.out.println("Waiting STORED");
						
						int timeout = 1000;
						int numMessages = bkup.getReplication_degree();
						int actualtries = 0;
						
						Thread.sleep(timeout);
						while(this.peer.mc.receivedStored < numMessages && actualtries < 5){
							System.out.println("Retransmiting PUTCHUNK...");
							this.peer.mc.receivedStored = 0;
							actualtries++;
							socket.send(packet);
							timeout = timeout * 2;
							Thread.sleep(timeout);
						}
						socket.close();
						
						System.out.println("Received all STORED messages");
						bkup.setState(Backup.State.DONE);
						
						this.peer.queue.put(protocol);
					}else if(bkup.state == Backup.State.SAVECHUNK){
						byte[] chunkData = bkup.getChunk();
						File output = new File("../peers/" + this.peer.getId() + "/" + bkup.getFileId() + "." + (currChunk + 1));
						if(!output.exists()){
							FileOutputStream chunk = new FileOutputStream(output);
							chunk.write(chunkData);
							chunk.flush();
							chunk.close();
							//aumenta rep_degree
					
							String message = bkup.getStored(currChunk + 1);
							MulticastSocket socket = new MulticastSocket();
							InetAddress address = InetAddress.getByName(this.peer.mc.getMcast_addr());
							DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), address, this.peer.mc.getPort());
							
							long randomTime = (0 + (int)(Math.random() * 4))*100;
							Thread.sleep(randomTime);
							
							socket.send(packet);
							
							System.out.println("Sending STORED message");
							
							socket.close();
						}
					}else if(bkup.state == Backup.State.RECEIVESTORED){
						System.out.println("Received STORED message from " + bkup.getSenderId());

						if(bkup.getChunk() != null){
							if(currChunk < chunks.size()){
								currChunk++;
								bkup.setState(Backup.State.SENDCHUNK);
							}else{
								currChunk = 0;
								bkup.setState(Backup.State.DONE);
							}
						}
						
					}else if(bkup.state == Backup.State.DONE){
						System.out.println("Done backing up");
					}
				}
				
				//if not done
				//this.peer.queue.put(protocol);
				//else
				//peer.protocols.put(id, protocol);
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void receiveStored(){
		
	}
}
