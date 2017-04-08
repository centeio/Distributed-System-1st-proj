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

	public Operator(Peer peer) {
		super();
		this.peer = peer;
	}

	
	static public ArrayList<byte[]> divideFileIntoChunks(File file){
		try{
			ArrayList<byte[]> chunks = new ArrayList<byte[]>();
			
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
			
			return chunks;
		}catch(FileNotFoundException e){
			System.out.println("File " + file.getName() + " not found");
			return null;
		}catch(SecurityException e){
			System.out.println("Denied reading file " + file.getName());
			return null;
		} catch (IOException e) {
			System.out.println("Error closing stream of file " + file.getName());
			return null;
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
						String message_header = bkup.getPutchunk();
						byte[] message_header_bytes = message_header.getBytes();
						byte[] message_body = bkup.getCurr_chunk();
						byte[] full_message = new byte[message_header_bytes.length + message_body.length];
						
						for(int i = 0; i < full_message.length; i++){
							if(i < message_header_bytes.length){
								full_message[i] = message_header_bytes[i];
							}else{
								full_message[i] = message_body[i-message_header_bytes.length];
							}
						}
						
						MulticastSocket socket = new MulticastSocket();
						InetAddress address = InetAddress.getByName(this.peer.mdb.getMcast_addr());
						DatagramPacket packet = new DatagramPacket(full_message, full_message.length, address, this.peer.mdb.getPort());
						socket.send(packet);
						
						System.out.println("Sending PUTCHUNK message");
						
						bkup.setState(Backup.State.SAVECHUNK);
						
						socket.close();
					}else if(bkup.state == Backup.State.SAVECHUNK){
						byte[] chunkData = bkup.getCurr_chunk();
						File output = new File("../peers/" + this.peer.getId() + "/" + bkup.getFileId() + "." + (bkup.getCurrChunkNo()+1));
						FileOutputStream chunk = new FileOutputStream(output);
						chunk.write(chunkData);
						chunk.flush();
						chunk.close();

						String message = bkup.getStored();
						MulticastSocket socket = new MulticastSocket();
						InetAddress address = InetAddress.getByName(this.peer.mc.getMcast_addr());
						DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), address, this.peer.mc.getPort());
						socket.send(packet);
						
						System.out.println("Sending STORED message");
						
						bkup.setState(Backup.State.WAITSTORED);
						
						socket.close();
					}else if(bkup.state == Backup.State.WAITSTORED){
						byte[] rbuf = new byte[(int) Math.pow(2,16)];
						MulticastSocket socket = new MulticastSocket(this.peer.mc.getPort());
						DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
						socket.receive(packet);
						
						System.out.println("Received STORED message");
						
						bkup.setState(Backup.State.RECEIVESTORED);
						
						socket.close();
					}else if(bkup.state == Backup.State.RECEIVESTORED){
												
						if(bkup.hasChunksLeft()){
							int currChunk = bkup.getCurrChunkNo();
							bkup.setCurr_chunk(currChunk++);
							
							bkup.setState(Backup.State.SENDCHUNK);
						}else{
							bkup.setState(Backup.State.DONE);
						}
					}else if(bkup.state == Backup.State.DONE){
						this.peer.queue.remove(protocol);
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
	
}
