package org.popp.bits;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

public class BitInputStreamTest {

	@Test
	public void testReadBit() throws Exception {
		// test an empty supplied inputstream, should return -1 immediately
		BitInputStream bis = new BitInputStream(new ByteArrayInputStream(new byte[]{}));
		Assert.assertEquals(-1, bis.readBit());
		
		// test with length 1 input streams, should return 8 0's or 1's, and then -1
		bis = new BitInputStream(new ByteArrayInputStream(new byte[]{0}));
		for(int i = 0; i < 8; i++) {
			Assert.assertEquals("BitRead i=" + i, 0, bis.readBit());
		}
		Assert.assertEquals(-1, bis.readBit());
		
		bis = new BitInputStream(new ByteArrayInputStream(new byte[]{(byte)0xFF}));
		for(int i = 0; i < 8; i++) {
			Assert.assertEquals("BitRead i=" + i, 1, bis.readBit());
		}
		Assert.assertEquals(-1, bis.readBit());		
		

		// test with length 2 input streams, should return 16 0's or 1's, and then -1
		bis = new BitInputStream(new ByteArrayInputStream(new byte[]{(byte)0x00, (byte)0x00}));
		for(int i = 0; i < 16; i++) {
			Assert.assertEquals("BitRead i=" + i, 0, bis.readBit());
		}
		Assert.assertEquals(-1, bis.readBit());		
		
		bis = new BitInputStream(new ByteArrayInputStream(new byte[]{(byte)0xFF, (byte)0xFF}));
		for(int i = 0; i < 16; i++) {
			Assert.assertEquals("BitRead i=" + i, 1, bis.readBit());
		}
		Assert.assertEquals(-1, bis.readBit());		
		
		// test with 1000 random input streams, with lengths between 1 and 5
		long seed = new Random().nextLong();
		Random r = new Random(seed);
		
		for(int iteration = 0; iteration < 1000; iteration++) {
			int randomArrayLength = 1 + r.nextInt(4);
			byte[] randomData = new byte[randomArrayLength];
			r.nextBytes(randomData);
			bis = new BitInputStream(new ByteArrayInputStream(randomData));
			for(int i = 0; i < (randomArrayLength * 8); i++) {
				int byteValue = randomData[i/8];
				int bitValue = (byteValue >>> (7 - (i%8))) & 0x01;
				Assert.assertEquals("Failed with seed: " + seed, bitValue, bis.readBit());
			}
			Assert.assertEquals("Failed with seed: " + seed, -1, bis.readBit());
		}
	}
	
	@Test
	public void testRead() throws Exception {
		// test an empty supplied inputstream, should return -1 immediately
		BitInputStream bis = new BitInputStream(new ByteArrayInputStream(new byte[]{}));
		Assert.assertEquals(-1, bis.read());
		
		// test with length 1 input streams, should return 8 0's or 1's, and then -1
		bis = new BitInputStream(new ByteArrayInputStream(new byte[]{0}));
		Assert.assertEquals(0, bis.read());
		Assert.assertEquals(-1, bis.read());
		Assert.assertEquals(-1, bis.readBit());
		
		bis = new BitInputStream(new ByteArrayInputStream(new byte[]{(byte)0xFF}));
		Assert.assertEquals(0xFF, bis.read());
		Assert.assertEquals(-1, bis.read());	
		Assert.assertEquals(-1, bis.readBit());		
		

		// test with length 2 input streams, should return 16 0's or 1's, and then -1
		bis = new BitInputStream(new ByteArrayInputStream(new byte[]{(byte)0x00, (byte)0x00}));
		for(int i = 0; i < 2; i++) {
			Assert.assertEquals("Read i=" + i, 0, bis.read());
		}
		Assert.assertEquals(-1, bis.read());	
		Assert.assertEquals(-1, bis.readBit());		
		
		bis = new BitInputStream(new ByteArrayInputStream(new byte[]{(byte)0xFF, (byte)0xFF}));
		for(int i = 0; i < 2; i++) {
			Assert.assertEquals("Read i=" + i, 0xFF, bis.read());
		}
		Assert.assertEquals(-1, bis.read());		
		Assert.assertEquals(-1, bis.readBit());		
		
		// test with 1000 random input streams, with lengths between 1 and 5
		long seed = new Random().nextLong();
		Random r = new Random(seed);
		
		for(int iteration = 0; iteration < 1000; iteration++) {
			int randomArrayLength = 1 + r.nextInt(4);
			byte[] randomData = new byte[randomArrayLength];
			r.nextBytes(randomData);
			bis = new BitInputStream(new ByteArrayInputStream(randomData));
			for(int i = 0; i < randomArrayLength; i++) {
				Assert.assertEquals("Failed with seed: " + seed, randomData[i]&0xFF, bis.read());
			}
			Assert.assertEquals("Failed with seed: " + seed, -1, bis.read());
			Assert.assertEquals("Failed with seed: " + seed, -1, bis.readBit());
		}
	}
	
	@Test
	public void testReadWhenOffset() throws Exception {
		
		// test an empty supplied inputstream, should return -1 immediately
		BitInputStream bis = new BitInputStream(new ByteArrayInputStream(new byte[]{}));
		Assert.assertEquals(-1, bis.readBit());
		Assert.assertEquals(-1, bis.read());
		
		// test with length 1 input streams, should return 8 0's or 1's, and then -1
		bis = new BitInputStream(new ByteArrayInputStream(new byte[]{0}));
		Assert.assertEquals(0, bis.readBit());
		try { 
			Assert.assertEquals(-1, bis.read());
			Assert.fail("Expected exception");
		} catch (IOException e) {}
		
		bis = new BitInputStream(new ByteArrayInputStream(new byte[]{(byte)0xFF}));
		Assert.assertEquals(1, bis.readBit());
		try {
			Assert.assertEquals(-1, bis.read());
			Assert.fail("Expected exception");
		} catch (IOException e){}
	}
	
	@Test
	public void testReadAlign() throws Exception {
		BitInputStream bis = new BitInputStream(new ByteArrayInputStream(new byte[]{(byte)128, (byte)128, (byte)128}));
		// first bit is 1
		Assert.assertEquals(1, bis.readBit());
		// second bit is 0
		Assert.assertEquals(0, bis.readBit());
		
		bis.align(); // align to start of next byte
		// first bit of second byte is 1
		Assert.assertEquals(1, bis.readBit());
		for(int i = 0; i < 7; i++) {
			Assert.assertEquals(0, bis.readBit()); // read remaining bits of second byte
		}
		
		bis.align(); // should do nothing since we are already aligned
		Assert.assertEquals(1, bis.readBit()); // first bit of third byte is 1
	}
}
