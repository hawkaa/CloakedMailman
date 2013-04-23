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
import cm.util.Util;

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
		Util.ServerClient = new Log(new File("log/server_client.log"), "Client");
		Util.Herpaderp = new Log(new File("log/herpaderp.log"), "Client");
		
		CloakedConnection con = new CloakedConnection();
		con.connect(InetAddress.getByName("localhost"), 4295);
		//Util.Herpaderp.d("Test", "Sending petter er kul");
		/*con.send("Petter er kul");
		con.send("Petter er dust");*/
		
		for(int i = 1; i<=100; ++i) {
			con.send(new Integer(i).toString());
		
		}
		con.close();
		//Util.Herpaderp.d("Test", "Send success!");
		/*String input;
		Scanner sc = new Scanner(System.in);
		while (true) {
			input = sc.next();
			System.out.println("Sending: " + input);
			con.send(input);
		}*/

	}

}
