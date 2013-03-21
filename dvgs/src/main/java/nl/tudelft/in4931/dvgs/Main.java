package nl.tudelft.in4931.dvgs;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import nl.tudelft.in4931.dvgs.models.Job;
import nl.tudelft.in4931.dvgs.network.Address;
import nl.tudelft.in4931.dvgs.network.Role;
import nl.tudelft.in4931.dvgs.network.TopologyListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

	private static final Logger log = LoggerFactory.getLogger(Main.class);
	
	private static ResourceManager manager;
	private static Scheduler scheduler;

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length == 0) {
			throw new IllegalArgumentException("Require at least one parameter: --resource-manager or --scheduler");
		}
		
		InetAddress address = configureAddress(args);
		if (args[0].equals("--resource-manager")) {
			manager = new ResourceManager(address, getCapacity(args));

			final AtomicBoolean initialized = new AtomicBoolean();
			manager.addTopologyListener(new TopologyListener() {
				@Override
				public void onNodeLeft(Address address, Role role) { }
				
				@Override
				public void onNodeJoin(Address address, Role role) {
					if (role == Role.SCHEDULER) {
						initialized.set(true);
					}
				}
			});
			
			while (!initialized.get()) {
				Thread.sleep(1000);
			}
			
			for (int i = 0; i < 100; i++) {
				Thread.sleep(new Random().nextInt(1000));
				manager.offerJob(new Job(System.nanoTime(), new Random().nextInt(10000) + 2000), true);
			}
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
			if (networkInterfaceName.contains(".")) {
				InetAddress element = InetAddress.getByName(networkInterfaceName);
				log.info("Selected IP address: {}", element.getHostAddress());
				System.setProperty("java.rmi.server.hostname", element.getHostAddress());
				return element;
			}
			else {
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
		}
		
		InetAddress localAddress = InetAddress.getByName("127.0.0.1");
		log.info("Selected IP address: {}", localAddress.getHostAddress());
		return localAddress;
	}

}
