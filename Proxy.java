import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * HTTP Proxy for handling requests between clients and servers.
 */
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
		ServerSocket proxy = TCPUtils.getProxy(port);

		// Accept client connections
		String date = new SimpleDateFormat("dd MMM HH:mm:ss").format(new Date());
		System.out.println(date + " - Proxy listening on " + proxy.getInetAddress().getHostAddress() + ":" + port);
		while (true) {
			Connection client = TCPUtils.accept(proxy);
			ProxyThread thread = new ProxyThread(client);
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