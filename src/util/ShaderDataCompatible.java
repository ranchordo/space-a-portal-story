package util;

import static org.lwjgl.opengl.GL11.glGetIntegerv;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL15.glMapBuffer;
import static org.lwjgl.opengl.GL15.glUnmapBuffer;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;
import static org.lwjgl.opengl.GL43.*;

import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.vecmath.Vector3f;

import org.lwjgl.system.MemoryStack;

import graphics.Renderer;
import logger.Logger;

public abstract class ShaderDataCompatible {
	public boolean IGNORE_MISSING=false;
	private HashMap<String, SSBO> ssbo=new HashMap<String, SSBO>();
	public HashMap<String,Integer> locationCache=new HashMap<String,Integer>();
	public byte SSBOId=1;
	public HashMap<String, SSBO> getSSBOMappings() {return ssbo;}
	private boolean gotBindingLimits=false;
	private int lastBindingLimits=0;
	private boolean programSynced=false;
	private int program_internal;
	public void syncRequiredShaderDataValues(int program, boolean bindErrorPolicy) {
		if(programSynced) {
			throw new IllegalStateException("Don't sync the progam value multiple times, idiot. Isn't program initialization supposed to be contructor-exclusive?");
		}
		program_internal=program;
		programSynced=true;
		IGNORE_MISSING=bindErrorPolicy;
	}
	private int program() {
		if(!programSynced) {
			throw new IllegalStateException("Maybe try syncing the program state first, okay?");
		}
		return program_internal;
	}
	public int getBindingLimits() {
		if(!gotBindingLimits) {
			gotBindingLimits=true;
			try(MemoryStack stack=MemoryStack.stackPush()){
				IntBuffer x=Renderer.stack.mallocInt(1);
				glGetIntegerv(GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS,x);
				lastBindingLimits=x.get(0);
				return lastBindingLimits;
			}
		}
		return lastBindingLimits;
	}
	public int getResourceLocation(String name) {
		if(locationCache.containsKey(name)) {
			return locationCache.get(name);
		}
		int ret=glGetProgramResourceIndex(program(),GL_SHADER_STORAGE_BLOCK,name);
		locationCache.put(name,ret);
		return ret;
	}
	public SSBO generateNewSSBO(String name, long initialLength) {
		SSBO out=new SSBO();
		out.buffer=glGenBuffers();
		out.id=SSBOId;
		if(SSBOId>=Math.min(0xFF,getBindingLimits())) {
			throw new IllegalStateException("Ya hit the SSBO limit.");
		}
		SSBOId++;
		out.location=getResourceLocation(name);
		if(out.location==-1) {
			Logger.log(3,name+" is not a valid shader SSBO binding point.");
		}
		ssbo.put(name,out);
		glShaderStorageBlockBinding(program(), out.location, out.id);
		initSSBOData(initialLength,out);
		return out;
	}
	public void initSSBOData(long size, SSBO ssbo) {
		glBindBuffer(GL_SHADER_STORAGE_BUFFER,ssbo.buffer);
		glBufferData(GL_SHADER_STORAGE_BUFFER,size,GL_DYNAMIC_DRAW);
		glBindBuffer(GL_SHADER_STORAGE_BUFFER,0);
		glMemoryBarrier(GL_ALL_BARRIER_BITS);
	}
	public void updateSSBOData(FloatBuffer data, SSBO ssbo) {
		glBindBuffer(GL_SHADER_STORAGE_BUFFER,ssbo.buffer);
		try {
			FloatBuffer buffer=null;
			try {
				buffer=glMapBuffer(GL_SHADER_STORAGE_BUFFER, GL_WRITE_ONLY).order(ByteOrder.nativeOrder()).asFloatBuffer();
			} catch (NullPointerException e) {
				Logger.log(4,"Ladies and gentlemen: We have issues.");
			}
			if(data.capacity()!=buffer.capacity()) {
				throw new IllegalArgumentException("Uh... you realize this is a method for *copying*, right? Input capacity was "+data.capacity()+", current data's capacity was "+buffer.capacity());
			}
			buffer.position(0);
			for(int i=0;i<buffer.capacity();i++) {
				buffer.put(data.get(i));
			}
			//buffer.flip();
			if(!glUnmapBuffer(GL_SHADER_STORAGE_BUFFER)) {
				Logger.log(4,"Buffer map failure");
			}
		} catch (Throwable e) {
			Logger.log(0,"We caught it. No resource leak.");
			glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);
			throw e;
		}
		glBindBuffer(GL_SHADER_STORAGE_BUFFER,0);
		glMemoryBarrier(GL_ALL_BARRIER_BITS);
	}
	
	public void applyAllSSBOs() {
		for(Entry<String, SSBO> e : ssbo.entrySet()) {
			applySSBO(e.getValue());
		}
	}
	public void applySSBO(SSBO ssbo) {
		glBindBufferBase(GL_SHADER_STORAGE_BUFFER, ssbo.id, ssbo.buffer);
		glMemoryBarrier(GL_ALL_BARRIER_BITS);
	}
	
	
	
	public int getUniformLocation(String name) {
		if(locationCache.containsKey(name)) {
			return locationCache.get(name);
		}
		int ret=glGetUniformLocation(program(),name);
		locationCache.put(name,ret);
		return ret;
	}
	public void setUniform1f(String name, float value) {
		int location=getUniformLocation(name);
		if(location!=-1) {
			glUniform1f(location,value);
		} else {
			if(!IGNORE_MISSING) {Logger.log(3,name+" is not a valid shader uniform");}
		}
	}
	public void setUniform1i(String name, int value) {
		int location=getUniformLocation(name);
		if(location!=-1) {
			glUniform1i(location,value);
		} else {
			if(!IGNORE_MISSING) {Logger.log(3,name+" is not a valid shader uniform");}
		}
	}
	public void setUniform3f(String name, Vector3f in) {
		setUniform3f(name,in.x,in.y,in.z);
	}
	public void setUniform3f(String name, float x, float y, float z) {
		int location=getUniformLocation(name);
		if(location!=-1) {
			glUniform3f(location,x,y,z);
		} else {
			if(!IGNORE_MISSING) {Logger.log(3,name+" is not a valid shader uniform");}
		}
	}
	public void setUniform4f(String name, float x, float y, float z, float w) {
		int location=getUniformLocation(name);
		if(location!=-1) {
			glUniform4f(location,x,y,z,w);
		} else {
			if(!IGNORE_MISSING) {Logger.log(3,name+" is not a valid shader uniform");}
		}
	}
	public void setUniform3fv(String name, FloatBuffer d) {
		int location=getUniformLocation(name);
		if(location!=-1) {
			glUniform3fv(location, d);
		} else {
			if(!IGNORE_MISSING) {Logger.log(3,name+" is not a valid shader uniform");}
		}
	}
	public void setUniformiv(String name, IntBuffer d) {
		int location=getUniformLocation(name);
		if(location!=-1) {
			glUniform1iv(location, d);
		} else {
			if(!IGNORE_MISSING) {Logger.log(3,name+" is not a valid shader uniform");}
		}
	}
	public void setUniform4fv(String name, FloatBuffer d) {
		int location=getUniformLocation(name);
		if(location!=-1) {
			glUniform4fv(location, d);
		} else {
			if(!IGNORE_MISSING) {Logger.log(3,name+" is not a valid shader uniform");}
		}
	}
	public void setUniformMatrix4fv(String name, FloatBuffer b) {
		setUniformMatrix4fv(name,b,false);
	}
	public void setUniformMatrix4fv(String name, FloatBuffer b, boolean transpose) {
		int location=getUniformLocation(name);
		if(location!=-1) {
			glUniformMatrix4fv(location, transpose, b);
		} else {
			if(!IGNORE_MISSING) {Logger.log(3,name+" is not a valid shader uniform");}
		}
	}

	public void setUniformMatrix3fv(String name, FloatBuffer b) {
		int location=getUniformLocation(name);
		if(location!=-1) {
			glUniformMatrix3fv(location, false, b);
		} else {
			if(!IGNORE_MISSING) {Logger.log(3,name+" is not a valid shader uniform");}
		}
	}
}
