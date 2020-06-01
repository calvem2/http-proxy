import java.io.*;
import java.net.Socket;

/**
 * A class representing a client-proxy connection.
 */
public class Connection {
	private Socket client;
	private BufferedReader reader;
	private DataOutputStream out;

	/**
	 * Creates new connection between client and proxy
	 * @param client socket representing client connection
	 */
	public Connection(Socket client) {
		this.client = client;
		try {
			InputStreamReader streamReader = new InputStreamReader(new DataInputStream(client.getInputStream()));
			reader = new BufferedReader(streamReader);
			out = new DataOutputStream(client.getOutputStream());
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Read next line of input from client
	 * @return line read from client
	 */
	public String readLine() {
		String line = null;
		try {
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
			out.write(buf.getBytes());
			out.flush();
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
	 */
	public OutputStream getOutputStream() {
		return this.out;
	}

	public BufferedReader getReader() {
		return this.reader;
	}

	/**
	 * Closes client-proxy connection
	 * @throws IOException if there's an error while attempting to close
	 */
	public void close() throws IOException {
		client.close();
	}
}