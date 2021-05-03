package particles;

import static org.lwjgl.opengl.GL15.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL15.GL_READ_ONLY;
import static org.lwjgl.opengl.GL15.glDepthMask;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Stream;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import graphics.RenderUtils;
import graphics.Shader;
import graphics.Tri;
import logger.Logger;
import objectTypes.GObject;
import physics.Physics;
import util.ComputeShader;
import util.SSBO;
import util.ShaderDataCompatible;
import util.Util;

public class ParticleSystem {
	/*
	 * f3-pos
	 * f-active
	 * f3-vel
	 */
	
	protected GObject geo;
	
	private static Random particleRandomness=new Random();
	
	public static final int bytesPerParticle=2*4*4;
	public static final int bytesPerEmitter=3*4*4;
	
	private boolean locked=false;
	private boolean inited=false;
	private ArrayList<ParticleEmitter> emitters=new ArrayList<ParticleEmitter>();
	
	private SSBO particles;
	private SSBO emitters_ssbo;
	private ComputeShader initializer;
	private ComputeShader processor;
	private String shaderName="particles/default";
	
	private float lifeTime=4;
	private int spawnsPerSecond;
	private int totalParticles;
	
	private boolean active=true;
	private long lastActiveChange=0;
	
	public void setActive(boolean a) {
		if(a&&!active) {
			initializer.bind();
			initializer.applyAllSSBOs();
			initializer.dispatch(totalParticles,1,1);
		}
		if(active!=a) {lastActiveChange=RenderUtils.micros();}
		active=a;
	}
	
	public void setShaderName(String nn) {
		shaderName=nn;
	}
	private void cl() {
		if(!inited) {
			throw new IllegalStateException("Please initialize before operating on this ParticleSystem.");
		}
	}
	public void lock() {
		locked=true;
	}
	public void addEmitter(ParticleEmitter e) {
		if(locked) {
			throw new SecurityException("Emitters are locked, ya dumbo.");
		}
		emitters.add(e);
	}
	public void stepParticles() {
		if(active || (RenderUtils.micros()-lastActiveChange)<=lifeTime*1000000) {
			processor.bind();
			processor.setUniform1f("fr",RenderUtils.fr);
			processor.setUniform3f("gravity",Physics.getGravity());
			processor.setUniform1f("drag",0.005f);
			processor.applyAllSSBOs();
			processor.dispatch(totalParticles,1,1);
			
			initializer.bind();
			initializer.setUniform1i("setActive",active?1:0);
			initializer.applyAllSSBOs();
			float iprob=RenderUtils.targ_fr/(float)spawnsPerSecond;
			if(iprob>=1) {
				if(particleRandomness.nextInt(Math.round(iprob*10))<10) {
					initializer.dispatch(1,1,1);
				}
			} else {
				for(int i=0;i<Math.ceil(1.0/iprob);i++) {
					float niprob=iprob/(float)Math.ceil(iprob);
					if(particleRandomness.nextInt(Math.round(niprob)>0?Math.round(niprob*10):1)<10) {
						initializer.dispatch(1,1,1);
					}
				}
			}
		}
	}
	public void refreshEmitterData() {
		if(!locked) {
			throw new IllegalStateException("How is it not locked by now???");
		}
		FloatBuffer b=ShaderDataCompatible.mappify(emitters_ssbo,GL_WRITE_ONLY); {
			b.position(0);
			for(ParticleEmitter e : emitters) {
				b.put(e.worldLocation.x);
				b.put(e.worldLocation.y);
				b.put(e.worldLocation.z);
				b.put(e.radius);
				b.put(e.addedVelocity.x);
				b.put(e.addedVelocity.y);
				b.put(e.addedVelocity.z);
				b.put(e.particlesPerSecond);
				b.put(e.randomVelocity.x);
				b.put(e.randomVelocity.y);
				b.put(e.randomVelocity.z);
				b.put(0);
			}
		} ShaderDataCompatible.unMappify();
	}
	public void render() {
		if(true) {//active || (RenderUtils.micros()-lastActiveChange)<=lifeTime*1000000) {
			glDepthMask(false);
			this.geo.highRender_noPushPop_customTransform(null);
			glDepthMask(true);
		}
	}
	public void initialize() {
		float s=0.010f;
		this.geo=new GObject();
		this.geo.useTex=true;
		this.geo.useBump=false;
		this.geo.useLighting=false;
		this.geo.vmap.vertices=new ArrayList<Vector3f>();
		this.geo.vmap.vertices.add(new Vector3f(-s,-s,0));
		this.geo.vmap.vertices.add(new Vector3f(+s,-s,0));
		this.geo.vmap.vertices.add(new Vector3f(+s,+s,0));
		this.geo.vmap.vertices.add(new Vector3f(-s,+s,0));
		this.geo.vmap.normals=new ArrayList<Vector3f>();
		this.geo.vmap.normals.add(new Vector3f(0,0,-1));
		this.geo.vmap.texcoords=new ArrayList<Vector2f>();
		this.geo.vmap.texcoords.add(new Vector2f(0,1));
		this.geo.vmap.texcoords.add(new Vector2f(1,1));
		this.geo.vmap.texcoords.add(new Vector2f(1,0));
		this.geo.vmap.texcoords.add(new Vector2f(0,0));
		
		this.geo.clearTris();
		this.geo.addTri(new Tri(2,1,0, 0,0,0).setTexCoords(1,2,3));
		this.geo.addTri(new Tri(0,3,2, 0,0,0).setTexCoords(3,0,1));
		this.geo.lock();
		
		this.geo.vmap.tex.colorLoaded=true;
		
		try {
			this.geo.loadTexture("3d/particles/generic.png");
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		
		Shader particleShader=new Shader("specific/particle");
		this.geo.setRenderingShader(particleShader);
		
		this.geo.setColor(1,1,1,1f);
		this.geo.initVBO();
		this.geo.refresh();
		
		lock();
		inited=true;
		spawnsPerSecond=0;
		for(ParticleEmitter emitter : emitters) {
			spawnsPerSecond+=emitter.particlesPerSecond;
		}
		int gcd=Util.gcd((emitters.stream().map(emitter -> Math.round(emitter.particlesPerSecond)))::iterator);
		totalParticles=Math.round(spawnsPerSecond*lifeTime);
		this.geo.instances=totalParticles;
		initializer=new ComputeShader(shaderName+"INIT");
		processor=new ComputeShader(shaderName+"PROC");
		particles=initializer.generateNewSSBO("particles_buffer",totalParticles*bytesPerParticle);
		SSBO sampletable=initializer.generateNewSSBO("sample_buffer",(spawnsPerSecond*4)/gcd);
		emitters_ssbo=initializer.generateNewSSBO("emitters_buffer",emitters.size()*bytesPerEmitter);
		initializer.generateNewSSBO("persistentCounter",2*4);
		processor.generateFromExistingSSBO("particles_buffer",particles);
		//processor.generateFromExistingSSBO("emitters_buffer",emitters_ssbo);
		particleShader.generateFromExistingSSBO("particles_buffer",particles);
		ShaderDataCompatible.clearSSBOData(particles);
		
		refreshEmitterData();
		
		//Populate sample table
		FloatBuffer f1=ShaderDataCompatible.mappify(sampletable,GL_WRITE_ONLY); {
			f1.position(0);
			for(int e=0;e<emitters.size();e++) {
				for(int i=0;i<emitters.get(e).particlesPerSecond/gcd;i++) {
					f1.put(e);
				}
			}
		} ShaderDataCompatible.unMappify();
		
		initializer.bind();
		initializer.applyAllSSBOs();
		initializer.dispatch(totalParticles,1,1);
		
//		FloatBuffer f=ShaderDataCompatible.mappify(particles,GL_READ_ONLY); {
//			for(int i=0;i<f.capacity();i++) {
//				System.out.println(f.get(i));
//			}
//		} ShaderDataCompatible.unMappify();
		
		Logger.log(0,"Initialized particle system with "+totalParticles+" particle capacity and a sampling table size of "+(spawnsPerSecond)/gcd+" elements ("+(spawnsPerSecond*4)/gcd+" bytes).");
	}
}
