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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA1 {
    static ThreadLocal<MessageDigest> md = new ThreadLocal<MessageDigest>() {

        @Override
        protected MessageDigest initialValue() {
            try {
                return MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                //impossible
                throw new AssertionError(e);
            }
        }
    };

    public byte[] doSHA1(byte[] bytes, int lenght) {
            MessageDigest digest = md.get();
            digest.reset();
            digest.update(bytes, 0, lenght);
            return digest.digest();
    }
}