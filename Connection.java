import java.io.*;
import java.net.Socket;
import java.util.Arrays;

/**
 * A class representing a client-proxy connection.
 */
public class Connection {
	private Socket client;
	private BufferedReader reader; 	//TODO: may not be using all of these
	private DataOutputStream out;
	private DataInputStream input;

	/**
	 * Creates new connection between client and proxy
	 * @param client socket representing client connection
	 */
	public Connection(Socket client) {
		this.client = client;
		try {
			input = new DataInputStream(client.getInputStream());
			InputStreamReader streamReader = new InputStreamReader(input);
			reader = new BufferedReader(streamReader);
			out = new DataOutputStream(client.getOutputStream());
//			in = new DataInputStream(input);
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
			System.out.println("SADNESS Error: " + e.getMessage());
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
	 * Get input stream for accepted connection
	 * @return input stream for this connection
	 * @throws IOException if could not create input stream
	 */
	public InputStream getInputStream() throws IOException {
		return client.getInputStream();
	}

	/**
	 * Get output stream for accepted connection
	 * @return output stream for this connection
	 * @throws IOException if could not create output stream
	 */
	public OutputStream getOutputStream() throws IOException {
		return client.getOutputStream();
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