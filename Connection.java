import java.io.*;
import java.net.Socket;
import java.util.Arrays;

/**
 * A class representing a client-proxy connection.
 */
public class Connection {
	private Socket client;
	BufferedReader reader;

	/**
	 * Creates new connection between client and proxy
	 * @param client socket representing client connection
	 */
	public Connection(Socket client) {
		this.client = client;
		try {
			InputStreamReader streamReader = new InputStreamReader(client.getInputStream());
			reader = new BufferedReader(streamReader);
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
//			System.out.println("Trying to read");
//			InputStreamReader streamReader = new InputStreamReader(client.getInputStream());
//			BufferedReader reader = new BufferedReader(streamReader);
			line = reader.readLine();
//			System.out.println("Read the line: " + line);
//			char[] buf = new char[256];
//			int read = reader.read(buf, 0, 256);
//			System.out.println("Read the line: " + Arrays.toString(buf));
//			System.out.println("Read " + read + " bytes");
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

	/**
	 * Closes client-proxy connection
	 * @throws IOException if there's an error while attempting to close
	 */
	public void close() throws IOException {
		client.close();
	}


}