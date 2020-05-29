import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class for handling requests for a single client-proxy connection
 */
public class ProxyThread extends Thread {
	private static final int PORT = 80;
	private static final int SECURE_PORT = 443;

	private Connection clientConnection;	// connection from client to proxy
	private Socket serverConnection;		// connection from proxy to server
	private Map<String, String> headers;	// headers of request

	/**
	 * Creates new thread for given established connection
	 * @param clientConnection TCP connection from client to proxy
	 */
	public ProxyThread(Connection clientConnection) {
		this.clientConnection = clientConnection;
		this.headers = null;
	}

	public void run() {
		// Get request line
		String initLine = clientConnection.readLine();

		// Output first line of request
		String date = new SimpleDateFormat("dd MMM HH:mm:ss").format(new Date());
		System.out.println(date + " - >>> " + initLine);

		// Decide if CONNECT or NON-CONNECT
		headers = getHeaders();
		if (initLine != null) {
			String[] elements = initLine.split(" ");
			if (elements[0].equals("CONNECT")) {
				handleConnect(initLine);
			} else {
				handleNonConnect(initLine);
			}
		}

		// TODO: close socket?
		try {
			clientConnection.close();
			serverConnection.close();
		} catch (IOException e) {
			System.out.println("Error closing connection(s)");
			e.printStackTrace();
		}
	}

	/**
	 * Tries to establish a connection between client and a server.
	 * If connection is successful, forwards data between client and server.
	 * @param initLine first line of the HTTP request proxy received to establish connection
	 */
	public void handleConnect(String initLine) {
		try {
			// Create connection
			String hostHeader = headers.get("host");
			serverConnection = new Socket(getHost(hostHeader), getPort(initLine, hostHeader));

			// Send HTTP response
			clientConnection.write("HTTP/1.0 200 OK\r\n\r\n");

			// Transfer bytes from client and server
			TunnelThread clientToServer = new TunnelThread(clientConnection.getSocket(), serverConnection);
			clientToServer.start();

			// Transfer bytes from server to client
			TunnelThread serverToClient = new TunnelThread(serverConnection, clientConnection.getSocket());
			serverToClient.start();
		} catch (Exception e) {
			// Send HTTP error response
			clientConnection.write("HTTP/1.0 502 Bad Gateway\r\n\r\n");
		}
	}


	/**
	 * Establishes proxy-server connection and forwards client request to server and
	 * server response to client
	 * @param initLine request line of HTTP client request
	 */
	public void handleNonConnect(String initLine) {
		try {
			// Create connection
			String hostHeader = headers.get("host");
			serverConnection = new Socket(getHost(hostHeader), getPort(initLine, hostHeader));

			StringBuffer request = new StringBuffer();

			// Edit request line
			String requestLine = initLine.replaceAll("HTTP\\/.*", "HTTP/1.0\r\n");
			request.append(requestLine);

			// Edit headers
			for (Map.Entry<String, String> h : headers.entrySet()) {
				request.append(h.getKey());
				if (h.getKey().equalsIgnoreCase("connection") ||
						h.getKey().equalsIgnoreCase("proxy-connection")) {
					request.append("close\r\n");
				} else {
					request.append(h.getValue() + "\r\n");
				}
			}

			// Pass header on to server
			clientConnection.write(request.toString());

			// Send rest of request to server
			InputStream fromClient = clientConnection.getInputStream();
			OutputStream toServer = serverConnection.getOutputStream();
			fromClient.transferTo(toServer);

			// Send server response to client
			InputStreamReader streamReader = new InputStreamReader(serverConnection.getInputStream());
			BufferedReader serverReader = new BufferedReader(streamReader);
			String nextLine = serverReader.readLine();
			while (nextLine != null) {
				if (nextLine.contains("HTTP/")) {
					nextLine.replaceAll("HTTP\\/.*", "HTTP/1.0\r\n");
				} else if (nextLine.toLowerCase().contains("connection") ||
						nextLine.toLowerCase().contains("proxy-connection")) {
					nextLine.replace("keep-alive", "close");
				}
				clientConnection.write(nextLine);
				nextLine = serverReader.readLine();
			}
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Parse headers from client request
	 * @return Map from header keyword to value for all specified headers
	 */
	public Map<String, String> getHeaders() {
		Map<String, String> headers = new LinkedHashMap<>();
		String nextLine = clientConnection.readLine();
		System.out.println(nextLine);
		while (nextLine != null && !nextLine.equals("\r\n")) {
			System.out.println(nextLine);
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