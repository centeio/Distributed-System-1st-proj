public class Reclaim {
	public enum State {
	     LOOKUPCHUNKS, SENDREMOVED, DONE
	}
	public State state;
	public double version;
	public int senderId;
	public String fileId;
	public int chunkNo;
	
	public Reclaim(State state, double version, int senderId, String fileId, int chunkNo) {
		super();
		this.state = state;
		this.version = version;
		this.senderId = senderId;
		this.fileId = fileId;
		this.chunkNo = chunkNo;
	}
	
	
	
	

	
}