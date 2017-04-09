public class Backup {
	public enum State {
	     SENDCHUNK, SAVECHUNK, WAITSTORED, RECEIVESTORED, RECEIVEREMOVED, DONE 
	}
	
	public State state ;
	private String fileId;
	private int senderId;
	private byte[] chunk;
	private int chunkNo;
	private int replication_degree;
	
	public int getReplication_degree() {
		return replication_degree;
	}

	public void setReplication_degree(int replication_degree) {
		this.replication_degree = replication_degree;
	}
	
	public Backup(String fileId, int senderId, int rep_degree){
		super();

		this.setFileId(fileId);
		this.setSenderId(senderId);
		this.replication_degree = rep_degree;
		
		this.state = Backup.State.SENDCHUNK;
	}

	public Backup(String fileId, byte[] chunk, int chunkNo, int senderId, int rep_degree, State state) {
		super();
		
		this.setChunk(chunk);
		this.setFileId(fileId);
		this.setSenderId(senderId);
		this.setChunkNo(chunkNo);
		this.setReplication_degree(rep_degree);
		
		this.state = state;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public int getSenderId() {
		return senderId;
	}

	public void setSenderId(int senderId) {
		this.senderId = senderId;
	}

	public void setState(State state){
		this.state = state;
	}
		
	/**
	 * PUTCHUNK <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
	 *
	 * @return
	 */
	public String getPutchunk() {
		String message = "PUTCHUNK 1.0 " + this.senderId + " " + this.fileId + " " + this.chunkNo + " " + this.replication_degree + " \r\n\r\n";
		return message;
	}

	/**
	 * STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
	 * 
	 * @return
	 */
	public String getStored() {
		String message = "STORED 1.0 " + this.senderId + " " + this.fileId + " " + this.chunkNo + " \r\n\r\n";
		return message;
	}

	public byte[] getChunk() {
		return chunk;
	}

	public void setChunk(byte[] chunk) {
		this.chunk = chunk;
	}

	public int getChunkNo() {
		return chunkNo;
	}

	public void setChunkNo(int chunkNo) {
		this.chunkNo = chunkNo;
	}

}
