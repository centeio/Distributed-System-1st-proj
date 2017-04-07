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
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Peer implements PeerObj {
	private int id;
	private Registry registry;
	private String mcast_addr;
	private int mcast_port;
	public MC mc;
	public MDB mdb;
	public MDR mdr;
	private String name;
	private String version;
	private File directory;
	private String folderName;
	public Hashtable protocols;
	public BlockingQueue<Object> queue;
	//Threadpool para processar por ordem
	public double space = 60; //em KB
		
	public int getId() {	return id;}
	public void setId(int id) {this.id = id;}
	public Registry getRegistry() {return registry;}
	
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}
	
	public Peer(String[] args) throws RemoteException {
		//<name> <mcast_addr> <mc> <mdb> <mdr>
		super();
		this.id = Integer.parseInt(args[1]);
		this.name = args[2];
		
		this.mc = new MC(args[3], args[4],this);
		this.mdb = new  MDB(args[5], args[6],this);
		this.mdr = new MDR(args[7], args[8],this);
		
		this.queue = new LinkedBlockingQueue<Object>();
		
	    PeerObj stub = (PeerObj) UnicastRemoteObject.exportObject(this, 0);

	    // Bind the remote object's stub in the registry
	    this.registry = LocateRegistry.getRegistry();
	    this.registry.rebind(this.name, stub);
	    
	    this.folderName = "../peers/"+this.id;
	    //creates dir for the peer
	    directory = new File(this.folderName);
	    directory.mkdir();
	    
	    //TODO: Threadpool stub
	    ExecutorService executor = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 10; i++) {
            Runnable worker = new Operator(this);
            executor.execute(worker);
          }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }
        System.out.println("Finished all threads");
	    
	}
	
	public static void main(String[] args) throws IOException, InterruptedException{
	
		if(args.length != 9){
			System.out.println("Usage: Peer <version> <id> <name> <mc_addr> <mc> <mdb_addr> <mdb> <mdr_addr> <mdr>");
			return;
		}		
		
	    Peer obj = new Peer(args);
	}
	
	@Override
	public void delete(String filename) throws RemoteException { //Restore and delete		
		File file = new File(filename);
		filename = file.getName();
		if(!file.exists()){
			System.out.println("File does not exist.");
			return;
		}

		String fileId = Operator.sha256(filename + file.lastModified() + this.id);
		this.queue.add(new Delete(fileId, this.id));
		
		if(!file.delete()){
			System.out.println(filename + " can not be deleted.");
			return;
		}
		
		System.out.println(filename + " will be deleted.");
	}
	
	/**
	 * 
	 * Backup
	 * 
	 * http://stackoverflow.com/questions/4431945/split-and-join-back-a-binary-file-in-java
	 * 
	 * @param filename
	 * @param repdegree
	 * @throws InterruptedException 
	 */
	@Override
	public void backup(String filename, int repdegree) throws RemoteException{
		File file = new File(filename);
		filename = file.getName();
		if(!file.exists()){
			System.out.println("File does not exist.");
			return;
		}
		
		ArrayList<byte[]> chunks = Operator.divideFileIntoChunks(file);
		String fileId = Operator.sha256(filename + file.lastModified() + this.id);
		this.queue.add(new Backup(fileId, this.id, repdegree, chunks));
		System.out.println("Backing up " + filename);
	}	
	
	@Override
	public void restore(String file) throws IOException {
		String chunkNo = "ch1";
		//TODO get chunkNo
		String message = "GETCHUNK " + version + " " + id + " " + file + " " + chunkNo +" <CRLF><CRLF>";
		MulticastSocket socket = new MulticastSocket();

		InetAddress address = InetAddress.getByName(mc.getMcast_addr());
		DatagramPacket packet = new DatagramPacket(message.getBytes(), message.toString().length(), address, mc.getPort());
		System.out.println("sends GETCHUNK to " + mc.getMcast_addr() + " port " + mc.getPort());

		socket.send(packet);
		
		address = InetAddress.getByName(mdr.getMcast_addr());
		byte[] rbuf = new byte[(int) Math.pow(2,16)];
		packet = new DatagramPacket(rbuf, rbuf.toString().length(), address, mdr.getPort());
		socket.receive(packet);
		
		System.out.println("gets CHUNK from " + packet.getAddress() + " port " + packet.getPort());
		
		socket.close();		
	}
		
	@Override
	public void reclaim(int space) throws RemoteException { //Reclaim
		// TODO Auto-generated method stub
	}	
	
	/**
	 * http://stackoverflow.com/questions/4431945/split-and-join-back-a-binary-file-in-java
	 * 
	 * @param filename
	 */
	public void restoreFile(String filename, List<File> files){
		File file = new File(filename);
		
		FileOutputStream chunk;
		FileInputStream stream;
		
		byte[] fileData;
		
//		List<File> files = findFiles(filename);
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

	public File getDirectory() {
		return directory;
	}
}
