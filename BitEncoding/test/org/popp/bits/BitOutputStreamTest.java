package org.popp.bits;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

public class BitOutputStreamTest {
	private static final long[] IMPORTANT_NUMBERS = {0, -1, 1, 2, -2, 255, -255, 256, -256, 65535, -65535, 65536, -65536, -4294967295L, 4294967295L, -4294967296L, 4294967296L, Long.MAX_VALUE, Long.MIN_VALUE};
	
	@Test
	public void testWriteBit() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BitOutputStream bos = new BitOutputStream(baos);
		
		// write 7 zeroes, making sure no byte is flushed to the underlying stream
		Assert.assertEquals(0, baos.size());
		for(int i = 0; i < 7; i++) {
			bos.writeBit(0);
			Assert.assertEquals(0, baos.size());
		}
		// write the 8th 0, verifying a 0 byte was written to the stream
		bos.writeBit(0);
		Assert.assertEquals(1, baos.size());
		Assert.assertEquals(0, baos.toByteArray()[0]);
		
		// write 7 ones, making sure no additional byte is written
		for(int i = 0; i < 7; i++) {
			bos.writeBit(1);
			Assert.assertEquals(1, baos.size());
		}
		// write the last one, verifying a 0xFF byte was written to the stream
		bos.writeBit(1);
		Assert.assertEquals(2, baos.size());
		Assert.assertEquals(0xFF, baos.toByteArray()[1]&0xFF);
		
		// write a single bit, verifying it is not written out yet
		bos.writeBit(1);
		Assert.assertEquals(2, baos.size());
		
		// but then flush it, verifying it is written with no further ones
		bos.flush();
		Assert.assertEquals(3, baos.size());
		Assert.assertEquals(0x80, baos.toByteArray()[2]&0xFF);
	}
	
	@Test
	public void testWrite() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BitOutputStream bos = new BitOutputStream(baos);
		
		Assert.assertEquals(0, baos.size());
		
		bos.write(12);
		Assert.assertEquals(1, baos.size());
		Assert.assertEquals(12, baos.toByteArray()[0]);
		
		bos.write(20);
		Assert.assertEquals(2, baos.size());
		Assert.assertEquals(20, baos.toByteArray()[1]);
		
		// flushing should not cause any writes since we're
		// on a byte boundary
		bos.flush();
		Assert.assertEquals(2, baos.size());
	}
	
	@Test
	public void testWriteNumber() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BitOutputStream bos = new BitOutputStream(baos);
		
		for(int i = 0; i < 8; i++) {
			bos.writeDynamicNumber(0);
		}
		
		Assert.assertEquals(1, baos.size());
		Assert.assertEquals(0, baos.toByteArray()[0]);
	}
	
	@Test
	public void testOffsetWrite() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BitOutputStream bos = new BitOutputStream(baos);
		
		Assert.assertEquals(0, baos.size());
		
		bos.writeBit(0);
		
		Assert.assertEquals(0, baos.size());
		
		bos.write(255);
		
		Assert.assertEquals(1, baos.size());
		Assert.assertEquals(255-128, baos.toByteArray()[0]&0xFF);
		
		bos.flush();
		Assert.assertEquals(2, baos.size());
		Assert.assertEquals(128, baos.toByteArray()[1]&0xFF);
		
		bos.writeBit(0);
		bos.writeBit(0);
		bos.write(0xFF);
		
		Assert.assertEquals(3, baos.size());
		Assert.assertEquals(255-128-64, baos.toByteArray()[2]&0xFF);
		
		for(int i = 0; i < 256; i++) {
			baos = new ByteArrayOutputStream();
			bos = new BitOutputStream(baos);
		
			bos.writeBit(1);
			bos.writeBit(0);
			bos.write(i);
			
			int expected = (i >>> 2) + 128;;
			Assert.assertEquals(expected, baos.toByteArray()[0] & 0xFF);
		}
	}
	
	@Test
	public void testAssortment() throws IOException {
		// create a bit outputstream and write an assortment
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BitOutputStream bos = new BitOutputStream(baos);
		bos.writeBit(1);
		bos.writeBit(0);
		bos.write(120);
		bos.write(0xFF);
		bos.write(0x00);
		bos.write(new byte[]{10, 11});
		bos.writeBoolean(true);
		bos.writeBoolean(false);
		bos.writeBit(1);
		bos.write(123);
		bos.flush();
		
		// now verify that using a bit inputstream to read it back in,
		// we get the same results
		
		BitInputStream bis = new BitInputStream(new ByteArrayInputStream(baos.toByteArray()));
		Assert.assertEquals(1,  bis.readBit());
		Assert.assertEquals(0, bis.readBit());
		Assert.assertEquals(120, bis.read() & 0xFF);
		Assert.assertEquals(0xFF, bis.read() & 0xFF);
		Assert.assertEquals(0x00, bis.read() & 0xFF);
		byte[] toFill = new byte[2];
		bis.read(toFill);
		Assert.assertEquals(10, toFill[0]);
		Assert.assertEquals(11, toFill[1]);
		Assert.assertEquals(true, bis.readBoolean());
		Assert.assertEquals(false, bis.readBoolean());
		Assert.assertEquals(1, bis.readBit());
		Assert.assertEquals(123,  bis.read());
	}
	
	@Test
	public void testNumberAssortment() throws IOException {
		// create a bit outputstream and write an assortment
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BitOutputStream bos = new BitOutputStream(baos);
		for(long number : IMPORTANT_NUMBERS) {
			bos.writeDynamicNumber(number);
		}
		for(long number : IMPORTANT_NUMBERS) {
			bos.writeDynamicNumber(number);
		}
		bos.flush();
		
		// now verify that using a bit inputstream to read it back in,
		// we get the same results
		
		BitInputStream bis = new BitInputStream(new ByteArrayInputStream(baos.toByteArray()));
		for(long number : IMPORTANT_NUMBERS) {
			Assert.assertEquals(number, bis.readDynamicNumber());
		}
		for(long number : IMPORTANT_NUMBERS) {
			Assert.assertEquals(number, bis.readDynamicNumber());
		}
	}
	
	@Test
	public void testRandomNumberAssortment() throws IOException {
		SecureRandom seedRandom = new SecureRandom();
		long seed = seedRandom.nextLong();
		
		Random r = new Random(seed);
		
		for(int iter = 0; iter < 100; iter++) {
			List<Long> numbers = new ArrayList<Long>();
			for(int i = 0; i < 200; i++) {
				if(r.nextBoolean()) {
					// add one of the important numbers
					numbers.add(IMPORTANT_NUMBERS[r.nextInt(IMPORTANT_NUMBERS.length)]);
				} else {
					// add a random long
					numbers.add(r.nextLong());
				}
			}
			
			// create a bit outputstream and write an assortment
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BitOutputStream bos = new BitOutputStream(baos);
			for(long number : numbers) {
				bos.writeDynamicNumber(number);
			}
			bos.flush();
			
			BitInputStream bis = new BitInputStream(new ByteArrayInputStream(baos.toByteArray()));
			for(long number : numbers) {
				Assert.assertEquals("Failed with random seed: " + seed, number, bis.readDynamicNumber());
			}
		}
	}
	
	@Test
	public void testWriteUTF() throws IOException {
		// create a bit outputstream and write an assortment
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BitOutputStream bos = new BitOutputStream(baos);
		bos.writeUTF("", false);
		bos.flush();
		
		// now verify that using a bit inputstream to read it back in,
		// we get the same results
		
		BitInputStream bis = new BitInputStream(new ByteArrayInputStream(baos.toByteArray()));
		Assert.assertEquals("",  bis.readUTF(false));
	}

}
