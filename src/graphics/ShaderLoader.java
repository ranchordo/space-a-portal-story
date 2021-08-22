package graphics;

import java.util.HashMap;

import lepton.engine.rendering.InstanceAccumulator;
import lepton.engine.rendering.Shader;
import lepton.util.advancedLogger.Logger;

public class ShaderLoader {
	public HashMap<String,Shader> shaders=new HashMap<String,Shader>();
	public Shader load(String fname) {
		if(shaders.containsKey(fname)) {
			return shaders.get(fname);
		} else {
			Shader s=new Shader(fname);
			shaders.put(fname,s);
			return s;
		}
	}
}
