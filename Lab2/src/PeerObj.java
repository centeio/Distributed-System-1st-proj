import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerObj extends Remote{
	
	void restore(String file) throws RemoteException; 
	void delete(String file) throws RemoteException;
	void reclaim(int space) throws RemoteException; 
	void backup(String file, int repdegree) throws RemoteException; 
}
