/*
 * Created on Oct 27, 2004
 */
package cm.net;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

import cm.util.Log;
import cm.util.Util;

import no.ntnu.fp.net.cl.ClException;
import no.ntnu.fp.net.cl.KtnDatagram;
import no.ntnu.fp.net.cl.KtnDatagram.Flag;
import no.ntnu.fp.net.co.AbstractConnection;
import no.ntnu.fp.net.co.Connection;

/**
 * Implementation of the Connection-interface. <br>
 * <br>
 * This class implements the behaviour in the methods specified in the interface
 * {@link Connection} over the unreliable, connectionless network realised in
 * {@link ClSocket}. The base class, {@link AbstractConnection} implements some
 * of the functionality, leaving message passing and error handling to this
 * implementation.
 * 
 * @author Sebjørn Birkeland and Stein Jakob Nordbø
 * @see no.ntnu.fp.net.co.Connection
 * @see no.ntnu.fp.net.cl.ClSocket
 */
public class CloakedConnection extends AbstractConnection {

    /** Keeps track of the used ports for each server port. */
    private static Map<Integer, Boolean> usedPorts = Collections.synchronizedMap(new HashMap<Integer, Boolean>());
    

    /**
     * Initialise initial sequence number and setup state machine.
     * 
     * @param myPort
     *            - the local port to associate with this connection
     */
    public CloakedConnection() {
    	// Generating random port number
    	this((int)(10000 + (Math.random() * 10000)));
    	
    }
    public CloakedConnection(int myPort) {
    	// Setting state to closed
    	this.state = State.CLOSED;
    	// Setting port
    	this.myPort = myPort;
    	
    	this.myAddress = getIPv4Address();
    	Util.ServerClient.d("Constructor", "Creating connection with port number " + this.myPort + ", ip " + this.myAddress + ", sequence number " + this.nextSequenceNo);
    }

    private String getIPv4Address() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    /**
     * Establish a connection to a remote location.
     * 
     * @param remoteAddress
     *            - the remote IP-address to connect to
     * @param remotePort
     *            - the remote portnumber to connect to
     * @throws IOException
     *             If there's an I/O error.
     * @throws java.net.SocketTimeoutException
     *             If timeout expires before connection is completed.
     * @throws InvalidStateException 
     * @throws ClException 
     * @see Connection#connect(InetAddress, int)
     */
    public void connect(InetAddress remoteAddress, int remotePort) throws IOException,
            SocketTimeoutException, InvalidStateException, ClException {
        /*throw new NotImplementedException();*/
    	
    	// Setter adresse og port
    	this.remoteAddress = remoteAddress.getHostAddress();
    	this.remotePort = remotePort;
    	
    	Util.ServerClient.d("Connect", "Trying to connect to " + this.remoteAddress + ":" + this.remotePort);
    	
    	// State må være CLOSED når connect() kalles.
    	if(this.state != State.CLOSED)
    	{
			throw new InvalidStateException("This call requires the connection to be CLOSED");
    	}
    	
    	// Sender SYN
    	KtnDatagram packet = constructInternalPacket(Flag.SYN);
    	Util.ServerClient.d("Connect", "Sending SYN: " + Util.dumpDatagram(packet));
    	simplySendPacket(packet);
    	Util.ServerClient.d("Connect", "Waiting for ACK");
    	KtnDatagram ackReceive = receiveAck();
    	this.state = State.SYN_SENT;
    	
    	// Får man ikke noe svar, har det oppstått en time out
    	if(ackReceive == null)
    	{
    		Util.ServerClient.d("Connect", "ACK timed out");
    		throw new SocketTimeoutException("Socket timed out");
    	}
    	Util.ServerClient.d("Connect", "ACK received: " + Util.dumpDatagram(ackReceive));
    	
    	
    	// Mottar man SYN_ACK, er connect() oppnådd
    	if(ackReceive.getFlag() != Flag.SYN_ACK)
    	{
    		Util.ServerClient.d("Connect", "ACK received, but it was not SYNACK.");
    		throw new SocketTimeoutException("Socket timed out");
    		
    	}
    	
    	// OK, set new data
    	this.remoteAddress = ackReceive.getSrc_addr();
    	this.remotePort = ackReceive.getSrc_port();
    	this.lastValidPacketReceived = ackReceive;
    	this.state = State.ESTABLISHED;
		//KtnDatagram ackPacket = constructInternalPacket(Flag.ACK);
		//Util.ServerClient.d("Connect", "Sending ACK 'simply': " + Util.dumpDatagram(ackPacket));
		//simplySendPacket(ackPacket);
    	this.sendAck(ackReceive, false);
		
		Util.ServerClient.d("Connect", "Connection established.");
    	
    	
    	
    }

    /**
     * Listen for, and accept, incoming connections.
     * 
     * @return A new ConnectionImpl-object representing the new connection.
     * @throws InvalidStateException 
     * @see Connection#accept()
     */
    public CloakedConnection accept() throws IOException, SocketTimeoutException, InvalidStateException {
    	/*
    	 * Its essential that the connection is in the correct states
    	 */
    	if (this.state != State.CLOSED){
    		throw new InvalidStateException("This call requires the connection to be CLOSED");
    	}
    	
    	// Set the state
    	this.state = State.LISTEN;
    	
    	Util.ServerClient.d("Accept", "Listening for connection on " + this.myAddress + ":" + this.myPort);
    	
    	while (true) {
    		// Receive datagram
    		KtnDatagram dg = this.receivePacket(true);
    		
    		// If datagram not set
    		if (dg == null) {
    			continue;
    		}
    		Util.ServerClient.d("Accept", "Received package: " + Util.dumpDatagram(dg));
    		// Check if syn. If not SYN, try again
    		if(dg.getFlag() != Flag.SYN) {
    			continue;
    		}
    		
    		// Allright, we've received a SYN package.
    		Util.ServerClient.d("Accept", "Received SYN package from " + dg.getSrc_addr() + ":" + dg.getSrc_port());
    		this.lastValidPacketReceived = dg;
    		
    		// Creating a new connection with random port number
    		CloakedConnection con = new CloakedConnection();
    		
    		// Setting the connection state to SYN RCVD
    		con.state = State.SYN_RCVD;
    		
    		// Setting connection parameters
    		con.remoteAddress = dg.getSrc_addr();
    		con.remotePort = dg.getSrc_port();
    		
    		// Send SYNACK to initiator
    		Util.ServerClient.d("Accept", "Sending SYNACK to initiator");
    		con.sendAck(dg, true);
    		
    		// Awaiting ACK
    		KtnDatagram ack = con.receiveAck();
    		if(ack == null) {
    			Util.ServerClient.d("Accept", "Connection ack timed out. Restarting.");
    		}
    		Util.ServerClient.d("Accept", "Received packet: " + Util.dumpDatagram(ack));
    		this.lastValidPacketReceived = ack;
    		// Connection established
    		con.state = State.ESTABLISHED;
    		Util.ServerClient.d("Accept", "Connection established, returning connection");
    		return con;
    		
    	}
    	
    }
	
    /**
     * Send a message from the application.
     * 
     * @param msg
     *            - the String to be sent.
     * @throws ConnectException
     *             If no connection exists.
     * @throws IOException
     *             If no ACK was received.
     * @see AbstractConnection#sendDataPacketWithRetransmit(KtnDatagram)
     * @see no.ntnu.fp.net.co.Connection#send(String)
     */
    public void send(String msg) throws ConnectException, IOException {
        if(this.state != State.ESTABLISHED) {
        	throw new ConnectException("Connection is not established");
        }
        Util.ServerClient.d("Send", "Last successfull received datagram was: " + Util.dumpDatagram(this.lastValidPacketReceived));
        KtnDatagram d = this.constructDataPacket(msg);
        System.err.println(d.getPayload());
        Util.ServerClient.d("Send", "Trying to send: " + Util.dumpDatagram(d));
        KtnDatagram ack = this.sendDataPacketWithRetransmit(d);
        if (ack == null) {
        	Util.ServerClient.d("Send", "Ack not received, packet timed out");
        	throw new IOException("Did not receive ACK on packet.");
        }
    	Util.ServerClient.d("Send", "Ack received, packet sent successfully");
        
    }

    /**
     * Wait for incoming data.
     * 
     * @return The received data's payload as a String.
     * @throws InvalidStateException 
     * @see Connection#receive()
     * @see AbstractConnection#receivePacket(boolean)
     * @see AbstractConnection#sendAck(KtnDatagram, boolean)
     */
    public String receive() throws ConnectException, IOException, InvalidStateException {
    	/*
    	 * Its essential that the connection is in the correct state
    	 */
    	if (this.state != State.ESTABLISHED){
    		throw new InvalidStateException("This call requires the connection to be ESTABLISHED");
    	}
    	
    	Util.ServerClient.d("Receive", "Starting to wait for packet");
    	
    	while (true) {
    		// Receive datagram
    		KtnDatagram dg = this.receivePacket(false);
    		
    		// If datagram not set
    		if (dg == null) {
    			continue;
    		}
    		
    		Util.ServerClient.d("Receive", "Received package: " + Util.dumpDatagram(dg));
    		
    		
    		// Package received, yay!
    		if(!this.isValid(dg)){
    			Util.ServerClient.d("Receive", "Package invalid, ignoring");
    			// Package not valid, ignore it.
    			continue;
    		}
    		
    		Util.ServerClient.d("Receive", "Package is valid, returning payload \"" + (String)dg.getPayload() + "\" to callee. Sending ACK on package.");
    		// Package is valid, send ack on package
    		this.sendAck(dg, false);
    		return (String) dg.getPayload();
    		
    	}
    }

    /**
     * Close the connection.
     * 
     * @see Connection#close()
     */
    public void close() throws IOException {
    	this.state = State.FIN_WAIT_1;
    	Util.ServerClient.d("Close", "Initiating close sequence");
    	
    	KtnDatagram dg = this.constructInternalPacket(Flag.FIN);
    	
    	Util.ServerClient.d("Close", "Sending: " + Util.dumpDatagram(dg));
    	this.sendDataPacketWithRetransmit(dg);
    	this.state = State.FIN_WAIT_2;
    	
    	KtnDatagram fin;
    	while (true) {
    		fin = this.receivePacket(false);
    		if(fin != null) {
    			break;
    		}
    	}
    	
    	// Fin Received
    	Util.ServerClient.d("Close","Received FIN: " + Util.dumpDatagram(fin));
    	this.sendAck(fin, false);
    	this.state = State.TIME_WAIT;
    	
    	try {
			Thread.sleep(30*1000);
		} catch (Exception e) {
			
		}
    	
    	this.state = State.CLOSED; 
    }

    /**
     * Test a packet for transmission errors. This function should only called
     * with data or ACK packets in the ESTABLISHED state.
     * 
     * @param packet
     *            Packet to test.
     * @return true if packet is free of errors, false otherwise.
     */
    protected boolean isValid(KtnDatagram packet) {
		/*throw new NotImplementedException();*/
		return ((packet.getChecksum() == packet.calculateChecksum()) && !isGhostPacket(packet));
	}
	protected boolean isGhostPacket(KtnDatagram packet)
	{
		if(!packet.getSrc_addr().equals(remoteAddress) || packet.getSrc_addr() == null)
		{
			return true;
		}
		else if(packet.getSrc_port() != remotePort)
		{
			return true;
		}

		return false;

	}
}
