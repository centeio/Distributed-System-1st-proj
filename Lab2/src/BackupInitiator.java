public class BackupInitiator {
	private String filename;
	private int peerID;
	private int repdegree;
	
	public BackupInitiator(String filename, int peerId, int repdegree){
		this.filename = filename;
		this.peerID = peerId;
		this.repdegree = repdegree;
	}

	public String getFileName() {
		return filename;
	}

	public void setFileName(String filename) {
		this.filename = filename;
	}

	public int getPeerID() {
		return peerID;
	}

	public void setPeerID(int peerID) {
		this.peerID = peerID;
	}

	public int getRepdegree() {
		return repdegree;
	}

	public void setRepdegree(int repdegree) {
		this.repdegree = repdegree;
	}
}