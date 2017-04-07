import java.util.ArrayList;

public class Backup {
	public enum State {
	     SENDCHUNK, SAVECHUNK, WAITSTORED, RECEIVESTORED, RECEIVEREMOVED, DONE 
	}
	
	public State state ;
	private String fileId;
	private int senderId;
	private ArrayList<byte[]> chunks;
	private int curr_chunk;
	private int replication_degree;
	
	public Backup(String fileId, int senderId, int rep_degree, ArrayList<byte[]> chunks){
		super();

		this.setFileId(fileId);
		this.setSenderId(senderId);
		this.chunks = chunks;
		this.setCurr_chunk(0);
		this.replication_degree = rep_degree;
		
		this.state = Backup.State.SENDCHUNK;
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
	
	public boolean hasChunksLeft() {
		try{
			this.chunks.get(curr_chunk);
			return true;
		}catch(IndexOutOfBoundsException e){
			return false;
		}
	}

	public int getCurrChunkNo(){
		return this.curr_chunk;
	}
	
	/**
	 * PUTCHUNK <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
	 *
	 * @return
	 */
	public String getPutchunk() {
		String message = "PUTCHUNK 1.0 " + this.senderId + " " + this.fileId + " " + this.curr_chunk + " " + this.replication_degree + " \r\n\r\n";
		return message;
	}

	public byte[] getCurr_chunk() {
		return this.chunks.get(this.curr_chunk);
	}

	public void setCurr_chunk(int curr_chunk) {
		this.curr_chunk = curr_chunk;
	}

	/**
	 * STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
	 * 
	 * @return
	 */
	public String getStored() {
		String message = "STORED 1.0 " + this.senderId + " " + this.fileId + " " + this.curr_chunk + " \r\n\r\n";
		return message;
	}

}
