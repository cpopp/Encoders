package org.popp.bits;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class BitOutputStream extends OutputStream {
	private final OutputStream os;
	
	private static final int BITS_IN_BYTE = 8;
	private final static int[] LOW_ORDER_BIT_MASKS = {0x00, 0x01, 0x03, 0x7, 0xF, 0x1F, 0x3F, 0x7F, 0xFF};
	
	private static final long MAX_8_BIT_NUMBER = (1L << 8) - 1;
	private static final long MAX_16_BIT_NUMBER = (1L << 16) - 1;
	private static final long MAX_32_BIT_NUMBER = (1L << 32) - 1;
	
	private int currentByte = 0;
	private int bitPosition = 0;
	
	public BitOutputStream(OutputStream os) {
		this.os = os;
	}
	
	/**
	 * Write a single bit to the stream
	 */
	public void writeBit(int bit) throws IOException {
		// mask off to make sure we only grab the low order bit
		bit &= 0x01;
		
		// shift it up to the expected position.  if the bit position is 0,
		// we need to shift it up 7 spots.  if the bit position is 7, we shift none
		bit <<= (BITS_IN_BYTE - bitPosition - 1);
		
		// and it into the current byte and move our pointer up one
		currentByte |= bit;
		
		++bitPosition;
		
		// flush if necessary
		flushIfNecessary();
	}

	/**
	 * Write a single byte to the stream.  If individual bits have been
	 * written to the stream, the write may cause the byte to be split across
	 * traditional byte boundaries.
	 */
	@Override
	public void write(int b) throws IOException {
		if(bitPosition == 0) {
			os.write(b);
			return;
		}
		
		// need to fill the remaining space of the current buffer,
		// if bitPosition is 7, we need the top bit.  if it is 1, we
		// need the 7 top bits
		// shift down the bits that will fit
		int bWillFit = b >>> (bitPosition);
		
		// mask off the bits we want into our buffer byte
		currentByte |= bWillFit & LOW_ORDER_BIT_MASKS[BITS_IN_BYTE - bitPosition];
		
		// write out the buffer byte and clear it
		os.write(currentByte);
		currentByte = 0x00;
		
		// now that we have a fresh buffer byte, put the remaining
		// bits from the supplied byte in.  if bitPosition 1 is, we
		// want the low bit shifted up and and it in.  if bitPosition is
		// 7, we want to shift it up 1.
		
		// we mask off the bits we want
		int bWillNowFit = b & LOW_ORDER_BIT_MASKS[bitPosition];
		
		// and shift them up before anding them in
		bWillNowFit <<= (BITS_IN_BYTE - bitPosition);
		
		currentByte |= bWillNowFit;
	}
	
	/**
	 * Write a single boolean value to the stream.  This will be
	 * represented in the stream as a single bit
	 */
	public void writeBoolean(boolean value) throws IOException {
		writeBit(value ? 1 : 0);
	}
	
	public void writeUTF(String string, boolean align) throws IOException {
		byte[] data = string.getBytes(Charset.forName("UTF-8"));
		writeDynamicNumber(data.length);
		if(align && data.length > 0) {
			align();
		}
		write(data);
	}
	
	/**
	 * Write a signed long to the stream.  This will be represented in the stream
	 * using a variable number of bits, with a smaller number used to represent smaller numbers
	 * 
	 * 0 is represented as a single bit 0, with other numbers represented with
	 * a sign bit and various header depending on the range the number fits in:
	 * 
	 * if value is positive
	 * 100 --> 1 
	 * 1010 --> byte next is value (0-255)
	 * 10110 --> next 2 bytes are value (0-65535)
	 * 101110 --> next 4 bytes are value (0-4294967295)
	 * 101111 --> next 8 bytes are value (any remaining number)
	 * 
	 * if value is negative
	 * 110 --> 1 
	 * 1110 --> byte next is value (0-255)
	 * 11110 --> next 2 bytes are value (0-65535)
	 * 111110 --> next 4 bytes are value (0-4294967295)
	 * 111111 --> next 8 bytes are value (any remaining number)
	 * 
	 * so, 0 will take a single bit to represent, -1 and 1 will take three bits,
	 * a number with magnitude less than 256 will take 12 bits, a number less than 65536 will take
	 * 21 bits, a number less than 4294967296 will take 38 bits, and any remaining number will take
	 * 70 bits.
	 */
	public void writeDynamicNumber(long number) throws IOException {
		if(number == 0) { // 0 is 0
			writeBit(0);
			return;
		}
		
		writeBit(1);
		
		if(number > 0) {
			writeBit(0); // 0 for positive, 1 for negative
		} else {
			writeBit(1);
		}
		
		number = Math.abs(number);
		
		if(number == 1) { // 1S0 is 1
			writeBit(0);
			return;
		}
		
		writeBit(1);
		
		if(number > 0 && number <= MAX_8_BIT_NUMBER) { // 1S10 is 8 bit number
			writeBit(0);
			write((int)number);
			return;
		}
		
		writeBit(1);
		
		if(number > 0 && number <= MAX_16_BIT_NUMBER) { // 1S110 is 16 bit number
			writeBit(0);
			write(((int)(number>>>8))); // high 8 bits
			write((int)number); // low 8 bits
			return;
		}
		
		writeBit(1);
		
		if(number > 0 && number <= MAX_32_BIT_NUMBER) { // 1S1110 is 32 bit number
			writeBit(0);
			write((int)number>>>24);
			write((int)number>>>16);
			write((int)number>>>8);
			write((int)number);
			return;
		}
		
		writeBit(1); // 1S1111 is 64 bit number
		write((int)(number>>>56));
		write((int)(number>>>48));
		write((int)(number>>>40));
		write((int)(number>>>32));
		write((int)(number>>>24));
		write((int)(number>>>16));
		write((int)(number>>>8));
		write((int)(number));
	}
	
	/**
	 * Aligns the output stream so the next write will occurs along
	 * a byte boundary.  This will discard any remaining space in the
	 * current byte for the sake of ensuring the next data is byte aligned.
	 * 
	 * For example, if writeBit() is followed by a call to align(), then
	 * seven bits will be wasted in order to align the stream.  The benefit
	 * is that some post professing may benefit from particular data being
	 * byte aligned.  For example, if you call align() before writing a large
	 * String, then compression on the output may perform better when the data
	 * representing the string is aligned to byte boundaries.
	 */
	public void align() throws IOException {
		flush();
	}
	
	@Override
	public void flush() throws IOException {
		if(bitPosition > 0) {
			os.write(currentByte);
			bitPosition = 0;
			currentByte = 0x00;
		}
	}
	
	private void flushIfNecessary() throws IOException {
		if(bitPosition == 8) {
			flush();
		}
	}
	
	@Override
	public void close() throws IOException {
		flushIfNecessary();
		os.close();
	}
}
