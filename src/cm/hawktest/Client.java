package cm.hawktest;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Scanner;

import no.ntnu.fp.net.cl.ClException;
import no.ntnu.fp.net.co.Connection;
import cm.net.CloakedConnection;
import cm.net.InvalidStateException;
import cm.util.Log;

public class Client {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws UnknownHostException 
	 * @throws SocketTimeoutException 
	 * @throws ClException 
	 * @throws InvalidStateException 
	 */
	public static void main(String[] args) throws SocketTimeoutException, UnknownHostException, IOException, InvalidStateException, ClException {
		Log.setLogFile(new File("log/server_client.log"));
		
		Connection con = new CloakedConnection();
		con.connect(InetAddress.getByName("localhost"), 4295);
		con.send("Petter er kul");
		/*String input;
		Scanner sc = new Scanner(System.in);
		while (true) {
			input = sc.next();
			System.out.println("Sending: " + input);
			con.send(input);
		}*/

	}

}
