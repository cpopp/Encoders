package org.popp.bits;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class BitInputStream extends InputStream{
	private static final int BITS_IN_BYTE = 8;
	private final static int[] LOW_ORDER_BIT_MASKS = {0x00, 0x01, 0x03, 0x7, 0xF, 0x1F, 0x3F, 0x7F, 0xFF};
	
	private final DataInputStream is;
	
	private int currentByte = -1;
	private short bitPosition = 0;
	
	public BitInputStream(InputStream is) {
		this.is = new DataInputStream(is);
	}
	
	/**
	 * Reads a single bit from the stream.  Returns -1 when the end
	 * of the stream has been reached.
	 */
	public int readBit() throws IOException {
		prime();
		
		if(currentByte == -1) {
			return currentByte;
		}
		
		// shift over to grab the current bit offset...e.g., if we are on
		// bitPosition 0, we want to shift over 7 places.  If we are on the
		// last bit of the byte, bitPosition 7, we do not want to shift at all
		int value = (currentByte >>> (BITS_IN_BYTE - bitPosition - 1));
		
		bitPosition+=1;
		
		return value & 0x01;
	}
	
	/**
	 * Reads a single byte from the stream
	 */
	@Override
	public int read() throws IOException {
		prime();
		
		if(currentByte == -1) {
			return -1;
		}
		
		if(bitPosition == 0) {
			// the current byte is unused, we can just return it
			bitPosition+=8;
			return currentByte;
		}
		
		
		
		// we weren't at the start of a byte, so we need to part of the current
		// with the next byte.  if bitPosition is 7, it means we have one bit left,
		// if it is 6, we have 2 bits left, etc.
		int bitsRemaining = BITS_IN_BYTE-bitPosition;

		// mask off the bits remaining.  we then need to shift them up.  if
		// we only had 1 bit remaining, we need to shift it up 7, if we have 7 bits
		// remaining, we only need a shift of 1
		int valueToReturn = (currentByte & LOW_ORDER_BIT_MASKS[bitsRemaining]) << (BITS_IN_BYTE - bitsRemaining);
		
		// now we've consumed the bits from our current byte, so we'll read in another
		currentByte = is.read();
		if(currentByte == -1) {
			throw new IOException("Insufficient data remaining to read byte");
		}
		
		// read in the remaining bits from the new byte
		// say bitPosition is 1...we need to shift 7.  if it is 7, we need to shift 1
		valueToReturn |= (currentByte >>> (BITS_IN_BYTE - bitPosition));
		
		return valueToReturn;
	}
	
	/**
	 * Reads a boolean from the stream
	 */
	public boolean readBoolean() throws IOException {
		return readBit() == 0 ? false : true;
	}
	
	public String readUTF(boolean align) throws IOException {
		int length = (int)readDynamicNumber();
		if(align && length > 0) {
			align();
		}
		byte[] data = new byte[length];
		
		for(int i = 0; i < data.length; i++) {
			data[i] = (byte)read();
		}
		
		return new String(data, Charset.forName("UTF-8"));
	}
	
	/**
	 * Reads a signed long from the stream.  See writeDynamicNumber of
	 * BitOutputStream for format information
	 */
	public long readDynamicNumber() throws IOException {
		if(readBit() == 0) { // is it zero?
			return 0;
		}
		
		boolean isPositive = readBit() == 0;
		
		if(readBit() == 0) {
			return isPositive ? 1 : -1;
		}
		
		int byteSize;
		
		if(readBit() == 0) { // 8 bit number
			byteSize = 1;
		} else if(readBit() == 0) { // 16 bit number
			byteSize = 2;
		} else if(readBit() == 0) { // 32 bit number
			byteSize = 4;
		} else { // 64 bit number
			byteSize = 8;
		}
		
		long number = 0L;
		for(int i = 0; i < byteSize; i++) {
			number <<= 8;
			number |= read(); // read 
		}
	
		if(!isPositive) {
			number = -number;
		}
		
		return number;
	}
	
	/**
	 * Discards remaining bits from the current byte and aligns
	 * the input stream to the next byte boundary.  Subsequents
	 * reads will occur at the beginning of the next byte boundary.
	 */
	public void align() {
		bitPosition = 8;
	}
	
	private void prime() throws IOException{
		if(currentByte == -1) {
			currentByte = is.read();
		} else if(bitPosition == 8) {
			currentByte = is.read();
			bitPosition = 0;
		}
	}
}
