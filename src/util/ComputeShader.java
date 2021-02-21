package util;
import static org.lwjgl.opengl.GL43.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map.Entry;

import graphics.Renderer;
import lighting.Lighting;
import logger.Logger;

public class ComputeShader {
	public static HashMap<String, Integer> defaults=new HashMap<String, Integer>();
	public static void defaultInit() {
		
		defaultsInited=true;
	}
	private static boolean defaultsInited=false;
	public static final boolean IGNORE_MISSING=true;
	private int program;
	private int cs;
	private String fname;
	public String getFname() {return fname;}
	public HashMap<String,Integer> locationCache=new HashMap<String,Integer>();
	public ComputeShader(String fname) {
		this.fname=fname;
		program=glCreateProgram();
		cs=glCreateShader(GL_COMPUTE_SHADER);
		glShaderSource(cs, readFile(fname+".csh"));
		glCompileShader(cs);
		if(glGetShaderi(cs, GL_COMPILE_STATUS) != 1) {
			System.err.println(glGetShaderInfoLog(cs));
			System.exit(1);
		}
		
		glAttachShader(program,cs);
		
		glLinkProgram(program);
		if(glGetProgrami(program,GL_LINK_STATUS)!=1) {
			System.err.println(glGetProgramInfoLog(program));
			System.exit(1);
		}
		glValidateProgram(program);
		if(glGetProgrami(program,GL_VALIDATE_STATUS)!=1) {
			System.err.println(glGetProgramInfoLog(program));
			System.exit(1);
		}
	}
	public int getUniformLocation(String name) {
		if(locationCache.containsKey(name)) {
			return locationCache.get(name);
		}
		int ret=glGetUniformLocation(program,name);
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
	private static boolean checkActiveShaderName(String tfname) {
		if(Renderer.activeComputeShader==null) {return true;}
		return !tfname.equals(Renderer.activeComputeShader.getFname());
	}
	public void bind() {
		if(!defaultsInited) {defaultInit();}
		if(checkActiveShaderName(this.fname)) {
			glUseProgram(program);
			for(Entry<String, Integer> e : ComputeShader.defaults.entrySet()) {
				int loc=getUniformLocation(e.getKey());
				if(loc!=-1) {glUniform1i(loc,e.getValue());}
			}
			Renderer.activeComputeShader=this;
			Lighting.apply();
		}
	}
	
	private String readFile(String fname) {
		StringBuilder string=new StringBuilder();
		BufferedReader b;
		try {
			b=new BufferedReader(new InputStreamReader(game.Main.class.getResourceAsStream("/compute_shaders/"+fname)));
			String line;
			while((line=b.readLine())!=null) {
				string.append(line);
				string.append("\n");
			}
			b.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (NullPointerException e) {
			Logger.log(4,"/compute_shaders/"+fname+" does not exist.");
		}
		return string.toString();
	}
}
