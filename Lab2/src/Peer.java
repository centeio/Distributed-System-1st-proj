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
	private String id;
	private Registry registry;
		
	public String getId() {	return id;}
	public void setId(String id) {this.id = id;}
	public Registry getRegistry() {return registry;}
	
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}
	
	public Peer(String id) throws RemoteException {
		super();
		this.id = id;
	    PeerObj stub = (PeerObj) UnicastRemoteObject.exportObject(this, 0);

	    // Bind the remote object's stub in the registry
	    this.registry = LocateRegistry.getRegistry();
	    this.registry.rebind(this.id, stub);
	}
	
	public static void main(String[] args) throws IOException{
	
		if(args.length != 3){
			System.out.println("Usage: Peer <mcast_addr> <mcast_port> <name>");
			return;
		}		
		
	    Peer obj = new Peer(args[2]);	    
		
		Thread mc = new Thread(new MC(args[0], args[1]));
		Thread mdb = new Thread(new MDB(args[0], args[1]));
		Thread mdr = new Thread(new MDR(args[0], args[1]));

		mc.start();
		mdb.start();
		mdr.start();
	}
	
	@Override
	public String initOp(String operation, String file) throws RemoteException { //Restore and delete
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String initOp(String operation, int space) throws RemoteException { //Reclaim
		// TODO Auto-generated method stub
		return null;
	}
	
	
	/**
	 * 
	 * Backup
	 * 
	 * @param operation
	 * @param file
	 * @param repdegree
	 * 
	 * @return
	 */
	@Override
	public String initOp(String operation, String file, int repdegree) throws RemoteException {
		
		
		return "backup";
	}
	
	/**
	 * http://stackoverflow.com/questions/4431945/split-and-join-back-a-binary-file-in-java
	 * 
	 * @param filename
	 */
	public void backupFile(String filename){
		File file = new File(filename);
		if(!file.exists()) return;
		
		FileInputStream stream;
		FileOutputStream chunk;
		
		byte[] chunkData;
		long filelength = file.length();
		int chunkNo = 0;
		int chunkMaxSize = 1024 * 64;
		int readLength = chunkMaxSize;
		
		try{
			stream = new FileInputStream(file);
			
			while(filelength > 0){
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

				File output = new File(chunkName);
				chunk = new FileOutputStream(output);
				chunk.write(chunkData);
				chunk.flush();
				chunk.close();
				
				chunkData = null;
				chunk = null;
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
