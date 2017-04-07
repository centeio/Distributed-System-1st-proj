public class Restore {
	public enum State {
	     SENDGETCHUNK, LOOKUP, SENDCHUNK, RECEIVEMESSAGE, DONE
	}
	public State state;
}
