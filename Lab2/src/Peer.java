import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Peer implements PeerObj {
	private int id;
	private Registry registry;
	private String mcast_addr;
	private int mcast_port;
	private MC mc;
	private MDB mdb;
	private MDR mdr;
	private String name;
		
	public int getId() {	return id;}
	public void setId(int id) {this.id = id;}
	public Registry getRegistry() {return registry;}
	
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}
	
	public Peer(String[] args) throws RemoteException {
		//<name> <mcast_addr> <mc> <mdb> <mdr>
		super();
		id = Integer.parseInt(args[1]);
		name = args[2];
		
	    mc = new MC(args[3], args[4]);
		mdb = new  MDB(args[5], args[6]);
		mdr = new MDR(args[7], args[8]);
		
	    PeerObj stub = (PeerObj) UnicastRemoteObject.exportObject(this, 0);

	    // Bind the remote object's stub in the registry
	    this.registry = LocateRegistry.getRegistry();
	    this.registry.rebind(this.name, stub);

	}
	
	public static void main(String[] args) throws IOException, InterruptedException{
	
		if(args.length != 9){
			System.out.println("Usage: Peer <version> <id> <name> <mc_addr> <mc> <mdb_addr> <mdb> <mdr_addr> <mdr>");
			return;
		}		
		
	    Peer obj = new Peer(args);
	}
	
	@Override
	public void delete(String file) throws RemoteException { //Restore and delete
		// TODO Auto-generated method stub
	}
	
	@Override
	public void reclaim(int space) throws RemoteException { //Reclaim
		// TODO Auto-generated method stub
	}	
	
	
	
	/**
	 * 
	 * Backup
	 * 
	 * http://stackoverflow.com/questions/4431945/split-and-join-back-a-binary-file-in-java
	 * 
	 * @param filename
	 * @param repdegree
	 * 
	 * @return
	 */
	@Override
	public void backup(String filename, int repdegree) throws RemoteException {
		System.out.println("entra backup "+filename);

		File file = new File(filename);
		if(!file.exists()) return;
		
		FileInputStream stream;
		FileOutputStream chunk;
		
		byte[] chunkData;
		long filelength = file.length();
		int chunkNo = 0;
		int chunkMaxSize = 1024 * 64;
		int readLength = chunkMaxSize;
		int n = 1;
		int m = 0;
		MulticastSocket socket;

		
		// TODO FileId
		
		try{
			stream = new FileInputStream(file);
			socket = new MulticastSocket();

			
			while(filelength > 0){
				System.out.println("entra ciclo backup");
				if(filelength < chunkMaxSize){
					readLength = (int)filelength;
				}
				
				chunkData = new byte[readLength];
				
				/*
				 * Reads information from the file up until 64KB (maximum size allowed)
				 * If the file has less than 64KB, it reads all the information and saves
				 * on 1 chunk.
				 */
				int bytesRead = stream.read(chunkData, 0, readLength);
				filelength -= bytesRead;
				
				if(chunkData.length != bytesRead){
					System.out.println("Error reading chunk");
					break;
				}
				
				chunkNo++;
				
				/*
				 * Saves information of the chunk onto a new file name:
				 * filename.partX, being x a number.
				 * Eg: text.part1 - text.part2 - text.part3
				 */
				String chunkName = filename + ".part" + Integer.toString(chunkNo);

				//PUTCHUNK <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
				//TODO ChunkName e parametros corretos
				String message = "PUTCHUNK " + Integer.toHexString(n) +  Integer.toHexString(m) + " " 
				+ this.id + " " + filename + " " + chunkNo + " " + repdegree + " <CRLF><CRLF>" + chunkData;
				
		/*		File output = new File(chunkName);
				chunk = new FileOutputStream(output);
				chunk.write(chunkData);
				chunk.flush();
				chunk.close();*/
								
				InetAddress address = InetAddress.getByName(mdb.getMcast_addr());
				DatagramPacket packet = new DatagramPacket(message.getBytes(), message.toString().length(), address, mdb.getPort());
				System.out.println("sends to " + mdb.getMcast_addr() + " port " + mdb.getPort());

				socket.send(packet);
				
				//TODO ciclo com timeout para receber Stored em novo MulticastSocket com port MC
				
	//			output.delete();
				chunkData = null;
	//			chunk = null;
				

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
	
	@Override
	public void restore(String file) throws RemoteException {
	}
	


	
	/**
	 * http://stackoverflow.com/questions/4431945/split-and-join-back-a-binary-file-in-java
	 * 
	 * @param filename
	 */
	public void restoreFile(String filename){
		File file = new File(filename);
		
		FileOutputStream chunk;
		FileInputStream stream;
		
		byte[] fileData;
		
		List<File> files = findFiles(filename);
		/*Collections.sort(files, new Comparator<File>(){
			@Override
			public int compare(File f1, File f2) {
				return  f1.getName().compareTo(f2.getName());
			}
		});*/
		
		try{
			chunk = new FileOutputStream(file, true);
			for(File f : files){
				stream = new FileInputStream(f);
				fileData = new byte[(int)f.length()];
				
				int read = stream.read(fileData, 0, (int)f.length());
				
				chunk.write(fileData);
				chunk.flush();
				fileData = null;
				stream.close();
				stream = null;
			}
			
			chunk.close();
			chunk = null;
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

	public List<File> findFiles(String filename){
		List<File> files = new ArrayList<File>();
		
		int chunkNo = 1;
		
		String tmpName = filename + ".part" + Integer.toString(chunkNo);
		File f = new File(tmpName);
	
		while(f.exists()){
			files.add(f);
			chunkNo++;
			tmpName = filename + ".part" + Integer.toString(chunkNo);

			f = new File(tmpName);
		}
		
		return files;
	}
	

}
