import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * Class for handling a single CONNECT request for a client.
 * Forwards data from one end of the connection to the other
 */
public class TunnelThread extends Thread {
	private Socket sender;
	private Socket receiver;

	/**
	 * Creates new thread for forwarding data from the sender to the receiver
	 * @param sender source of information
	 * @param receiver destination for information
	 */
	public TunnelThread(Socket sender, Socket receiver) {
		this.sender = sender;
		this.receiver = receiver;
	}

	/**
	 * Forwards all data from sender to receiver
	 */
	public void run() {
		try {
			DataInputStream fromSender = new DataInputStream(sender.getInputStream());
			DataOutputStream toReceiver = new DataOutputStream(receiver.getOutputStream());
			fromSender.transferTo(toReceiver);
		} catch (SocketException se){
			// Do nothing - catching when socket is closed
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
//		// TODO: clean up?
//		// Clean up
//		try {
//			sender.close();
//			receiver.close();
//		} catch (IOException e) {
//			System.out.println("Error closing socket");
//			e.printStackTrace();
//		}
	}
}
