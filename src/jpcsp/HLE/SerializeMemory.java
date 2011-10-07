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
package jpcsp.HLE;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;

public class SerializeMemory {
	static public void serialize(Object object, Field objectField, DataOutputStream dataOutputStream) throws Throwable {
		Class<?> objectClass = object.getClass();

		if (objectClass == int.class) {
			dataOutputStream.writeInt((Integer)object);
			return;
		} 

		if (objectClass == short.class) {
			dataOutputStream.writeShort((Short)object);
			return;
		}
		
		if (objectClass == float.class) {
			dataOutputStream.writeFloat((Float)object);
			return;
		}
		
		if (objectClass == long.class) {
			dataOutputStream.writeLong((Long)object);
			return;
		}

		if (objectClass == String.class) {
			ISerializeString serializeString = objectClass.getAnnotation(ISerializeString.class);
			String string = (String)object;
			int size = serializeString.size();
			byte padWith = serializeString.padWith();
			byte[] data = string.getBytes(Charset.forName(serializeString.charset()));
			if (data.length >= size - 1) {
				throw(new Exception(String.format(
					"(Field '%s') contains a value too long %d", objectField, data.length
				)));
			}
			for (int n = 0; n < size; n++) {
				if (n < data.length) {
					dataOutputStream.writeByte(data[n]);
				} else {
					dataOutputStream.writeByte(padWith);
				}
			}
			
			return;
		}
		
		if (objectClass.isArray()) {
			ISerializeArray serializeArray = objectClass.getAnnotation(ISerializeArray.class);
			
			int count = serializeArray.size();
			int arrayLength = Array.getLength(object);
			
			if (count != arrayLength) {
				throw(new Exception(String.format(
					"(Field '%s') has a length mismatch %d != %d", objectField, count, arrayLength
				)));
			}
			
			for (int n = 0; n < count; n++) {
				serialize(Array.get(object, n), null, dataOutputStream);
			}
			
			return;
		}
		
		for (Field field : objectClass.getFields()) {
			int fieldModifiers = field.getModifiers();
			if ((fieldModifiers & Modifier.STATIC) != 0) continue;

			serialize(field.get(object), field, dataOutputStream);
		}
	}
	
	static public void serialize(Object object, OutputStream outputStream) {
		try {
			serialize(object, null, new DataOutputStream(outputStream));
		} catch (Throwable o) {
			throw(new RuntimeException(o.getCause()));
		}
	}

	@SuppressWarnings("unchecked")
	static public <T> T unserialize(Class<T> objectClass, Field objectField, DataInputStream dataInputStream) throws Throwable {
		if (objectClass == int.class) {
			return (T)(Integer)dataInputStream.readInt();
		} 

		if (objectClass == short.class) {
			return (T)(Short)dataInputStream.readShort();
		}
		
		if (objectClass == float.class) {
			return (T)(Float)dataInputStream.readFloat();
		}
		
		if (objectClass == long.class) {
			return (T)(Long)dataInputStream.readLong();
		}
		
		if (objectClass.isArray()) {
			ISerializeArray serializeArray = objectClass.getAnnotation(ISerializeArray.class);
			
			int count = serializeArray.size();
			Class<?> elementType = objectClass.getComponentType();

			Object object = Array.newInstance(objectClass, count);

			for (int n = 0; n < count; n++) {
				Array.set(object, n, unserialize(elementType, null, dataInputStream));
			}
		}
		
		T object = objectClass.newInstance();
		
		for (Field field : objectClass.getFields()) {
			int fieldModifiers = field.getModifiers(); 
			if ((fieldModifiers & Modifier.STATIC) != 0) continue;
			
			field.set(
				object,
				unserialize(field.get(object).getClass(), field, dataInputStream)
			);
		}
		
		return object;
	}

	static public <T> T unserialize(Class<T> objectClass, InputStream inputStream) {
		try {
			return unserialize(objectClass, null, new DataInputStream(inputStream));
		} catch (Throwable o) {
			throw(new RuntimeException(o.getCause()));
		}
	}
}
