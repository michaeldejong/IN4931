package nl.tudelft.in4931.dvgs;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

	private static final Logger log = LoggerFactory.getLogger(Main.class);
	
	private static ResourceManager manager;
	private static Scheduler scheduler;

	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			throw new IllegalArgumentException("Require at least one parameter: --resource-manager or --scheduler");
		}
		
		InetAddress address = configureAddress(args);
		if (args[0].equals("--resource-manager")) {
			manager = new ResourceManager(address, getCapacity(args));
		}
		else if (args[0].equals("--scheduler")) {
			scheduler = new Scheduler(address);
		}
		else {
			throw new IllegalArgumentException("First argument should be either: --resource-manager or --scheduler");
		}
	}
	
	private static int getCapacity(String[] args) {
		if (args.length >= 3) {
			return Integer.parseInt(args[2]);
		}
		return 100;
	}

	private static InetAddress configureAddress(String[] args) throws IOException {
		if (args.length >= 2) {
			String networkInterfaceName = args[1];
			NetworkInterface networkInterface = NetworkInterface.getByName(networkInterfaceName);
			Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
			
			while (inetAddresses.hasMoreElements()) {
				InetAddress element = inetAddresses.nextElement();
				if (element instanceof Inet4Address) {
					log.info("Selected IP address: {}", element.getHostAddress());
					System.setProperty("java.rmi.server.hostname", element.getHostAddress());
					return element;
				}
			}
		}
		
		InetAddress localAddress = InetAddress.getByName("127.0.0.1");
		log.info("Selected IP address: {}", localAddress.getHostAddress());
		return localAddress;
	}

}
