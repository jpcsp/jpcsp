/*
This file is part of jpcsp.

Jpcsp is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Jpcsp is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */
package jpcsp.HLE.modules500;

import org.apache.log4j.Logger;

import jpcsp.crypto.CryptoEngine;
import jpcsp.crypto.KeyVault;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.modules.HLEModule;

@HLELogging
public class scePauth extends HLEModule {
    public static Logger log = Modules.getLogger("scePauth");

    @Override
	public String getName() {
		return "scePauth";
	}

    @HLEUnimplemented
    @HLEFunction(nid = 0xF7AA47F6, version = 500)
    public int scePauth_F7AA47F6(TPointer inputAddr, int inputLength, @CanBeNull TPointer32 resultLengthAddr, TPointer keyAddr) {
    	CryptoEngine crypto = new CryptoEngine();
        byte[] in = inputAddr.getArray8(inputLength);
        byte[] key = keyAddr.getArray8(0x10);
        byte[] xor = new byte[0x10];
        for(int i = 0; i < 0x10; i++) {
            xor[i] = (byte)(KeyVault.pauthXorKey[i] & 0xFF);
        }

        // Decryption is not working properly due to a missing KIRK key.
        int reslength = crypto.getPRXEngine().DecryptPRX(in, inputLength, null, 0, 5, key, xor);
    	
        // Faking the result.
        inputAddr.clear(reslength);
        resultLengthAddr.setValue(reslength);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x98B83B5D, version = 500)
    public int scePauth_98B83B5D(TPointer inputAddr, int inputLength,  @CanBeNull TPointer32 resultLengthAddr, TPointer keyAddr) {
        CryptoEngine crypto = new CryptoEngine();
        byte[] in = inputAddr.getArray8(inputLength);
        byte[] key = keyAddr.getArray8(0x10);
        byte[] xor = new byte[0x10];
        for(int i = 0; i < 0x10; i++) {
            xor[i] = (byte)(KeyVault.pauthXorKey[i] & 0xFF);
        }

        // Decryption is not working properly due to a missing KIRK key.
        int reslength = crypto.getPRXEngine().DecryptPRX(in, inputLength, null, 0, 5, key, xor);
    	
        // Faking the result.
        inputAddr.clear(reslength);
        resultLengthAddr.setValue(reslength);

    	return 0;
    }
}
