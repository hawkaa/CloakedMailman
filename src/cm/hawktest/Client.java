package cm.hawktest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Scanner;

import no.ntnu.fp.net.co.Connection;
import cm.net.CloakedConnection;

public class Client {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws UnknownHostException 
	 * @throws SocketTimeoutException 
	 */
	public static void main(String[] args) throws SocketTimeoutException, UnknownHostException, IOException {
		Connection con = new CloakedConnection();
		con.connect(InetAddress.getByName("localhost"), 4295);
		System.out.println("Connected");
		String input;
		Scanner sc = new Scanner(System.in);
		while (true) {
			input = sc.next();
			System.out.println("Sending: " + input);
			con.send(input);
		}

	}

}
