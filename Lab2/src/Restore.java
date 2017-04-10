
public class Restore {
	public enum State {
	     SENDGETCHUNK, LOOKUP, SENDCHUNK, RECEIVEMESSAGE, DONE
	}
	public State state;
	
	private String filename;
	private String file_id;
	private int senderId;
	private byte[] data;
	private int chunkNo;
	private Peer peerInitiator;
	
	public Restore(String filename, int senderId, Peer peerInitiator, State sendgetchnk){
		this.setFilename(filename);
		this.setPeerInitiator(peerInitiator);

		state = sendgetchnk;
	}

	public Restore(int senderId2, byte[] data, String fileId, int chunkNo, State sendchunk) {
		this.setSenderId(senderId2);
		this.setFile_id(fileId);
		this.setChunkNo(chunkNo);
		this.setData(data);
		state = sendchunk;
	}

	public Restore(String fileId, State done) {
		this.setFile_id(fileId);

		state = done;
	}

	private void setChunkNo(int chunkNo) {
		this.chunkNo = chunkNo;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getFile_id() {
		return file_id;
	}

	public void setFile_id(String file_id) {
		this.file_id = file_id;
	}
	
	public void setSenderId(int senderId){
		this.senderId = senderId;
	}
	
	public int getSenderId(){
		return senderId;
	}
	
	/**
	 * GETCHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
	 * @return
	 */
	public String getGetchunk(int chunkNo){
		String message = "GETCHUNK 1.0 " + this.senderId + " " + this.file_id + " " + chunkNo + " \r\n\r\n";
		return message;
	}

	/**
	 * CHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
	 * @return
	 */
	public String getChunk(){
		String message = "CHUNK 1.0 " + this.senderId + " " + this.file_id + " " + chunkNo + " \r\n\r\n";
		return message;
	}
	
	public Peer getPeerInitiator() {
		return peerInitiator;
	}

	public void setPeerInitiator(Peer peerInitiator) {
		this.peerInitiator = peerInitiator;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
}
