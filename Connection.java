import java.io.*;
import java.net.Socket;

/**
 * A class representing a client-proxy connection.
 */
public class Connection {
	private Socket client;

	/**
	 * Creates new connection between client and proxy
	 * @param client socket representing client connection
	 */
	public Connection(Socket client) {
		this.client = client;
	}

	/**
	 * Read next line of input from client
	 * @return line read from client
	 */
	public String readLine() {
		String line = null;
		try {
			InputStreamReader streamReader = new InputStreamReader(client.getInputStream());
			BufferedReader reader = new BufferedReader(streamReader);
			line = reader.readLine();
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
		return line;
	}

	/**
	 * Write to client
	 * @param buf String to send to client
	 */
	public void write(String buf) {
		try {
			OutputStream os = getOutputStream();
			os.write(buf.getBytes());
		} catch (IOException e){
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Returns this connection's socket
	 * @return client-proxy socket
	 */
	public Socket getSocket() {
		return this.client;
	}

	/**
	 * Get output stream for accepted connection
	 * @return output stream for this connection
	 * @throws IOException if could not create output stream
	 */
	public OutputStream getOutputStream() throws IOException {
		return client.getOutputStream();
	}


}