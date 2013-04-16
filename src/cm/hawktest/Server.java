package cm.hawktest;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;

import no.ntnu.fp.net.co.Connection;
import cm.net.CloakedConnection;
import cm.util.Log;

public class Server {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws SocketTimeoutException 
	 */
	public static void main(String[] args) throws Exception {
		Log.setLogFile(new File("log/server_client.log"));
		Connection server = new CloakedConnection(4295);
		Connection c = server.accept();
		while(true) {
			Log.d("Server", "Received: " + c.receive());
		}

	}

}
