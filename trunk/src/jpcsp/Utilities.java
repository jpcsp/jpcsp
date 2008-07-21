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
package jpcsp;


public class Utilities {
  public static String formatString(String type , String oldstring)
  {
    int counter=0; 
    if(type.equals("byte")) counter=2;
    if(type.equals("short")) counter =4;
    if(type.equals("long")) counter =8;
    
    int len = oldstring.length();
   StringBuilder sb = new StringBuilder();
   while ( len++ < counter )
   {
     sb.append( '0' );
   }
    oldstring= sb.append(oldstring).toString();  
    return oldstring;
    
  }
  public static String integerToHex(int value)
  {
      return Integer.toHexString( 0x100 | value).substring(1).toUpperCase();
  }
}
