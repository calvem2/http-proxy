import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class for handling a request for a single client-proxy connection
 */
public class ProxyThread extends Thread {
	private static final int PORT = 80;
	private static final int SECURE_PORT = 443;

	private Connection clientConnection;			// connection from client to proxy
	private Socket serverConnection;				// connection from proxy to server
	private LinkedHashMap<String, String> headers;	// headers of request
	private String host;							// value of host header in request received

	String initLine; // todo: change back to local

	/**
	 * Creates new thread for given established connection
	 * @param clientConnection TCP connection from client to proxy
	 */
	public ProxyThread(Connection clientConnection) {
		this.clientConnection = clientConnection;
		this.headers = null;
	}

	/**
	 * Handles CONNECT and non-connect requests between client and a server
	 */
	public void run() {
		// Get request line
		initLine = clientConnection.readLine();
		headers = getHeaders();
		if (initLine != null) {
			// Output first line of request
			String date = new SimpleDateFormat("dd MMM HH:mm:ss").format(new Date());
			System.out.println(date + " - >>> " + initLine);

			// Determine request type: CONNECT or NON-CONNECT
			String[] elements = initLine.split(" ");
			int port = getPort(initLine, this.host);
			if (elements[0].equals("CONNECT")) {
				handleConnect(getHost(), port);
			} else {
				handleNonConnect(getHost(), port, initLine);
			}
		}
//		try {
//			clientConnection.close();
//			serverConnection.close();
//		} catch (IOException e) {
//			System.out.println("SOCKET CLOSED");
//		}
	}

	/**
	 * Tries to establish a connection between client and a server.
	 * If connection is successful, forwards data between client and server.
	 * @param host hostname to connect to
	 * @param port port number to connect to
	 */
	public void handleConnect(String host, int port) {
		try {
			// Create connection
			serverConnection = new Socket(host, port);

			// Send HTTP response
			clientConnection.write("HTTP/1.0 200 OK\r\n\r\n");

			// Transfer bytes from client and server
			TunnelThread clientToServer = new TunnelThread(clientConnection.getSocket(), serverConnection);

			// Transfer bytes from server to client
			TunnelThread serverToClient = new TunnelThread(serverConnection, clientConnection.getSocket());

			// Start the threads
			clientToServer.start();
			serverToClient.start();
		} catch (Exception e) {
			// Send HTTP error response
			clientConnection.write("HTTP/1.0 502 Bad Gateway\r\n\r\n");
		}
	}

	/**
	 * Establishes proxy-server connection and forwards client request to server and
	 * server response back to client
	 * @param host hostname to connect to
	 * @param port port number to connect to
	 * @param initLine request line of HTTP client request
	 */
	public void handleNonConnect(String host, int port, String initLine) {
		try {
			// Create connection
			serverConnection = new Socket(host, port);
			DataOutputStream toServer = new DataOutputStream(serverConnection.getOutputStream());
			StringBuilder request = new StringBuilder();

			// Edit request line
			request.append(initLine.replaceAll("HTTP/1.1", "HTTP/1.0")).append("\r\n");

			// Edit headers
			for (Map.Entry<String, String> h : headers.entrySet()) {
				request.append(h.getKey()).append(": ");
				if (h.getKey().equalsIgnoreCase("connection") ||
						h.getKey().equalsIgnoreCase("proxy-connection")) {
					request.append("close\r\n");
				} else {
					request.append(h.getValue()).append("\r\n");
				}
			}
			request.append("\r\n");
			// Send request line and headers to server
			toServer.write(request.toString().getBytes());

			// Send rest of request to server
			while (clientConnection.getReader().ready()) {
				String payloadLine = clientConnection.readLine();
				payloadLine += "\r\n";
				toServer.write(payloadLine.getBytes());
			}
			toServer.write("\r\n\r\n".getBytes());

			// Send response back to client
			InputStream serverIn = serverConnection.getInputStream();

			byte[] data = new byte[256];
			int read = serverIn.read(data);
			while (read != -1) {
				clientConnection.getOutputStream().write(data, 0, read);
				clientConnection.getOutputStream().flush();
				read = serverIn.read(data);
			}
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				// Close the connections
				clientConnection.close();
				serverConnection.close();
			} catch (Exception e) {
				System.out.println("Error: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Parse headers from client request
	 * @return Map from header keyword to value for all specified headers
	 */
	public LinkedHashMap<String, String> getHeaders() {
		LinkedHashMap<String, String> headers = new LinkedHashMap<>();
		String nextLine = clientConnection.readLine();
		while (nextLine != null && !nextLine.equals("")) {
			String[] elements = nextLine.split(":", 2);
			headers.put(elements[0].trim(), elements[1].trim());

			if (elements[0].equalsIgnoreCase("host")) {
				this.host = elements[1].trim();
			}
			nextLine = clientConnection.readLine();
		}

		return headers;
	}

	/**
	 * Parse host name from client request
	 * @return host name in hostHeader in the form 'host' or 'host:port'
	 */
	private String getHost() {
		// Split host from port
		return this.host.split(":")[0];
	}

	/**
	 * Parse specified port from client request
	 * @param initLine first line of the client request
	 * @param hostHeader value of host header from client request in the form 'host' or 'host:port'
	 * @return specified port in hostHeader or initLine if none is specified in header; otherwise returns
	 * default port based on transport protocol (or absence of it) if no port is specified in the request
	 */
	private int getPort(String initLine, String hostHeader) {
		String[] elements = hostHeader.split(":");
		// Check if value is in format hostname:port
		if (elements.length == 2) {
			return Integer.parseInt(elements[1]);
		}

		// Check initial line for port number
		String uri = initLine.split(" ")[1];

		// Separate transport from rest of uri
		String[] uriElements = uri.split("://");
		String[] hostAndPort;
		// Split host and port
		if (uriElements.length == 1) {
			hostAndPort = uriElements[0].split(":");
		} else {
			hostAndPort = uriElements[1].split(":");
		}
		// Return specified port or proper default port
		if (hostAndPort.length == 2) {
			return Integer.parseInt(hostAndPort[1]);
		} else if (uriElements.length == 2 && uriElements[0].trim().equalsIgnoreCase("https")) {
			return SECURE_PORT;
		} else {
			return PORT;
		}
	}
}