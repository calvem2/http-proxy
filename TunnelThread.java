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
//			System.out.println("Handling tunnel");
			DataInputStream fromSender = new DataInputStream(sender.getInputStream());
			DataOutputStream toReceiver = new DataOutputStream(receiver.getOutputStream());
			fromSender.transferTo(toReceiver);
//			fromSender.close();
//			toReceiver.flush();
//			toReceiver.close();
//			System.out.println("Done with tunnel");
		} catch (SocketException se){
			// TODO: hi
			// do nothing? see ed post..."just general try catch blocks to handle cases where socket closed"
			// https://us.edstem.org/courses/403/discussion/72632

		} catch (IOException e) {
//			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}

		// TODO: clean up?
		// Clean up
//		try {
//			sender.close();
//			receiver.close();
//		} catch (IOException e) {
//			System.out.println("Error closing socket");
//			e.printStackTrace();
//		}
	}
}
