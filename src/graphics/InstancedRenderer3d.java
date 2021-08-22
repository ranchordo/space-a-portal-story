package graphics;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map.Entry;

import org.lwjgl.BufferUtils;

import com.bulletphysics.linearmath.Transform;

import lepton.engine.rendering.GLContextInitializer;
import lepton.engine.rendering.GObject;
import lepton.engine.rendering.Shader;
import lepton.engine.rendering.Texture;
import lepton.util.LeptonUtil;
import lepton.util.advancedLogger.Logger;

public class InstancedRenderer3d {
	public interface InstancedRenderRoutine3d {
		public void run();
	}
	private HashMap<InstancedRenderConfig3d,InstancedRenderConfig3d> renderConfigs=new HashMap<InstancedRenderConfig3d,InstancedRenderConfig3d>();
	private InstancedRenderConfig3d hashObject;
	public InstancedRenderConfig3d loadConfiguration(Shader s, Texture t, GObject g, int objectSize, String ssbo_name) {
		if(hashObject==null) {
			hashObject=new InstancedRenderConfig3d(s,t,g,0,null);
		} else {
			hashObject.shader=s;
			hashObject.tex=t;
		}
		if(!renderConfigs.containsKey(hashObject)) {
			InstancedRenderConfig3d nconfig=new InstancedRenderConfig3d(s,t,g,objectSize,ssbo_name);
			renderConfigs.put(nconfig,nconfig);
			Logger.log(0,"Generating new 3d instanced render configuration for shader with name "+s.getFname());
			return nconfig;
		}
		return renderConfigs.get(hashObject);
	}
	private Transform mmc=new Transform();
	private float[] mma=new float[16];
	private FloatBuffer fm=BufferUtils.createFloatBuffer(16);
	public void renderInstanced(InstancedRenderRoutine3d renderRoutine) {
		for(Entry<InstancedRenderConfig3d,InstancedRenderConfig3d> rc : renderConfigs.entrySet()) {
			rc.getValue().instanceAccumulator.reset();
		}
		renderRoutine.run();
		for(Entry<InstancedRenderConfig3d,InstancedRenderConfig3d> e : renderConfigs.entrySet()) {
			InstancedRenderConfig3d s=e.getKey();
			if(s.instanceAccumulator.getBuffer().position()==0) {
				Logger.log(0,"Instanced buffer for shader with name "+s.shader.getFname()+" was empty. Skipping rendering for this round.");
				continue;
			}
			s.instanceAccumulator.submit();
			s.shader.bind();
			mmc.set(GLContextInitializer.cameraTransform);
			if(s.geo.viewMatrixModifier!=null) {
				mmc=s.geo.viewMatrixModifier.modifyViewMatrix(mmc);
			}
			mmc.getOpenGLMatrix(mma);
			fm=LeptonUtil.asFloatBuffer(mma,fm);
			GLContextInitializer.activeShader.setUniformMatrix4fv("world2view",fm);

			LeptonUtil.openGLMatrix(GLContextInitializer.proj_matrix,mma);
			fm=LeptonUtil.asFloatBuffer(mma,fm);
			s.shader.setUniformMatrix4fv("proj_matrix",fm);
			s.shader.setUniform1i("millis",(int)(LeptonUtil.micros())); //This is really dumb and I hate it.
			s.shader.setUniform1f("useLighting", (s.geo.useLighting && GLContextInitializer.useGraphics) ? 2 : 0);
			s.shader.setUniform1i("textureUse", s.tex==null?0:s.tex.loadedBitflag());
//			s.shader.applyAllSSBOs();
			int prevInstances=s.geo.instances;
			if(s.tex!=null) {
//				s.tex.bind();
			}
//			s.geo.instances=(s.instanceAccumulator.getBuffer().position())/s.instanceAccumulator.objectSize;
			s.geo.render_raw();
			s.geo.instances=prevInstances;
		}
	}
}
