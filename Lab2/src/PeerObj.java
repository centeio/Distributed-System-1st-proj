import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerObj extends Remote{
	
	String initOp(String operation, String file) throws RemoteException; //Restore and delete
	String initOp(String operation, int space) throws RemoteException; //Reclaim
	String initOp(String operation, String file, int repdegree) throws RemoteException; //Backup

}
