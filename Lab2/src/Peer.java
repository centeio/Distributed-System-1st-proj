import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Peer implements PeerObj {
	private int id;
	private Registry registry;
	public MC mc;
	public MDB mdb;
	public MDR mdr;
	private String name;
	private String version;
	public File directory;
	private String folderName;
	public Hashtable<String, ArrayList<Backup>> protocols;
	public BlockingQueue<Object> queue;
	//Threadpool para processar por ordem
	public double space = 60; //em KB
	public int maxspace = 200;
	private int receivedStored;
	public int getId() {	return id;}
	public void setId(int id) {this.id = id;}
	public Registry getRegistry() {return registry;}
	private boolean initiator = false;
	
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
		this.protocols = new Hashtable<String,ArrayList<Backup>>();
		//TODO put in hashtable
		
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
        for (int i = 0; i < 2; i++) {
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
		this.setInitiator(true);
		
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
		this.setInitiator(true);
		
		File file = new File(filename);
		if(!file.exists()){
			System.out.println("File does not exist.");
			return;
		}
		this.setReceivedStored(0);
		this.queue.add(new BackupInitiator(filename, this.id, repdegree));
		System.out.println("Backing up " + filename);
	}	
	
	@Override
	public void restore(String file) throws IOException {
		this.setInitiator(true);
		
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
		this.setInitiator(true);
		
		this.queue.add(new Reclaim(space));
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
	public void addBackup(String fileId, Backup b) {
		if(this.protocols.get(fileId) == null){
			this.protocols.put(fileId, new ArrayList<Backup>());
		}
		this.protocols.get(fileId).add(b);
	}
	public String getVersion() {
		return version;
	}
	public int getReceivedStored() {
		return receivedStored;
	}
	public void setReceivedStored(int receivedStored) {
		this.receivedStored = receivedStored;
		
	}
	public boolean isInitiator() {
		return initiator;
	}
	public void setInitiator(boolean initiator) {
		this.initiator = initiator;
	}
	
	public Backup getChunk(String fileId, int chunkNo){
		ArrayList<Backup> chunks = this.protocols.get(fileId);
		for (Backup chunk: chunks){
			if(chunk.getChunkNo() == chunkNo){
				return chunk;
			}
		}
		return null;
	}
	
	public void chunkStored(String fileId, int chunkNo) {
		Backup chunk;
		if((chunk = getChunk(fileId,chunkNo)) == null)
			return;
		else{
			chunk.setReplication_degree(chunk.getReplication_degree()+1);
		}
	}
	
	public void chunkRemoved(String fileId, int chunkNo) {
		Backup chunk;
		if((chunk = getChunk(fileId,chunkNo)) == null)
			return;
		else{
			chunk.setReplication_degree(chunk.getReplication_degree()-1);
			if(chunk.getReplication_degree() < chunk.getNcopies()){
				this.queue.add(new Reclaim(Reclaim.State.BACKUP, this.getVersion(), fileId, chunkNo));
			}
		}
	}	
	
	public int countRepDegree(String fileID, int chunkN){
		ArrayList<Backup> backups = this.protocols.get(fileID);
		
		int count = 0;
		
		for(Backup b : backups){
			if(b.getChunkNo() == chunkN){
				count++;
			}
		}
		
		return count;
	}
	public boolean canSaveChunk(String fileId, int chunkNo, int rep) {
		ArrayList<Backup> backups = this.protocols.get(fileId);

		if(backups == null) return true;
		
		int count = 0;
		for(Backup b : backups){
			if(b.getChunkNo() == chunkNo){
				return false;
			}
		}
		
		return true;
	}
	public int getNumberChunks(String fileId, int chunkNo) {
		return this.mc.getNumberChunks(fileId,chunkNo);
	}
}
