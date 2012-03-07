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
package jpcsp.graphics;

import java.util.HashMap;

import jpcsp.util.ClassSpecializer;

import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 */
public class VertexInfoCompiler {
	private static Logger log = VideoEngine.log;
	private static VertexInfoCompiler instance;
	private HashMap<Integer, VertexInfoReaderTemplate> compiledVertexInfoReaders = new HashMap<Integer, VertexInfoReaderTemplate>();
	private VertexInfo vinfo = new VertexInfo();

	public static VertexInfoCompiler getInstance() {
		if (instance == null) {
			instance = new VertexInfoCompiler();
		}

		return instance;
	}

	private VertexInfoCompiler() {
	}

	public VertexInfoReaderTemplate getCompiledVertexInfoReader(int vtype, boolean readTexture) {
		int key = vtype;
		if (readTexture) {
			key |= 0x01000000;
		}
		if (log.isTraceEnabled()) {
			key |= 0x02000000;
		}
		VertexInfoReaderTemplate compiledVertexInfoReader = compiledVertexInfoReaders.get(key);
		if (compiledVertexInfoReader == null) {
			compiledVertexInfoReader = compileVertexInfoReader(key, vtype, readTexture);
			if (compiledVertexInfoReader != null) {
				compiledVertexInfoReaders.put(key, compiledVertexInfoReader);
			}
		}

		return compiledVertexInfoReader;
	}

	private VertexInfoReaderTemplate compileVertexInfoReader(int key, int vtype, boolean readTexture) {
		VertexInfo.processType(vinfo, vtype);

		if (log.isInfoEnabled()) {
			log.info(String.format("Compiling VertexInfoReader for %s", vinfo));
		}

		HashMap<String, Object> variables = new HashMap<String, Object>();
		// All these variables have to be defined as static members in the class VertexInfoReaderTemplate.
		variables.put("isLogTraceEnabled", Boolean.valueOf(log.isTraceEnabled()));
		variables.put("transform2D", Boolean.valueOf(vinfo.transform2D));
		variables.put("skinningWeightCount", Integer.valueOf(vinfo.skinningWeightCount));
		variables.put("morphingVertexCount", Integer.valueOf(vinfo.morphingVertexCount));
		variables.put("texture", Integer.valueOf(vinfo.texture));
		variables.put("color", Integer.valueOf(vinfo.color));
		variables.put("normal", Integer.valueOf(vinfo.normal));
		variables.put("position", Integer.valueOf(vinfo.position));
		variables.put("weight", Integer.valueOf(vinfo.weight));
		variables.put("index", Integer.valueOf(vinfo.index));
		variables.put("vtype", Integer.valueOf(vinfo.vtype));
		variables.put("readTexture", Boolean.valueOf(readTexture));
		variables.put("vertexSize", Integer.valueOf(vinfo.vertexSize));
		variables.put("oneVertexSize", Integer.valueOf(vinfo.oneVertexSize));
		variables.put("textureOffset", Integer.valueOf(vinfo.textureOffset));
		variables.put("colorOffset", Integer.valueOf(vinfo.colorOffset));
		variables.put("normalOffset", Integer.valueOf(vinfo.normalOffset));
		variables.put("positionOffset", Integer.valueOf(vinfo.positionOffset));
		variables.put("alignmentSize", Integer.valueOf(vinfo.alignmentSize));

		String specializedClassName = String.format("VertexInfoReader%07X", key);
		ClassSpecializer cs = new ClassSpecializer();
		Class<?> specializedClass = cs.specialize(specializedClassName, VertexInfoReaderTemplate.class, variables);
		VertexInfoReaderTemplate compiledVertexInfoReader = null;
		if (specializedClass != null) {
			try {
				compiledVertexInfoReader = (VertexInfoReaderTemplate) specializedClass.newInstance();
			} catch (InstantiationException e) {
				log.error("Error while instanciating compiled vertexInfoReader", e);
			} catch (IllegalAccessException e) {
				log.error("Error while instanciating compiled vertexInfoReader", e);
			}
		}

		return compiledVertexInfoReader;
	}
}
