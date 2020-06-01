import java.io.*;
import java.lang.reflect.Array;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Arrays;
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
		System.out.println("Done handling request: " + initLine);

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
	 * @param host hostname to connect to
	 * @param port port number to connect to
	 */
	public void handleConnect(String host, int port) {
		try {
			System.out.println("Handling CONNECT");
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
	 * server response back to client
	 * @param host hostname to connect to
	 * @param port port number to connect to
	 * @param initLine request line of HTTP client request
	 */
	public void handleNonConnect(String host, int port, String initLine) {
		try {
			System.out.println("Handling non-connect");
			// Create connection
			serverConnection = new Socket(host, port);
			DataOutputStream toServer = new DataOutputStream(serverConnection.getOutputStream());

			StringBuilder request = new StringBuilder();

			// Edit request line
			request.append(initLine.replaceAll("HTTP/1.1", "HTTP/1.0")).append("\r\n");
//			request.append(requestLine);
//			toServer.write(requestLine.getBytes());

			// Edit headers
			for (Map.Entry<String, String> h : headers.entrySet()) {
//				StringBuilder header = new StringBuilder();
				request.append(h.getKey()).append(": ");
				if (h.getKey().equalsIgnoreCase("connection") ||
						h.getKey().equalsIgnoreCase("proxy-connection")) {
					request.append("close\r\n");
				} else {
					request.append(h.getValue()).append("\r\n");
				}
//				toServer.write(header.toString().getBytes());
			}
			request.append("\r\n");
			System.out.println("Headers sent to server for " + initLine + ": ");
			System.out.println(request.toString());
//			toServer.write("\r\n".getBytes());
			// Send request line and headers to server
			toServer.write(request.toString().getBytes());

			// Send rest of request to server
			System.out.println("Sending payload to server");
//			InputStream fromClient = clientConnection.getInputStream();
//			fromClient.transferTo(toServer);
//			String payloadLine = clientConnection.readLine();
			while (clientConnection.getReader().ready()) {
				String payloadLine = clientConnection.readLine();
				payloadLine += "\r\n";
				toServer.write(payloadLine.getBytes());
//				System.out.println("Line sent: " + payloadLine);
//				payloadLine = clientConnection.readLine();
				System.out.println("Payload line sent for " + initLine + payloadLine);
			}
			toServer.write("\r\n\r\n".getBytes());

//			byte[] payload = new byte[256];
//			int bytesRead = clientConnection.input.read(payload);
//			while (bytesRead != -1) {
//				toServer.write(payload, 0, bytesRead);
//				toServer.flush();
//				bytesRead = clientConnection.input.read(payload);
//			}

			// Send response back to client
			// todo: condense to one line if not using method that uses input stream
			InputStream serverIn = serverConnection.getInputStream();
//			InputStreamReader streamReader = new InputStreamReader(serverIn);
//
//			BufferedReader serverReader = new BufferedReader(streamReader);
			System.out.println("Sending response back to client for: " + initLine);
			// wow don't need to edit the headers wtf i am rewriting that spec holy crap
			// https://us.edstem.org/courses/403/discussion/74415
//			String nextLine = serverReader.readLine();
//			while (nextLine != null && !nextLine.equals("")) {
////				if (nextLine.contains("HTTP/1.1")) {
////					nextLine = nextLine.replaceAll("HTTP/1.1", "HTTP/1.0");
//////					System.out.println(nextLine);
////				} else if (nextLine.toLowerCase().contains("connection: keep-alive") ||
////						nextLine.toLowerCase().contains("proxy-connection: keep-alive")) {
////					nextLine = nextLine.replaceAll("keep-alive", "close");
////				}
//
//				clientConnection.write(nextLine + "\r\n");
//				System.out.println("Line sent for " + initLine + ": " + nextLine);
//				nextLine = serverReader.readLine();
//			}
//			clientConnection.write("\r\n");

			byte[] data = new byte[256];
			int read = serverIn.read(data);
			while (read != -1) {
				clientConnection.getOutputStream().write(data, 0, read);
				System.out.println("Line sent: " + new String(data));
				clientConnection.getOutputStream().flush();
				read = serverIn.read(data);
			}


//			serverIn.transferTo(clientConnection.out);

//			// Clean up
//			clientConnection.close();
//			serverConnection.close();
			System.out.println("Done with non connect");
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}

	}

	/**
	 * Parse headers from client request
	 * @return Map from header keyword to value for all specified headers
	 */
	public LinkedHashMap<String, String> getHeaders() {
		LinkedHashMap<String, String> headers = new LinkedHashMap<>();
//		System.out.println("About to try and read ok go");
		String nextLine = clientConnection.readLine();
//		System.out.println("Header received: " + nextLine);
		while (nextLine != null && !nextLine.equals("")) {

//			System.out.println("Header for to be parsed: " + nextLine);
			String[] elements = nextLine.split(":", 2);
//			System.out.println(Arrays.toString(elements));
			headers.put(elements[0].trim(), elements[1].trim());
//			System.out.println("Header received: " + nextLine);

			if (elements[0].equalsIgnoreCase("host")) {
				this.host = elements[1].trim();
			}
			nextLine = clientConnection.readLine();
		}
		System.out.println("Done handling headers");
		return headers;
	}

	/**
	 * Parse host name from client request
	 * @return host name in hostHeader in the form 'host' or 'host:port'
	 */
	private String getHost() {
		// Split host from port
		System.out.println("Host determined: " + this.host.split(":")[0]);
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
		// check if value is in format hostname:port
		if (elements.length == 2) {
			System.out.println("Determined port #: " + Integer.parseInt(elements[1]));
			return Integer.parseInt(elements[1]);
		}

		// check initial line for port number
		String uri = initLine.split(" ")[1];
		System.out.println(uri);

		// separate transport from rest of uri
		String[] uriElements = uri.split("://");
		String[] hostAndPort;
		// split host and port
		if (uriElements.length == 1) {
			hostAndPort = uriElements[0].split(":");
		} else {
			hostAndPort = uriElements[1].split(":");
		}
		System.out.println(Arrays.toString(uriElements));
		// return specified port or proper default port
		if (hostAndPort.length == 2) {
			System.out.println("Determined port #: " + Integer.parseInt(hostAndPort[1]));
			return Integer.parseInt(hostAndPort[1]);
		} else if (uriElements.length == 2 && uriElements[0].trim().equalsIgnoreCase("https")) {
			System.out.println("Determined port #: " + SECURE_PORT);
			return SECURE_PORT;
		} else {
			System.out.println("Determined port #: " + PORT);
			return PORT;
		}
	}

}