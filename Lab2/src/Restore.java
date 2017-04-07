public class Restore {
	public enum State {
	     SENDGETCHUNK, LOOKUP, SENDCHUNK, RECEIVEMESSAGE, Done
	}
	public State state;
}
