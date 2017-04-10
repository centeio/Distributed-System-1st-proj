<<<<<<< HEAD
import java.io.Serializable;

@SuppressWarnings("serial")
public class Backup implements Serializable{

=======
import java.util.ArrayList;

public class Backup {
>>>>>>> master
	public enum State {
	     SENDCHUNK, SAVECHUNK, WAITSTORED, RECEIVESTORED, RECEIVEREMOVED, DONE 
	}
	//TODO tirar RECEIVEDSTORED, WAITSTORED etc
	
	private State state ;
	private String filename;
	private String fileId;
	private int senderId;
	private byte[] chunk;
	private int chunkNo;
	private int replication_degree;
	private int storedMessages;
	private int peerInitiator;
	private ArrayList<Integer> peers;
	
	public ArrayList<Integer> getPeers() {
		return peers;
	}

	public void setPeers(ArrayList<Integer> peers) {
		this.peers = peers;
	}

	/**
	 * Constructor for a Backup object
	 * 
	 * @param fileId Hashed file name
	 * @param senderId Id of the peer that sends the message
	 * @param rep_degree Replication degree of the file
	 */
	public Backup(String fileId, int senderId, int rep_degree){
		super();

		this.setFileId(fileId);
		this.setSenderId(senderId);
		this.setReplication_degree(rep_degree);
		this.peers =  new ArrayList<Integer>();
		this.state = Backup.State.SENDCHUNK;
	}
	
	/**
	 * Constructor for a Backup object
	 * 
	 * @param fileId Hashed file name
	 * @param chunk Data of the chunk
	 * @param chunkNo Number of the chunk
	 * @param senderId Id of the peer that sends the message
	 * @param rep_degree Replication degree of the file
	 * @param state State of the Backup object
	 */
	public Backup(String fileId, byte[] chunk, int chunkNo, int senderId, int rep_degree, State state) {
		super();
		
		this.setChunk(chunk);
		this.setFileId(fileId);
		this.setSenderId(senderId);
		this.setChunkNo(chunkNo);
		this.setReplication_degree(rep_degree);
		this.setStoredMessages(0);
		this.peers =  new ArrayList<Integer>();
		
		this.state = state;
	}

	/**
	 * Returns the hashed file id
	 * @return fileId
	 */
	public String getFileId() {
		return fileId;
	}

	/**
	 * Changes the value of the hashed file id
	 * @param fileId new hashed file id
	 */
	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	/**
	 * Returns the id of the sender
	 * @return senderId
	 */
	public int getSenderId() {
		return senderId;
	}

	/**
	 * Changes the value of the sender id
	 * @param senderId new sender id
	 */
	public void setSenderId(int senderId) {
		this.senderId = senderId;
	}
		
	/**
	 * Returns the PUTCHUNK message for the backup of that chunk
	 * Format of the message: PUTCHUNK <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
	 * @return PUTCHUNK message
	 */
	public String getPutchunk() {
		String message = "PUTCHUNK 1.0 " + this.senderId + " " + this.fileId + " " + this.chunkNo + " " + this.replication_degree + " \r\n\r\n";
		return message;
	}

	/**
	 * Returns the STORED message for the backup of that chunk
	 * Format of the message: STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
	 * @return STORED message
	 */
	public String getStored() {
		String message = "STORED 1.0 " + this.senderId + " " + this.fileId + " " + this.chunkNo + " \r\n\r\n";
		return message;
	}

	/**
	 * Returns the chunk data of the backup
	 * @return chunk
	 */
	public byte[] getChunk() {
		return chunk;
	}

	/**
	 * Changes the chunk data of the backup
	 * @param chunk new chunk data
	 */
	public void setChunk(byte[] chunk) {
		this.chunk = chunk;
	}

	/**
	 * Returns the chunk number
	 * @return chunkNo
	 */
	public int getChunkNo() {
		return chunkNo;
	}

	/**
	 * Changes the value of the chunk number
	 * @param chunkNo new chunk number
	 */
	public void setChunkNo(int chunkNo) {
		this.chunkNo = chunkNo;
	}
	
	/**
	 * Returns the replication degree of the backup
	 * @return replication_degree
	 */
	public int getReplication_degree() {
		return replication_degree;
	}

	/**
	 * Changes the value of the replication degree
	 * @param replication_degree new replication degree
	 */
	public void setReplication_degree(int replication_degree) {
		this.replication_degree = replication_degree;
	}
	
	/**
	 * Returns the state of the backup
	 * @return state
	 */
	public State getState(){
		return state;
	}

	/**
	 * Changes the state of the backup
	 * @param state new state
	 */
	public void setState(State state){
		this.state = state;
	}

	/**
	 * Returns the replication degree of the backup
	 * @return replication_degree
	 */
	public int getNcopies() {
		return peers.size();
	}

	/**
	 * Adds a new peer to peers
	 * @param peerId new peer
	 */
	public void incNcopies(int peerId) {
		for(int peer: peers){
			if(peer == peerId)
				return;
		}
		peers.add(peerId);
	}
	
	/**
	 * Deletes a new peer to peers
	 * @param peerId old peer
	 */
	public void decNcopies(int peerId) {
		peers.remove((Integer)peerId);
	}	

	public int getStoredMessages() {
		return storedMessages;
	}

	public void setStoredMessages(int storedMessages) {
		this.storedMessages = storedMessages;
	}

	public int getPeerInitiator() {
		return peerInitiator;
	}

	public void setPeerInitiator(int peerInitiator) {
		this.peerInitiator = peerInitiator;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}
}
