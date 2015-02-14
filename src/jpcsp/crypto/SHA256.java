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

public class SHA256 {

    public SHA256() {
    }

    public byte[] doSHA256(byte[] bytes, int lenght) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] sha256Hash = new byte[40];
            md.update(bytes, 0, lenght);
            sha256Hash = md.digest();
            return sha256Hash;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}