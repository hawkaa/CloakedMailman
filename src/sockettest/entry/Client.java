package sockettest.entry;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
	
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Socket s = new Socket("localhost", 4295);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

}
