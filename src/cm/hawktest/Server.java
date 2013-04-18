package cm.hawktest;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;

import no.ntnu.fp.net.co.Connection;
import cm.net.CloakedConnection;
import cm.util.Log;
import cm.util.Util;

public class Server {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws SocketTimeoutException 
	 */
	public static void main(String[] args) throws Exception {
		Util.ServerClient = new Log(new File("log/server_client.log"), "Server");
		Util.Herpaderp = new Log(new File("log/herpaderp.log"), "Server");
		CloakedConnection server = new CloakedConnection(4295);
		CloakedConnection c = server.accept();
		while(true) {
			Util.Herpaderp.d("Test", c.receive());
		}
		

	}

}
