import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

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

	public void run() {
		try {
			InputStream fromSender = sender.getInputStream();
			OutputStream toReceiver = receiver.getOutputStream();
			fromSender.transferTo(toReceiver);
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
