public class Reclaim {
	public enum State {
	     LOOKUPCHUNKS, BACKUP, DONE
	}
	public State state;
	public String version;
	public int senderId;
	public String fileId;
	public int chunkNo;
	private long space;
	
	public Reclaim(long space){
		super();
		this.setSpace(space);
	}
	
	public Reclaim(State state, String version, String fileId, int chunkNo) {
		super();
		this.state = state;
		this.version = version;
	//	this.senderId = senderId;
		this.fileId = fileId;
		this.chunkNo = chunkNo;
	}

	public long getSpace() {
		return space;
	}

	public void setSpace(long space) {
		this.space = space;
	}
	
	
	
	

	
}