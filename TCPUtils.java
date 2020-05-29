import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Utility class for making connections to the proxy
 */
public class TCPUtils {
	/**
	 * Creates new TCP connection with the specified port
	 * @param port to make connection on
	 * @return server socket for proxy to accept connections on
	 */
	public static ServerSocket getProxy(int port) {
		ServerSocket tcp = null;
		try {
			tcp = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("Error: Could not open socket");
			e.printStackTrace();
			System.exit(1);
		}
		return tcp;
	}

	/**
	 * Accept a connection on the proxy's tcp socket
	 * @param proxy server socket to accept connection on
	 * @return socket representing client-proxy connection
	 */
	public static Connection accept(ServerSocket proxy) {
		Socket client = null;
		try {
			client = proxy.accept();
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
		return new Connection(client);
	}
}
