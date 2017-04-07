public class Delete {
	public enum State {
	     DELETEFILE, DELETECHUNKS, DONE
	}
	public State state;
	
	private String fileId;
	private int senderId;
	private int triesLeft;

	public Delete(String fileId, int senderId) {
		super();
		this.fileId = fileId;
		this.senderId = senderId;
		
		//TODO: check how many tries are "necessary to ensure that all space used by chunks of the deleted file are deleted in spite of the loss of some messages"
		this.triesLeft = 3;
		this.state = Delete.State.DELETEFILE;
	}
	
	public Delete(String fileId) {
		super();
		this.fileId = fileId;
		this.senderId = -1;
		this.triesLeft = 1;
		this.state = Delete.State.DELETECHUNKS;
	}
	
	public String getMessage(){
		String message = "DELETE 1.0 " + this.senderId + " " + this.fileId + " \\r\\n\\r\\n";
		
		return message;
	}
	
	public State updateState(){
		
		triesLeft--;
		
		if(triesLeft == 0){
			this.state = Delete.State.DONE;
		}
		
		return this.state;
	}
	
	public String getFileId() {
		return fileId;
	}
}