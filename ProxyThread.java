import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ProxyThread extends Thread {
	private static final int PORT = 80;
	private static final int SECURE_PORT = 443;

	private TCPUtil clientConnection;	// connection from client to proxy
	private Socket serverConnection;	// connection from proxy to server

	/**
	 * Creates new thread for given established connection
	 * @param clientConnection TCP connection from client to proxy
	 */
	public ProxyThread(TCPUtil clientConnection) {
		this.clientConnection = clientConnection;
	}

	public void run() {
		// Decide if CONNECT or NON-CONNECT
		String initLine = clientConnection.readLine();
		Map<String, String> headers = getHeaders();
		if (initLine != null) {
			String[] elements = initLine.split(" ");
			if (elements[0].equals("CONNECT")) {
				handleConnect(initLine, headers);
			} else {
				handleNonConnect();
			}
		}
	}

	public void handleConnect(String initLine, Map<String, String> headers) {
		try {
			// Create connection
			String hostHeader = headers.get("host");
			serverConnection = new Socket(getHost(hostHeader), getPort(initLine, hostHeader));

			if (serverConnection.isConnected()) {
				// Send HTTP response
				clientConnection.write("HTTP/1.0 200 OK\r\n\r\n");

				// Transfer bytes from client and server
				InputStream fromClient = clientConnection.getInputStream();
				OutputStream toServer = serverConnection.getOutputStream();
				fromClient.transferTo(toServer);

				// Transfer bytes from server to client
				InputStream fromServer = serverConnection.getInputStream();
				OutputStream toClient = clientConnection.getOutputStream();
				fromServer.transferTo(toClient);
			} else {
				// Send HTTP error response
				clientConnection.write("HTTP/1.0 502 Bad Gateway\r\n\r\n");
			}

		} catch (Exception e) {
			// TODO: handle this

		}
	}

	public void handleNonConnect() {

	}

	/**
	 * Parse headers from client request
	 * @return Map from header keyword to value for all specified headers
	 */
	public Map<String, String> getHeaders() {
		Map<String, String> headers = new HashMap<>();
		String nextLine = clientConnection.readLine();
		while (!nextLine.equals("\r\n")) {
			String[] elements = nextLine.split(":", 1);
			headers.put(elements[0].toLowerCase().trim(), elements[1].trim());
			nextLine = clientConnection.readLine();
		}
		return headers;
	}

	/**
	 * Parse host name from client request
	 * @param hostHeader value of host header from client request
	 * @return host name in hostHeader
	 */
	private String getHost(String hostHeader) {
		return hostHeader.split(":")[0];
	}

	/**
	 * Parse specified port from client request
	 * @param initLine first line of the client request
	 * @param hostHeader value of host header from client request
	 * @return specified port in hostHeader or initLine if none is specified in header;
	 * default port if no port is specified in the request
	 */
	private int getPort(String initLine, String hostHeader) {
		String[] elements = hostHeader.split(":");
		// check if value is in format hostname:port
		if (elements.length == 2) {
			return Integer.parseInt(elements[1]);
		}

		// check initial line for port number
		String uri = initLine.split(" ")[1];

		// separate transport from rest of uri
		String[] uriElements = uri.split("://");
		String[] hostAndPort;
		// split host and port
		if (uriElements.length == 1) {
			hostAndPort = uriElements[0].split(":");
		} else {
			hostAndPort = uriElements[1].split(":");
		}

		// return specified port or proper default port
		if (hostAndPort.length == 2) {
			return Integer.parseInt(hostAndPort[1]);
		} else if (uriElements.length == 2 && uriElements[1].equals("https")) {
			return SECURE_PORT;
		} else {
			return PORT;
		}
	}

}