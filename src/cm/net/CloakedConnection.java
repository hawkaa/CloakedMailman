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
 * @author Sebj�rn Birkeland and Stein Jakob Nordb�
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
    	Log.d("Constructor", "Creating connection with port number " + this.myPort + ", ip " + this.myAddress + ", sequence number " + this.nextSequenceNo);
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
    	
    	Log.d("Connect", "Trying to connect to " + this.remoteAddress + ":" + this.remotePort);
    	
    	// State m� v�re CLOSED n�r connect() kalles.
    	if(this.state != State.CLOSED)
    	{
			throw new InvalidStateException("This call requires the connection to be CLOSED");
    	}
    	
    	// Sender SYN
    	KtnDatagram packet = constructInternalPacket(Flag.SYN);
    	Log.d("Connect", "Sending SYN");
    	simplySendPacket(packet);
    	Log.d("Connect", "Waiting for ACK");
    	KtnDatagram ackReceive = receiveAck();
    	this.state = State.SYN_SENT;
    	
    	// F�r man ikke noe svar, har det oppst�tt en time out
    	if(ackReceive == null)
    	{
    		Log.d("Connect", "ACK timed out");
    		throw new SocketTimeoutException("Socket timed out");
    	}
    	Log.d("Connect", "ACK received");
    	// Mottar man SYN_ACK, er connect() oppn�dd
    	if(ackReceive.getFlag() != Flag.SYN_ACK)
    	{
    		Log.d("Connect", "ACK received, but it was not SYNACK.");
    		throw new SocketTimeoutException("Socket timed out");
    		
    	}
    	// OK, set new data
    	this.remoteAddress = ackReceive.getSrc_addr();
    	this.remotePort = ackReceive.getSrc_port();
    	
    	this.state = State.ESTABLISHED;
		KtnDatagram ackPacket = constructInternalPacket(Flag.ACK);
		Log.d("Connect", "Sending ACK");
		simplySendPacket(ackPacket);
		
		Log.d("Connect", "Connection established.");
    	
    	
    	
    }

    /**
     * Listen for, and accept, incoming connections.
     * 
     * @return A new ConnectionImpl-object representing the new connection.
     * @throws InvalidStateException 
     * @see Connection#accept()
     */
    public Connection accept() throws IOException, SocketTimeoutException, InvalidStateException {
    	/*
    	 * Its essential that the connection is in the correct states
    	 */
    	if (this.state != State.CLOSED){
    		throw new InvalidStateException("This call requires the connection to be CLOSED");
    	}
    	
    	// Set the state
    	this.state = State.LISTEN;
    	
    	Log.d("Accept", "Listening for connection on " + this.myAddress + ":" + this.myPort);
    	
    	while (true) {
    		// Receive datagram
    		KtnDatagram dg = this.receivePacket(true);
    		
    		// If datagram not set
    		if (dg == null) {
    			continue;
    		}
    		
    		// Check if syn. If not SYN, try again
    		if(dg.getFlag() != Flag.SYN) {
    			continue;
    		}
    		
    		// Allright, we've received a SYN package.
    		Log.d("Accept", "Received SYN package from " + dg.getSrc_addr() + ":" + dg.getSrc_port());
    		
    		// Creating a new connection with random port number
    		CloakedConnection con = new CloakedConnection();
    		
    		// Setting the connection state to SYN RCVD
    		con.state = State.SYN_RCVD;
    		
    		// Setting connection parameters
    		con.remoteAddress = dg.getSrc_addr();
    		con.remotePort = dg.getSrc_port();
    		
    		// Send SYNACK to initiator
    		Log.d("Accept", "Sending SYNACK to initiator");
    		con.sendAck(dg, true);
    		
    		// Awaiting ACK
    		KtnDatagram ack = con.receiveAck();
    		if(ack == null) {
    			Log.d("Accept", "Connection ack timed out. Restarting.");
    		}
    		// Connection established
    		con.state = State.ESTABLISHED;
    		Log.d("Accept", "Connection established, returning connection");
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
        Log.d("Send", "Trying to send: " + msg);
        KtnDatagram d = this.constructDataPacket(msg);
        KtnDatagram ack = this.sendDataPacketWithRetransmit(d);
        if (ack == null) {
        	Log.d("Send", "Ack not received, packet timed out");
        	throw new IOException("Did not receive ACK on packet.");
        }
    	Log.d("Send", "Ack received, packet sent successfully");
        
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
    	
    	Log.d("Receive", "Starting to wait for packet");
    	
    	while (true) {
    		// Receive datagram
    		KtnDatagram dg = this.receivePacket(true);
    		
    		// If datagram not set
    		if (dg == null) {
    			continue;
    		}
    		
    		
    		// Package received, yay!
    		if(!this.isValid(dg)){
    			Log.d("Receive", "Package invalid, ignoring");
    			// Package not valid, ignore it.
    			continue;
    		}
    		
    		Log.d("Receive", "Package is valid, returning payload \"" + (String)dg.getPayload() + "\" to callee. Sending ACK on package.");
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
        /*throw new NotImplementedException();*/
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
    	return true;
    }
}
