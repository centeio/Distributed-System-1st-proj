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

//TODO backup -> ver se tem espaço para guardar, senao apaga chunk com rep degree > do que o suposto e guarda

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
	private ConcurrentHashMap<String, String> file_fileid;
	public BlockingQueue<Object> queue;
	public List<File> restoreFile;
	//Threadpool para processar por ordem
	public int maxspace = 200 * 1000;
	private int receivedStored;
	public boolean receivedChunk = false;
	
	public static void main(String[] args) throws IOException, InterruptedException{
		if(args.length != 8){
			System.out.println("Usage: Peer <version> <id> <mc_addr> <mc> <mdb_addr> <mdb> <mdr_addr> <mdr>");
			return;
		}		
		
	    Peer p = new Peer(args);
	}
	
	@SuppressWarnings("unchecked")
	public Peer(String[] args) throws IOException {
		super();

	    PeerObj stub = (PeerObj) UnicastRemoteObject.exportObject(this, 0);
	    
		this.id = Integer.parseInt(args[1]);
		this.mc = new MC(args[2], args[3],this);
		this.mdb = new  MDB(args[4], args[5],this);
		this.mdr = new MDR(args[6], args[7],this);
		this.queue = new LinkedBlockingQueue<Object>();
		this.protocols = new ConcurrentHashMap<String,ArrayList<Backup>>();
		this.backups = new ConcurrentHashMap<String,ArrayList<Backup>>();
		this.file_fileid = new ConcurrentHashMap<String, String>();
		this.restoreFile = new ArrayList<File>();
		
	    this.registry = LocateRegistry.getRegistry();
	    this.registry.rebind(args[1], stub);
	    
	    this.folderName = "../peers/" + this.id;

	    this.directory = new File(this.folderName);
	    this.directory.mkdir();

	    File mem_dir = new File("../memory/");
	    if(!mem_dir.exists()) mem_dir.mkdir();
        
		File f1 = new File("../memory/" + this.id + "_protocols" + ".tmp"); 
		File f2 = new File("../memory/" + this.id + "_backups" + ".tmp");
		File f3 = new File("../memory/" + this.id + "_files" + ".txt");
	    
        if(!f1.exists()){
        	f1.createNewFile();
        }else{
        	loadMemoryProtocols();
        }
        
        if(!f2.exists()){
        	f2.createNewFile();
        }else{
        	loadMemoryBackups();
        }
        
        if(!f3.exists()){
        	f3.createNewFile();
        }else{
        	loadMemoryFiles();
        }
        
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
	
	@SuppressWarnings("unchecked")
	public void loadMemoryProtocols(){
		try {
			FileInputStream fis = new FileInputStream("../memory/"+this.id +"_protocols"+ ".tmp");
	        ObjectInputStream ois = new ObjectInputStream(fis);

			this.protocols = (ConcurrentHashMap<String, ArrayList<Backup>>) ois.readObject();
			ois.close();
		} catch (EOFException ignored){
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void loadMemoryBackups(){
		try {
			FileInputStream fis = new FileInputStream("../memory/"+this.id +"_backups"+ ".tmp");
	        ObjectInputStream ois = new ObjectInputStream(fis);
	        
			this.backups = (ConcurrentHashMap<String, ArrayList<Backup>>) ois.readObject();
			ois.close();
		} catch (EOFException ignored){
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void loadMemoryFiles(){
		try {
			BufferedReader br = new BufferedReader(new FileReader("../memory/"+this.id +"_files"+ ".txt"));
		    String line = br.readLine();
		    while (line != null) {
			    String[] file_fileid = line.split(":");
			    
			    String filename = file_fileid[0];
			    String file_id = file_fileid[1];
			    
			    this.file_fileid.put(filename, file_id);
			    
		        line = br.readLine();
		    }
		    br.close();		    		    
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	
	/**
	 * 
	 * Restore
	 * 
	 * @param filename
	 */
	@Override
	public void restore(String filename) throws IOException {
		File file = new File(filename);
		if(!file.exists()){
			String nameOfFile = file.getName();
			
			Restore r = new Restore(nameOfFile, this.id, this, Restore.State.SENDGETCHUNK);
			this.queue.add(r);
			
			System.out.println("Restoring file " + filename);
		}
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
		try {

			if(!this.protocols.containsKey(fileId)){
				ArrayList<Backup> list = new ArrayList<Backup>();
				list.add(b);
				this.protocols.put(fileId, list);
			}else{
				this.protocols.get(fileId).add(b);
			}

			FileOutputStream fos = new FileOutputStream("../memory/" + this.id +"_protocols"+ ".tmp");
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this.protocols);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
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
	
	public void chunkStored(String fileId, int chunkNo) {
		Backup chunk;
		if((chunk = getChunk(this.protocols,fileId,chunkNo)) == null)
			return;
		else{
			System.out.println("Stored chunk nCopies before= " +  chunk.getNcopies());
			chunk.setNcopies(chunk.getNcopies()+1);
			System.out.println("Stored chunk nCopies after= " +  chunk.getNcopies());

		}
	}
	
	public void chunkRemoved(String fileId, int chunkNo) throws InterruptedException {
		Backup chunk;

			if(this.protocols == null || (chunk = getChunk(this.protocols,fileId,chunkNo)) == null){
				return;
			}else{
				System.out.println(chunk.getChunkNo());
				if(chunk.getPeerInitiator() == this.id) return;
				
				chunk.setNcopies(Math.max(chunk.getNcopies()-1, 0));
				System.out.println("ncp: " + chunk.getNcopies());
				System.out.println("rep: " + chunk.getReplication_degree());
				if(chunk.getReplication_degree() > chunk.getNcopies()){
					System.out.println("New Backup of chunk " + chunk.getChunkNo());
					
					long randomTime = (0 + (int)(Math.random() * 4))*100;
					Thread.sleep(randomTime);
					
					if((chunk.getReplication_degree()-chunk.getNcopies()) == chunk.getNcopies()){
						this.queue.add(new BackupInitiator(chunk.getFilename(), this.id, chunk.getNcopies()));
					}
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
	
	/**
	 * Saves backups done by this peer
	 * 
	 * @param fileId fileId of the file backed up
	 * @param b backup of a chunk backed up
	 */
	public void saveBackupDone(String fileId, Backup b){
		try {

			if(!this.backups.containsKey(fileId)){
				ArrayList<Backup> list = new ArrayList<Backup>();
				list.add(b);
				this.backups.put(fileId, list);
			}else{
				this.backups.get(fileId).add(b);
			}

			FileOutputStream fos = new FileOutputStream("../memory/" + this.id +"_backups"+ ".tmp");
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this.backups);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	
	public ConcurrentHashMap<String, ArrayList<Backup>> getBackups() {
		return backups;
	}

	public void setBackups(ConcurrentHashMap<String, ArrayList<Backup>> backups) {
		this.backups = backups;
	}

	public ConcurrentHashMap<String, String> getFile_fileid() {
		return file_fileid;
	}

	public void setFile_fileid(ConcurrentHashMap<String, String> file_fileid) {
		this.file_fileid = file_fileid;
	}
	
}
