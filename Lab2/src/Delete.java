/**
 * The class representing the delete protocol's state machine.
 */
public class Delete {
	
	/**
	 * protocol's state enum.
	 */
	public enum State {	     
     	
	     /**  deleting the original file. */ DELETEFILE, 
     	
	     /**  deleting the file's chunks in peer. */ DELETECHUNKS, 
		
		/**  protocol is done. */ DONE
	}
	
	/**  protocol's state. */
	public State state;
	
	/** file's id. */
	private String fileId;
	
	/**  id of initiator peer. */
	private int senderId;
	
	/** number of tries left. */
	private int triesLeft;

	/**
	 * Instantiates a new delete in initiator peer in DELETEFILE state.
	 *
	 * @param fileId the file's id in sha256
	 * @param senderId id of initiator peer
	 */
	public Delete(String fileId, int senderId) {
		super();
		this.fileId = fileId;
		this.senderId = senderId;
		
		//TODO: check how many tries are "necessary to ensure that all space used by chunks of the deleted file are deleted in spite of the loss of some messages"
		this.triesLeft = 3;
		this.state = Delete.State.DELETEFILE;
	}
	
	/**
	 * Instantiates a new delete in non-initiator peer in DELETECHUNKS state.
	 *
	 * @param fileId the file's id in sha256
	 */
	public Delete(String fileId) {
		super();
		this.fileId = fileId;
		this.senderId = -1;
		this.triesLeft = 1;
		this.state = Delete.State.DELETECHUNKS;
	}
	
	/**
	 * Gets the DELETE message for the deleting every chunk of that file
	 * Format of the message: DELETE &lt;Version&gt; &lt;SenderId&gt; &lt;FileId&gt; &lt;CRLF&gt;&lt;CRLF&gt;
	 *
	 * @return the message
	 */
	public String getMessage(){
		String message = "DELETE 1.0 " + this.senderId + " " + this.fileId + " \r\n\r\n";
		
		return message;
	}
	
	/**
	 * decreases the number of tries remaining and checks if protocol is done.
	 *
	 * @return the new state
	 */
	public State updateState(){
		
		triesLeft--;
		
		if(triesLeft == 0){
			this.state = Delete.State.DONE;
		}
		
		return this.state;
	}
	
	/**
	 * Gets the file id.
	 *
	 * @return the file id
	 */
	public String getFileId() {
		return fileId;
	}
}