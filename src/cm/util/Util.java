package cm.util;


import no.ntnu.fp.net.cl.KtnDatagram;

public class Util {
	
	public static Log ServerClient;
	public static Log Herpaderp;
	
	public static String dumpDatagram(KtnDatagram dg) {
		String s = "[";
		s += "Src: " + dg.getSrc_addr() + ":" + dg.getSrc_port() + ", ";
		s += "Dest: " + dg.getDest_addr() + ":" + dg.getDest_port() + ", ";
		s += "Seq nr: " + dg.getSeq_nr() + ", ";
		s += "Ack: " + dg.getAck() + ", ";
		s += "Payload: " + dg.getPayload() + ", ";
		s += "Checksum: " + dg.getChecksum() + " ";
		String chk = "valid";
		if (dg.calculateChecksum() != dg.getChecksum()) {
			chk = "not valid";
		}
		s += chk + ", ";
		s += "Flag: " + dg.getFlag().toString() + "]";
		return s;
	}
}
