import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

public class MulticastServer implements Runnable {
	private int mcast_port;
	private String srvc_addr;
	private int srvc_port;

	public MulticastServer(String mcastport, String srvcaddr, String srvcport){
		super();
		this.mcast_port = Integer.parseInt(mcastport);
		this.srvc_addr = srvcaddr;
		this.srvc_port = Integer.parseInt(srvcport);
	}
	
	@Override
	public void run() {
		MulticastSocket socket;
		try {
			socket = new MulticastSocket();
			String sbuf = "multicast:" + mcast_port + ":" + srvc_addr + ":" + srvc_port;
			
			InetAddress address = InetAddress.getByName(srvc_addr);
			DatagramPacket packet = new DatagramPacket(sbuf.getBytes(), sbuf.length(), address, srvc_port);
			
			int count = 0;
			
			while(count<5){
				System.out.println(sbuf);
				socket.send(packet);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				count++;
			}
			
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

	}	
	
}
