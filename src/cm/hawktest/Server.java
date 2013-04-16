package cm.hawktest;

import java.io.IOException;
import java.net.SocketTimeoutException;

import no.ntnu.fp.net.co.Connection;
import cm.net.CloakedConnection;

public class Server {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws SocketTimeoutException 
	 */
	public static void main(String[] args) throws Exception {
		Connection server = new CloakedConnection(4295);
		Connection c = server.accept();
		while(true) {
			System.out.println(c.receive());
		}

	}

}
