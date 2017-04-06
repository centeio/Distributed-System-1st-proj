import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Operator {

	/**
	 * Splits files into chunks with 64KB each
	 * 
	 * @param filename Name of the file  to split
	 * @param mdb Channel where the chunk if going to be sent to 
	 * @throws InterruptedException 
	 */
	public void splitFile(String filename, int peerID, int rd, MDB mdb, MC mc){
		System.out.println("Backing up file " + filename);

		File file = new File(filename);
		filename = file.getName();
		if(!file.exists()){
			System.out.println("File does not exist.");
			return;
		}
		
		FileInputStream stream;
		
		byte[] chunkData;
		long filelength = file.length();
		int chunkNo = 0;
		int chunkMaxSize = 1024 * 64;
		int readLength = chunkMaxSize;
		MulticastSocket socket;

		try{
			stream = new FileInputStream(file);
			socket = new MulticastSocket();

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
				
				chunkNo++;

				String messageType = "PUTCHUNK";
				String version = "1.0";
				int senderId = peerID;
				String fileId = sha256(filename + Integer.toString(chunkNo));
				int repDegree = rd;

				String header = messageType + " " + version + " " + senderId + " " + fileId + " " + chunkNo + " " + repDegree + " " + " \r\n\r\n";
				byte[] headerBytes = header.getBytes();
				
				byte[] message = new byte[headerBytes.length + chunkData.length];
				
				for(int i = 0; i < message.length; i++){
					if(i < headerBytes.length){
						message[i] = headerBytes[i];
					}else{
						message[i] = chunkData[i-headerBytes.length];
					}
				}
					
				InetAddress address = InetAddress.getByName(mdb.getMcast_addr());
				DatagramPacket packet = new DatagramPacket(message, message.length, address, mdb.getPort());
				socket.send(packet);
				
				System.out.println("Sending PUTCHUNK message to: \n\t\taddress:" + mdb.getMcast_addr() + "\n\t\tport: " + mdb.getPort());	
				
				//TODO ciclo com timeout para receber Stored em novo MulticastSocket com port MC
				
				chunkData = null;
			}
			
			stream.close();
			stream = null;
		}catch(FileNotFoundException e){
			System.out.println("File " + filename + " not found");
			return;
		}catch(SecurityException e){
			System.out.println("Denied reading file " + filename);
			return;
		} catch (IOException e) {
			System.out.println("Error closing stream of file " + filename);
			e.printStackTrace();
		}
	}
	
	/**
	 * Encodes a given String according to the MessageDigest SHA-256 algorithm
	 * 
	 * @param base String to encode
	 * @return Encoded String
	 */
	public String sha256(String base) {
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
	
}
