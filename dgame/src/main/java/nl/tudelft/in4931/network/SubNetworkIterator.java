package nl.tudelft.in4931.network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;

import com.google.common.collect.Range;

public class SubNetworkIterator implements Iterator<Address> {
	
	public static final Range<Integer> PORTS = Range.closed(1100, 1300);

	private final InetAddress local;
	private final boolean localOnly;
	
	private byte currentByte;
	private int currentPort;
	private boolean exhausted;
	
	public SubNetworkIterator(InetAddress localAddress) throws UnknownHostException {
		this.local = localAddress;
		this.localOnly = localAddress.equals("127.0.0.1");
		this.currentPort = PORTS.lowerEndpoint();
		this.exhausted = false;
		this.currentByte = localOnly ? getLastByte(localAddress) : 0;
	}

	@Override
	public boolean hasNext() {
		return !exhausted;
	}

	@Override
	public Address next() {
		if (localOnly) {
			Address address = new Address(local.getHostAddress(), currentPort);
			if (currentPort == PORTS.upperEndpoint()) {
				exhausted = true;
			}
			else {
				currentPort++;
			}
			
			return address;
		}
		else {
			byte[] chunks = local.getAddress();
			byte[] newChunks = new byte[chunks.length];
			
			newChunks[chunks.length - 1] = currentByte;
			for (int i = 0; i < chunks.length - 1; i++) {
				newChunks[i] = chunks[i];
			}
			
			String hostAddress = null;
			try {
				hostAddress = InetAddress.getByAddress(newChunks).getHostAddress();
			}
			catch (UnknownHostException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
			
			Address address = new Address(hostAddress, currentPort);
	
			if (currentByte == (byte) 255 && currentPort == PORTS.upperEndpoint()) {
				exhausted = true;
			}
			else {
				if (currentPort == PORTS.upperEndpoint()) {
					currentPort = PORTS.lowerEndpoint();
					currentByte++;
				}
				else {
					currentPort++;
				}
			}
			
			return address;
		}
	}

	@Override
	public void remove() {
		// Do nothing.
	}
	
	private byte getLastByte(InetAddress localAddress) {
		byte[] byteArray = localAddress.getAddress();
		return byteArray[byteArray.length - 1];
	}

}
