import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPUtil {
	private ServerSocket tcp;
	private Socket socket;

	/**
	 * Creates new TCP connection with the specified port
	 * @param port
	 */
	public TCPUtil(int port) {
		try {
			tcp = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("Error: Could not open socket");
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Accept a connection on the tcp socket
	 */
	public Socket accept() {
		try {
			socket = tcp.accept();
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
		return socket;
	}

	/**
	 * Read next line of input from socket
	 * @return line read from socket
	 */
	public String readLine() {
		String line = null;
		try {
			InputStreamReader streamReader = new InputStreamReader(socket.getInputStream());
			BufferedReader reader = new BufferedReader(streamReader);
			line = reader.readLine();
		} catch (IOException e) {
			// TODO: handle exception
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
			// TODO: handle exception
		}
	}

	/**
	 * Get input stream for accepted connection
	 * @requires socket != null, i.e. accept() has been called
	 * @return input stream for this connection
	 * @throws IOException if could not create input stream
	 */
	public InputStream getInputStream() throws IOException {
		return socket.getInputStream();
	}

	/**
	 * Get output stream for accepted connection
	 * @requires socket != null, i.e. accept() has been called
	 * @return output stream for this connection
	 * @throws IOException if could not create output stream
	 */
	public OutputStream getOutputStream() throws IOException {
		return socket.getOutputStream();
	}


}