package grails.plugin.nettymvc.http;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

// Based on org.apache.catalina.util.SessionIdGenerator
public class SessionIdGenerator {

	/**
	 * Queue of random number generator objects to be used when creating session
	 * identifiers. If the queue is empty when a random number generator is
	 * required, a new random number generator object is created. This is
	 * designed this way since random number generators use a sync to make them
	 * thread-safe and the sync makes using a a single object slow(er).
	 */
	protected Queue<SecureRandom> randoms = new ConcurrentLinkedQueue<SecureRandom>();

	protected int sessionIdLength = 16;

	/**
	 * Generate and return a new session identifier.
	 */
	public String generateSessionId() {

		byte[] random = new byte[16];

		// Render the result as a String of hexadecimal digits
		StringBuilder buffer = new StringBuilder();

		int resultLenBytes = 0;

		while (resultLenBytes < sessionIdLength) {
			getRandomBytes(random);
			for (int j = 0; j < random.length && resultLenBytes < sessionIdLength; j++) {
				byte b1 = (byte) ((random[j] & 0xf0) >> 4);
				byte b2 = (byte) (random[j] & 0x0f);
				if (b1 < 10) {
					buffer.append((char) ('0' + b1));
				}
				else {
					buffer.append((char) ('A' + (b1 - 10)));
				}
				if (b2 < 10) {
					buffer.append((char) ('0' + b2));
				}
				else {
					buffer.append((char) ('A' + (b2 - 10)));
				}
				resultLenBytes++;
			}
		}

		return buffer.toString();
	}

	protected void getRandomBytes(byte bytes[]) {
		SecureRandom random = randoms.poll();
		if (random == null) {
			random = createSecureRandom();
		}
		random.nextBytes(bytes);
		randoms.add(random);
	}

	/**
	 * Create a new random number generator instance we should use for generating session identifiers.
	 */
	protected SecureRandom createSecureRandom() {
		SecureRandom result;
		try {
			result = SecureRandom.getInstance("SHA1PRNG");
		}
		catch (NoSuchAlgorithmException e) {
			result = new SecureRandom();
		}

		// Force seeding to take place
		result.nextInt();
		return result;
	}
}
