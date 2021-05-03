package util;
import static org.lwjgl.opengl.GL43.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.IntBuffer;
import javax.vecmath.Vector4f;

import logger.Logger;
import org.lwjgl.system.MemoryStack;

import graphics.Renderer;

public class ComputeShader extends ShaderDataCompatible {
	public static final boolean IGNORE_MISSING=true;
	private int program;
	private int cs;
	private String fname;
	public String getFname() {return fname;}
	public ComputeShader(String fname) {
		this.fname=fname;
		program=glCreateProgram();
		syncRequiredShaderDataValues(program, IGNORE_MISSING);
		cs=glCreateShader(GL_COMPUTE_SHADER);
		glShaderSource(cs, readFile(fname+".cpsh"));
		glCompileShader(cs);
		if(glGetShaderi(cs, GL_COMPILE_STATUS) != 1) {
			Logger.log(3,"In shader "+fname+".cpsh:");
			Logger.log(4,glGetShaderInfoLog(cs));
		}
		
		glAttachShader(program,cs);
		
		glLinkProgram(program);
		if(glGetProgrami(program,GL_LINK_STATUS)!=1) {
			Logger.log(4,glGetProgramInfoLog(program));
		}
		glValidateProgram(program);
		if(glGetProgrami(program,GL_VALIDATE_STATUS)!=1) {
			Logger.log(4,glGetProgramInfoLog(program));
		}
		Logger.log(0,"Loaded compute shader \""+fname+"\" successfully.");
	}
	private static Vector4f ret=new Vector4f();
	private static boolean gotLimits=false;
	public static Vector4f getGlobalWorkLimits() {
		if(!gotLimits) {
			gotLimits=true;
			try(MemoryStack stack=MemoryStack.stackPush()){
				IntBuffer x=stack.mallocInt(1);
				IntBuffer y=stack.mallocInt(1);
				IntBuffer z=stack.mallocInt(1);
				IntBuffer w=stack.mallocInt(1);
				glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT,0,x);
				glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT,1,y);
				glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT,2,z);
				glGetIntegerv(GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS,w);
				ret.set(x.get(0),y.get(0),z.get(0),w.get(0));
				return ret;
			}
		}
		return ret;
	}
	private static boolean checkActiveShaderName(String tfname) {
		if(Renderer.activeComputeShader==null) {return true;}
		if(Renderer.shaderSwitch!=Renderer.COMPUTE_SHADER) {return true;}
		return !tfname.equals(Renderer.activeComputeShader.getFname());
	}
	public void bind() {
		if(checkActiveShaderName(this.fname)) {
			glUseProgram(program);
			Renderer.activeComputeShader=this;
			Renderer.shaderSwitch=Renderer.COMPUTE_SHADER;
		}
	}
	private String readFile(String fname) {
		StringBuilder string=new StringBuilder();
		BufferedReader b;
		try {
			b=new BufferedReader(new InputStreamReader(ComputeShader.class.getResourceAsStream("/compute_shaders/"+fname)));
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
	public void dispatch(int w, int h, int d) {
		if((w>getGlobalWorkLimits().x)||(h>getGlobalWorkLimits().y)||(d>getGlobalWorkLimits().z)) {
			throw new IllegalStateException("You are dispatching too many threads. Global 3d-structured limits are "+getGlobalWorkLimits()+". You called this with ("+w+", "+h+", "+d+").");
		}
		setUniform3f("invocation_dimensions",w,h,d);
		glDispatchCompute(w,h,d);
	}
}