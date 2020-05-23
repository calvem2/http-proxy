import java.net.Socket;

public class Proxy {

	/**
	 * Runs a proxy for handling transactions between browsers and servers
	 * @param args program args; requires one argument that specifies port number for
	 *             proxy to create connections on
	 */
	public static void main(String[] args) {
		// Check correct number of args
		if (args.length != 1) {
			Usage();
		}

		// Get user port number
		int port = 0;
		try {
			port = Integer.parseInt(args[0]);
		} catch (NumberFormatException ne) {
			System.out.println("Error parsing port number");
			System.exit(1);
		}

		// Make new TCP connection
		TCPUtil tcpSocket = new TCPUtil(port);

		// Accept client connections
		while (true) {
			// TODO: does accept() need to return socket??
			Socket client = tcpSocket.accept();
			ProxyThread thread = new ProxyThread(tcpSocket);
			thread.start();
		}

	}

	/**
	 * Prints program use message and exits with error
	 */
	private static void Usage() {
		System.out.println("Usage: ./run <port number>");
		System.exit(1);
	}
}