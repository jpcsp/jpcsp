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

package jpcsp.crypto;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.util.encoders.Hex;

public class ECDSA {
    private KeyPair keyPair;
    private ECCurve curve;
    private ECParameterSpec spec;
    private KeyPairGenerator g;
    private KeyFactory f;

    public ECDSA() {
    }

    public void setCurve() {
        try {
            curve = new ECCurve.Fp(
                    new BigInteger("FFFFFFFFFFFFFFFF00000001FFFFFFFFFFFFFFFF", 16),  // p 
                    new BigInteger("FFFFFFFFFFFFFFFF00000001FFFFFFFFFFFFFFFC", 16),  // a
                    new BigInteger("A68BEDC33418029C1D3CE33B9A321FCCBB9E0F0B", 16)); // b
            
            spec = new ECParameterSpec(curve, 
                    curve.decodePoint(Hex.decode("128EC456487FD8FDF64E2437BC0A1F6D5AFDE2C5958557EB1DB001260425524DBC379D5AC5F4ADF")), // G
                    new BigInteger("00FFFFFFFFFFFFFFFEFFFFB5AE3C523E63944F2127")); // n
            
            g = KeyPairGenerator.getInstance("ECDSA", "BC");
            f = KeyFactory.getInstance("ECDSA", "BC");
            g.initialize(spec, new SecureRandom());
            
            keyPair = g.generateKeyPair();
        } catch (Exception e) {
        }
    }
    
    public void sign(byte[] hash, byte[] priv, byte[] R, byte[] S) {
        // TODO
    }
    
    public void verify(byte[] hash, byte[] pub, byte[] R, byte[] S) {
        // TODO
    }
    
    public byte[] getPrivateKey() {
        return keyPair.getPrivate().getEncoded();
    }
    
    public byte[] getPublicKey() {
        return keyPair.getPublic().getEncoded();
    }
    
    public byte[] multiplyPublicKey(byte[] pub, byte[] priv) {
        PublicKey multPubKey = null;
        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(priv), spec);
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(curve.decodePoint(pub), spec);
        ECPublicKeySpec newPublicKeySpec = new ECPublicKeySpec(publicKeySpec.getQ().multiply(privateKeySpec.getD()), spec);
        try {
            multPubKey = f.generatePublic(newPublicKeySpec);
        } catch (Exception e) {  
        }       
        return multPubKey.getEncoded();
    }
}