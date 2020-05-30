import java.io.*;
import java.lang.reflect.Array;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Arrays;
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
		System.out.println("Printed initial line");

		// Decide if CONNECT or NON-CONNECT
		headers = getHeaders();
		if (initLine != null) {
			String[] elements = initLine.split(" ");
			String host = getHost(headers.get("host"));
			int port = getPort(initLine, headers.get("host"));
			if (elements[0].equals("CONNECT")) {
				handleConnect(host, port);
			} else {
				handleNonConnect(host, port, initLine);
			}
		}

		// TODO: close socket?
//		try {
//			clientConnection.close();
//			serverConnection.close();
//		} catch (IOException e) {
//			System.out.println("Error closing connection(s)");
//			e.printStackTrace();
//		}
	}

	/**
	 * Tries to establish a connection between client and a server.
	 * If connection is successful, forwards data between client and server.
	 */
	public void handleConnect(String host, int port) {
		try {
			System.out.println("Handling CONNECT request");
			// Create connection
			serverConnection = new Socket(host, port);

			// Send HTTP response
			clientConnection.write("HTTP/1.0 200 OK\r\n\r\n");

			// Transfer bytes from client and server
			TunnelThread clientToServer = new TunnelThread(clientConnection.getSocket(), serverConnection);


			// Transfer bytes from server to client
			TunnelThread serverToClient = new TunnelThread(serverConnection, clientConnection.getSocket());

			clientToServer.start();
			serverToClient.start();
			System.out.println("Done with CONNECT request");
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
	public void handleNonConnect(String host, int port, String initLine) {
		try {
			// Create connection
			serverConnection = new Socket(host, port);

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
					System.out.println(nextLine);
				} else if (nextLine.toLowerCase().contains("connection") ||
						nextLine.toLowerCase().contains("proxy-connection")) {
					nextLine.replace("keep-alive", "close");
				}
				clientConnection.write(nextLine);
				nextLine = serverReader.readLine();
			}
//			// Clean up
//			clientConnection.close();
//			serverConnection.close();
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
		System.out.println("Handling headers");
		Map<String, String> headers = new LinkedHashMap<>();
		System.out.println("About to try and read ok go");
		String nextLine = clientConnection.readLine();
		System.out.println(nextLine);
		while (nextLine != null && !nextLine.equals("")) {
			System.out.println("Header for to be parsed: " + nextLine);
			String[] elements = nextLine.split(":", 2);
			System.out.println(Arrays.toString(elements));
			headers.put(elements[0].toLowerCase().trim(), elements[1].trim());
			nextLine = clientConnection.readLine();
		}
		System.out.println("Done handling headers");
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