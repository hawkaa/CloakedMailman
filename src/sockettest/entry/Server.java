package sockettest.entry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;

public class Server {
	
	private static final int PORT = 4295;
	
	private final ServerSocket server;
	
	private ArrayList<Client> clients = new ArrayList<Client>();
	
	public Server() throws IOException{
		this.server = new ServerSocket(PORT);
	}
	
	public void run() {
		Thread listener = new Thread() {
			private Socket s;
			public void run() {
				while (true) {
					try {
						this.s = Server.this.server.accept();
						Client c = new Client(s);
						Server.this.clients.add(c);
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
			};
		};
		listener.start();
		Random rn = new Random();
		while(true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for(Client c: this.clients) {
				String message = String.valueOf(rn.nextInt());
				System.out.println(message);
				c.send(message);
			}
		}
	}
	
	private class Client {
		private Socket socket;
		private BufferedReader br;
		private PrintWriter out;
		public Client(Socket s) {
			this.socket = s;
			try {
				this.br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
				this.out = new PrintWriter(this.socket.getOutputStream(),true);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			//Starting recieve thread
			Thread recieve = new Thread() {
				public void run() {
					try {
						String message = br.readLine();
						Server.this.recieve(Client.this, message);
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
			};
			recieve.start();
			
			
		}
		public void send(String message) {
			out.println(message);
		}
		
	}
	
	private void recieve(Client client, String message) {
		System.out.println(message);
	}
	
	
	public static void main(String[] args) {
		Server server;
		try {
			server = new Server();
			server.run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		

	}

}
