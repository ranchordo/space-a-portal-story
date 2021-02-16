package graphics;
import static org.lwjgl.opengl.GL20.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map.Entry;

import lighting.Lighting;
import logger.Logger;

public class Shader {
	public static HashMap<String, Integer> defaults=new HashMap<String, Integer>();
	public static void defaultInit() {
		defaults.put("tex",0);
		defaults.put("bump",1);
		defaults.put("norm",2);
		defaults.put("bloom",1);
		
		defaults.put("iPosition",0);
		defaults.put("iNormal",1);
		defaults.put("iNoise",2);
		defaultsInited=true;
	}
	private static boolean defaultsInited=false;
	public static final boolean IGNORE_MISSING=true;
	private int program;
	private int vs;
	private int fs;
	private String fname;
	public String getFname() {return fname;}
	public HashMap<String,Integer> locationCache=new HashMap<String,Integer>();
	public Shader(String fname) {
		this.fname=fname;
		program=glCreateProgram();
		vs=glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vs, readFile(fname+".vsh"));
		glCompileShader(vs);
		if(glGetShaderi(vs, GL_COMPILE_STATUS) != 1) {
			System.err.println(glGetShaderInfoLog(vs));
			System.exit(1);
		}
		
		fs=glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs, readFile(fname+".fsh"));
		glCompileShader(fs);
		if(glGetShaderi(fs, GL_COMPILE_STATUS) != 1) {
			System.err.println(glGetShaderInfoLog(fs));
			System.exit(1);
		}
		
		glAttachShader(program,vs);
		glAttachShader(program,fs);
		
		glBindAttribLocation(program,0,"glv");
		glBindAttribLocation(program,2,"gln");
		glBindAttribLocation(program,3,"glc");
		glBindAttribLocation(program,8,"mtc0");
		glBindAttribLocation(program,13,"material");
		glBindAttribLocation(program,14,"tangent");
		glBindAttribLocation(program,15,"bitangent");
		
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
		if(Renderer.activeShader==null) {return true;}
		return !tfname.equals(Renderer.activeShader.getFname());
	}
	public void bind() {
		if(!defaultsInited) {defaultInit();}
		if(checkActiveShaderName(this.fname)) {
			glUseProgram(program);
			for(Entry<String, Integer> e : Shader.defaults.entrySet()) {
				int loc=getUniformLocation(e.getKey());
				if(loc!=-1) {glUniform1i(loc,e.getValue());}
			}
			Renderer.activeShader=this;
			Lighting.apply();
		}
	}
	
	private String readFile(String fname) {
		StringBuilder string=new StringBuilder();
		BufferedReader b;
		try {
			b=new BufferedReader(new InputStreamReader(game.Main.class.getResourceAsStream("/shaders/"+fname)));
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
			Logger.log(4,"/shaders/"+fname+" does not exist.");
		}
		return string.toString();
	}
}
