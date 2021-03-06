/*
 EcmDroid - Android Diagnostic Tool for Buell Motorcycles
 Copyright (C) 2012 by Michel Marti

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 3
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.ecmdroid.sim;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.ecmdroid.EEPROM;
import org.ecmdroid.EEPROM.Page;
import org.ecmdroid.PDU;

/**
 * Simple and stupid ECM Simulator. Listens on TCP port 6275 for
 * incoming connections from ecmdroid. Claims to be a BUE2D
 * DDFI3 ECM.
 *
 */
public class Main
{
	static final int PORT = 6275;
	static final PDU ACK  = new PDU(PDU.STOCK_ECM_ID, PDU.DROID_ID, new byte[]{0x06, 0x00});
	static final PDU NACK = new PDU(PDU.STOCK_ECM_ID, PDU.DROID_ID, new byte[]{0x42, 0x42});

	static final byte[] VERSION_RESPONSE_BUE2D = {0x01, 0x42, 0x00, 0x13, (byte) 0xff, 0x02, 0x06, 0x42, 0x55, 0x45, 0x32, 0x44, 0x32, 0x34, 0x32, 0x20, 0x31, 0x31, 0x2d, 0x33, 0x30, 0x2d, 0x30, 0x39, 0x03, (byte) 0x93};
	static final byte[] VERSION_RESPONSE_BUEIB = {0x01, 0x42, 0x00, 0x13, (byte) 0xff, 0x02, 0x06, 0x42, 0x55, 0x45, 0x49, 0x42, 0x33, 0x31, 0x30, 0x20, 0x31, 0x32, 0x2d, 0x31, 0x31, 0x2d, 0x30, 0x33, 0x03, (byte) 0xe2};
	static final byte[] VERSION_RESPONSE_BUEJA = {0x01, 0x42, 0x00, 0x13, (byte) 0xFF, 0x02, 0x06, 0x42, 0x55, 0x45, 0x4A, 0x41, 0x31, 0x32, 0x30, 0x20, 0x30, 0x35, 0x2D, 0x32, 0x37, 0x2D, 0x39, 0x39, 0x03, (byte) 0xE3};
	static final byte[] VERSION_RESPONSE = VERSION_RESPONSE_BUEIB;

	static final byte[] RT_DATA_BUE2D = new byte[] {0x01, 0x42, 0x00, (byte) 0x80, (byte) 0xff, 0x02, 0x06, 0x15, 0x58, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xcc, 0x25, (byte) 0xcc, 0x25, 0x13, 0x3f, 0x13, 0x3f, 0x23, 0x00, 0x15, (byte) 0xb5, 0x04, 0x49, 0x02, 0x7e, 0x02, 0x66, 0x00, 0x58, 0x02, 0x37, 0x06, (byte) 0xf2, 0x03, 0x00, 0x00, (byte) 0xe8, 0x03, (byte) 0xe8, 0x03, (byte) 0xe8, 0x03, (byte) 0xe8, 0x03, (byte) 0xe8, 0x03, (byte) 0x86, 0x03, 0x00, (byte) 0x90, 0x00, 0x11, 0x3c, (byte) 0xb1, 0x5c, (byte) 0xff, 0x03, (byte) 0xe4, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x17, 0x00, 0x7d, 0x00, 0x00, 0x00, 0x00, 0x56, 0x00, 0x34, 0x01, (byte) 0xf2, (byte) 0xc2, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x83, 0x67, (byte) 0xff, (byte) 0xff, 0x70, 0x19, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x4d, 0x00, (byte) 0xdc, 0x00, 0x00, (byte) 0xb1, 0x5c, (byte) 0xff, (byte) 0xff, 0x00, 0x00, 0x32, 0x00, 0x64, (byte) 0xe8, 0x03, (byte) 0xe8, 0x03, 0x00, 0x03, 0x7c};
	static final byte[] RT_DATA_BUEIB = new byte[] {0x01, 0x42, 0x00, 0x64, (byte) 0xff, 0x02, 0x06, 0x7b, 0x4c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x58, 0x20, 0x58, 0x20, 0x26, 0x36, 0x26, 0x36, 0x2c, 0x00, 0x0d, (byte) 0xd0, 0x04, 0x56, 0x02, 0x79, 0x02, 0x00, 0x00, 0x25, 0x02, 0x38, 0x06, (byte) 0xf2, 0x03, 0x00, 0x00, (byte) 0xe8, 0x03, (byte) 0xe8, 0x03, (byte) 0xe8, 0x03, (byte) 0xe8, 0x03, (byte) 0xe8, 0x03, (byte) 0xe8, 0x03, 0x00, (byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xf8, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xfe, 0x00, 0x40, 0x04, 0x00, 0x00, 0x00, 0x00, (byte) 0xa2, 0x00, 0x3c, 0x01, (byte) 0xf1, (byte) 0xc3, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x0f, 0x03, 0x45};
	static final byte[] RT_DATA_BUEJA = new byte[] {0x01, 0x42, 0x00, 0x5C, (byte) 0xFF, 0x02, 0x06, 0x79, 0x2D, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x90, 0x01, (byte) 0x90, 0x01, 0x27, 0x00, 0x0B, (byte) 0xDF, 0x04, (byte) 0xC3, 0x01, (byte) 0xDB, 0x01, 0x00, 0x00, 0x06, 0x02, (byte) 0x8E, 0x07, 0x28, 0x04, 0x00, 0x00, (byte) 0xE8, 0x03, (byte) 0xE8, 0x03, (byte) 0xE8, 0x03, (byte) 0xE8, 0x03, (byte) 0xC0, 0x04, (byte) 0xC0, 0x04, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, (byte) 0x8B, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xDE, 0x32, 0x30, 0x03, 0x00, 0x00, 0x00, 0x00, (byte) 0xF9, 0x00, 0x3F, 0x01, (byte) 0xF9, (byte) 0xDD, 0x05, 0x03, 0x37};
	static final byte[] RT_DATA = RT_DATA_BUEIB;

	public static void main(String[] args) throws IOException
	{
		EEPROM eeprom = prepareEEPROM();
		ServerSocket socket = new ServerSocket(PORT);

		Socket s;
		while (true) {
			System.out.println("Waiting for incomming connection on port " + PORT + "...");
			s = socket.accept();
			System.out.println("Connection established...");
			InputStream in = s.getInputStream();
			OutputStream out = s.getOutputStream();
			byte[] buffer = new byte[256];
			int rtc = 0;
			long deviceTestStart = 0;
			while(!s.isClosed()) {
				try {
					int len = in.read(buffer,0,6);
					if (len == -1) {
						s.close();
						break;
					}
					len = (buffer[3]&0xff) + 1;
					len = in.read(buffer, 6, len);
					// Verify checksum
					byte cs = 0;
					for (int i = 1; i < len + 5; i++) {
						cs ^= (buffer[i]);
					}
					if (buffer[len+5] != cs) {
						System.err.println("Checksum failed.");
						out.write(NACK.getBytes());
						continue;
					}

					System.out.println(hexdump(buffer, 0, len+6));
					// Thread.sleep(50);
					switch (buffer[6])
					{
					case PDU.CMD_VERSION:
						System.out.println("Version request...");
						out.write(VERSION_RESPONSE);
						break;
					case PDU.CMD_SET:
						int offset = buffer[7]  & 0xff;
						int pgno = buffer[8] & 0xff;
						len = buffer[9] & 0xff;
						System.out.println("SET Command, page " + pgno +", offset " + offset + ", len " + len);
						// Page number 32 -> Virtual Page
						if (buffer[8] == 0x20) {
							System.out.println("Device test");
							deviceTestStart = System.currentTimeMillis();
						}
						out.write(ACK.getBytes());
						break;
					case PDU.CMD_RTDATA:
						System.out.println("RT Data request");
						rtc++;
						if (rtc % 5 == 0) {
							// broken pdu
							out.write(0x4711);
						}
						out.write(RT_DATA);
						break;

					case PDU.CMD_GET:
						pgno = buffer[8] & 0xff;
						offset  = buffer[7] & 0xff;
						len = buffer[9] & 0xff;
						System.out.println("GET request for " + len  + " bytes from page " + pgno + " at offset " + offset);
						byte[] payload = new byte[len+1];
						payload[0]=0x06;
						if (pgno != 32) {
							Page pg = eeprom.getPage(pgno);
							pg.getBytes(offset, len, payload, 1);
						} else {
							// Virtual page 0x20 - device tests
							byte stat = (byte) ((System.currentTimeMillis() - deviceTestStart > 3000) ? 0 : 1);
							payload[1] = stat;
						}
						PDU response = new PDU(PDU.STOCK_ECM_ID, PDU.DROID_ID, payload);
						out.write(response.getBytes());
						break;
					default:
						System.out.println("Unsupported command -> NACK");
						out.write(NACK.getBytes());
					}
				} catch (SocketException se) {
					s.close();
					break;
				} catch (Exception ioe) {
					ioe.printStackTrace();
				}
			}
		}
	}

	static EEPROM prepareEEPROM() throws IOException {
		EEPROM eeprom;
		if (RT_DATA == RT_DATA_BUE2D) {
			eeprom = new EEPROM("BUE2D");
			eeprom.setBytes(readResource("BUE2D.xpr"));
			Page page = eeprom.new Page(0, 40);
			page.setStart(eeprom.length() - 256);
			eeprom.addPage(page);

			int start = 0;
			for (int i = 1; i < 15; i++) {
				page = eeprom.new Page(i, 256);
				page.setStart(start);
				start += page.length();
				eeprom.addPage(page);
			}

			page = eeprom.new Page(15, 172);
			page.setStart(start);
			eeprom.addPage(page);
		} else {
			eeprom = new EEPROM("BUEIB");
			eeprom.setBytes(readResource("BUEIB.xpr"));
			Page page = eeprom.new Page(0, 4);
			page.setStart(eeprom.length() - 256);
			eeprom.addPage(page);

			int start = 0;
			page = eeprom.new Page(1, 256); page.setStart(start); eeprom.addPage(page); start += page.length();
			page = eeprom.new Page(2, 256); page.setStart(start); eeprom.addPage(page); start += page.length();
			page = eeprom.new Page(3, 158); page.setStart(start); eeprom.addPage(page); start += page.length();
			page = eeprom.new Page(4, 256); page.setStart(start); eeprom.addPage(page); start += page.length();
			page = eeprom.new Page(5, 256); page.setStart(start); eeprom.addPage(page); start += page.length();
			page = eeprom.new Page(6, 24);  page.setStart(start); eeprom.addPage(page); start += page.length();

		}
		return eeprom;
	}

	static byte[] readResource(String resource) throws IOException {
		InputStream in = Main.class.getResourceAsStream(resource);
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length;
		while ((length = in.read(buffer)) > 0){
			bao.write(buffer, 0, length);
		}
		in.close();
		return  bao.toByteArray();
	}

	static String hexdump(byte[] data, int offset, int len) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; (i  + offset) < Math.min(data.length, len); i++) {
			sb.append(":").append(String.format("%02X", data[i+offset]&0xFF));
		}
		return sb.length() > 0 ? sb.substring(1) : "<empty>";
	}
}
