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
	private Backup b;
	private int ncopies;
	private Peer peerInitiator;
	
	public Reclaim(long space){
		super();
		this.setSpace(space);
	}
	
	public Reclaim(State state, String version, int ncopies, Backup b) {
		super();
		this.state = state;
		this.version = version;
	//	this.senderId = senderId;
		this.b = b;
	}

	public long getSpace() {
		return space;
	}

	public void setSpace(long space) {
		this.space = space;
	}

	public Backup getB() {
		return b;
	}

	public void setB(Backup b) {
		this.b = b;
	}

	public int getNcopies() {
		return ncopies;
	}

	public void setNcopies(int ncopies) {
		this.ncopies = ncopies;
	}

	public Peer getPeerInitiator() {
		return peerInitiator;
	}

	public void setPeerInitiator(Peer peerInitiator) {
		this.peerInitiator = peerInitiator;
	}
	
	
	
	

	
}