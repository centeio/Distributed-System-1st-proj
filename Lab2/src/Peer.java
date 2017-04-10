import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
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
	public ConcurrentHashMap<String, ArrayList<Backup>> protocols;
	private ConcurrentHashMap<String, ArrayList<Backup>> backups;
	public BlockingQueue<Object> queue;
	//Threadpool para processar por ordem
	public int maxspace = 200 * 1000;
	private int receivedStored;
	private boolean sending;
	
	public static void main(String[] args) throws IOException, InterruptedException{
		if(args.length != 8){
			System.out.println("Usage: Peer <version> <id> <mc_addr> <mc> <mdb_addr> <mdb> <mdr_addr> <mdr>");
			return;
		}		
		
	    new Peer(args);
	}
	
	public Peer(String[] args) throws RemoteException {
		super();

	    PeerObj stub = (PeerObj) UnicastRemoteObject.exportObject(this, 0);
	    
		this.id = Integer.parseInt(args[1]);
		this.mc = new MC(args[2], args[3],this);
		this.mdb = new  MDB(args[4], args[5],this);
		this.mdr = new MDR(args[6], args[7],this);
		this.queue = new LinkedBlockingQueue<Object>();
		this.protocols = new ConcurrentHashMap<String,ArrayList<Backup>>();
		this.backups = new ConcurrentHashMap<String,ArrayList<Backup>>();
		
		this.setSending(false);
		
	    this.registry = LocateRegistry.getRegistry();
	    this.registry.rebind(args[1], stub);
	    
	    this.folderName = "../peers/" + this.id;
	    
	    this.directory = new File(this.folderName);
	    this.directory.mkdir();
	    
	    ExecutorService executor = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 1; i++) {
            Runnable worker = new Operator(this);
            executor.execute(worker);
        }
        executor.shutdown();
        while (!executor.isTerminated()) {}
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public Registry getRegistry() {
		return registry;
	}
	
	public void setRegistry(Registry registry) {
		this.registry = registry;
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
	 * @param filename
	 * @param repdegree
	 * @throws InterruptedException 
	 */
	@Override
	public void backup(String filename, int repdegree) throws RemoteException{
		File file = new File(filename);
		if(!file.exists()){
			System.out.println("File does not exist.");
			return;
		}
				
		BackupInitiator bi = new BackupInitiator(filename, this.id, repdegree);
		bi.setPeerInitiator(this);
		this.queue.add(bi);
	
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
		Reclaim r = new Reclaim(space);
		r.setPeerInitiator(this);
		
		this.queue.add(r);
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
		if(!this.protocols.containsKey(fileId)){
			ArrayList<Backup> list = new ArrayList<Backup>();
			list.add(b);
			this.protocols.put(fileId, list);
		}else{
			this.protocols.get(fileId).add(b);
		}
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
	
	public Backup getChunk(ConcurrentHashMap<String,ArrayList<Backup>> table, String fileId, int chunkNo){
		if(table == null) return null;
		
		ArrayList<Backup> chunks = table.get(fileId);

		if(chunks != null){
			for (Backup chunk: chunks){
				if(chunk.getChunkNo() == chunkNo){
					return chunk;
				}
			}
		}
		return null;
	}
	
	public void chunkStored(String fileId, int chunkNo, int senderId) {
		Backup chunk;
		if(this.backups != null && (chunk = getChunk(this.backups,fileId,chunkNo)) != null){
			chunk.incNcopies(senderId);
			return;
		}			
		
		if(this.protocols == null || (chunk = getChunk(this.protocols,fileId,chunkNo)) == null)
			return;
		else{
			System.out.println("Stored chunk nCopies before= " +  chunk.getNcopies());
			chunk.incNcopies(senderId);
			System.out.println("Stored chunk nCopies after= " +  chunk.getNcopies());

		}
	}
	
	public void chunkRemoved(String fileId, int chunkNo, int senderId) throws InterruptedException {
		Backup chunk;
		if(this.backups != null && getChunk(this.backups,fileId,chunkNo) != null){
			System.out.println("initiator of this chunk "+chunkNo);		
			getChunk(this.backups,fileId,chunkNo).decNcopies(senderId);
			return;
		}
			if(this.protocols == null || getChunk(this.protocols,fileId,chunkNo) == null){
				System.out.println("chunk not in protocol "+chunkNo);
				return;
			}else{
				chunk = getChunk(this.protocols,fileId,chunkNo);
				chunk.decNcopies(senderId);
				System.out.println("ncp: " + chunk.getNcopies());
				System.out.println("rep: " + chunk.getReplication_degree());
				if(chunk.getReplication_degree() > chunk.getNcopies()){
					//New Backup
					System.out.println("Needs to Backup chunk");
					long randomTime = (0 + (int)(Math.random() * 4))*100;
					System.out.println("Will sleep " + randomTime);
					Thread.sleep(randomTime);
					System.out.println("woke up with dif: " + (chunk.getReplication_degree()-chunk.getNcopies()) + " ncopies: "+chunk.getNcopies());
					
					if(chunk.getReplication_degree() > chunk.getNcopies()){
						System.out.println("New Backup of chunk " + chunk.getChunkNo());
						System.out.println(chunk.getFilename());
						Backup newBackup = new Backup(chunk.getFileId(), this.getId(), chunk.getReplication_degree());
						newBackup.setChunk(chunk.getChunk());
						newBackup.setPeerInitiator(-1);
						newBackup.setChunkNo(chunk.getChunkNo());
						this.queue.add(newBackup);
					}
				}
			}

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
	
	/**
	 * Saves backups done by this peer
	 * 
	 * @param fileId fileId of the file backed up
	 * @param b backup of a chunk backed up
	 */
	public void saveBackupDone(String fileId, Backup b){
		if(!backups.containsKey(fileId)){
			ArrayList<Backup> list = new ArrayList<Backup>();
			list.add(b);
			backups.put(fileId, list);
		}else{
			backups.get(fileId).add(b);
		}
	}
	
	public ConcurrentHashMap<String, ArrayList<Backup>> getBackups() {
		return backups;
	}

	public void setBackups(ConcurrentHashMap<String, ArrayList<Backup>> backups) {
		this.backups = backups;
	}

	public boolean isSending() {
		return sending;
	}

	public void setSending(boolean sending) {
		this.sending = sending;
	}
	
}
