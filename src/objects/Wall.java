package objects;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Tuple4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.Transform;

import game.Main;
import lepton.engine.physics.PhysicsObject;
import lepton.engine.physics.WorldObject;
import lepton.engine.rendering.GObject;
import lepton.engine.rendering.Texture;
import lepton.engine.rendering.Tri;
import lepton.optim.objpoollib.DefaultVecmathPools;
import lepton.optim.objpoollib.PoolElement;
import lepton.util.LeptonUtil;
import lepton.util.advancedLogger.Logger;

public class Wall extends Thing {
	private static final long serialVersionUID = -4731923936294407139L;
	public static final int SINGLE=0;
	public static final int DOUBLE=1;
	public static final int ALL=2;
	public static final String[] textures=new String[] {"3d/wall/cropped_border","3d/wall/cropped_border","3d/wall/cropped_border"};
	private Vector3f origin;
	private Quat4f quat;
	public Vector2f aspect=new Vector2f(1,1.6f);
	public Vector2f texOffset=new Vector2f(0,0);
	public int textureType=0;
	public int sided=SINGLE;
	/*
	 * Instanced object format:
	 * mat4 worldtransform (16)
	 * vec3 shape (4)
	 * vec4 texinfo (widths,texOffset) (4)
	 * mat4 world2view
	 */
	public static final int objectSize=16+4+4+16;
	public Wall(Vector2f shape, Vector3f origin, Quat4f quat) {
		this.type="Wall";
		this.shape=new Vector3f(shape.x,shape.y,0.3f); //shape.z - wall "thickness"
		this.origin=origin;
		this.quat=quat;
		this.isScaleNormalized=true;
	}
	public Wall() {
		this.type="Wall";
		this.shape=new Vector3f(1,1,1);
		this.isScaleNormalized=true;
		origin=new Vector3f(0,0,0);
		quat=new Quat4f(1,0,0,0);
	}
	@Thing.DesignerParameter(name="Shape",desc="XYZ scale of the wall. Applied after rotation.")
	public void setShape(Float f0, Float f1, Float f2) {
		this.shape.set(f0,f1,f2);
		this.geo.p.removePhysics(physicsWorld);
		this.initPhysics();
		this.addPhysics();
	}
	@Thing.DesignerParameter(name="Origin",desc="Position of the center of the wall in 3d space.")
	public void setOrigin(Float f0, Float f1, Float f2) {
		if(this.origin==null) {
			this.origin=new Vector3f(f0,f1,f2);
		} else {
			this.origin.set(f0,f1,f2);
		}
		PoolElement<Matrix4f> pe1=DefaultVecmathPools.matrix4f.alloc();
		PoolElement<Transform> pe2=DefaultVecmathPools.transform.alloc();
		this.geo.p.body.getWorldTransform(pe2.o()).getMatrix(pe1.o());
		pe1.o().set(quat,origin,1);
		pe2.o().set(pe1.o());
		this.geo.p.body.setWorldTransform(pe2.o());
		pe2.free();
		pe1.free();
	}
	private AxisAngle4f axisAngle=null;
	@Thing.DesignerParameter(name="Rotation",desc="Rotation of the wall in 3d space. Conforms to vecmath.AxisAngle4f specifications. X, Y, Z, THETA. Also refreshes origin.")
	public void setRotation(Float f0, Float f1, Float f2, Float f3) {
		if(axisAngle==null) {
			axisAngle=new AxisAngle4f(f0,f1,f2,f3);
		} else {
			axisAngle.set(f0,f1,f2,f3);
		}
		PoolElement<Quat4f> peq=LeptonUtil.AxisAngle(axisAngle);
		this.quat.set(peq.o());
		peq.free();
		setOrigin(this.origin.x,this.origin.y,this.origin.z); //Refresh stuff
	}
	@Thing.DesignerParameter(name="Type",desc="Also controls portalability. Will round to nearest integer. Range limited.")
	public void setTextureTypeWRefresh(Float tt) {
		textureType=Math.round(tt);
		textureType=Math.max(textureType,0);
		textureType=Math.min(textureType,textures.length);
		initTexture();
	}
	@Thing.DesignerParameter(name="Aspect",desc="Aspect ratio of texture.")
	public Wall setAspectWRefresh(Float f0, Float f1) {
		aspect.set(f0,f1);
		return this;
	}
	@Thing.DesignerParameter(name="Tex off",desc="Offset of texture on wall.")
	public Wall setTextureOffsetWRefresh(Float f0, Float f1) {
		texOffset.set(f0,f1);
		return this;
	}
	@Thing.DesignerParameter(name="Side mode",desc="0=One side, 1=Two sides, 2=All sides. Range limited. Will round to nearest integer.")
	public Wall setSideModeWRefresh(Float in) {
		sided=Math.round(in);
		sided=Math.max(sided,SINGLE);
		sided=Math.min(sided,ALL);
//		this.geo.g.vmap.tex.delete();
//		this.geo.g.clean();
		initGeo();
//		this.geo.g.initVBO();
		this.geo.g.unlock();
		this.geo.g.refresh();
		this.geo.g.lock();
		return this;
	}
	public Wall setTextureType(int tt) {
		textureType=tt;
		return this;
	}
	public Wall setAspect(Vector2f naspect) {
		aspect=naspect;
		return this;
	}
	public Wall setTextureOffset(Vector2f nto) {
		texOffset=nto;
		return this;
	}
	public Wall setSideMode(int in) {
		sided=in;
		return this;
	}
	public void initPhysics() {
		if(this.geo.p==null) {this.geo.p=new PhysicsObject();}
		this.geo.p.mass=0;
		CollisionShape s=new BoxShape(getShape());
		RigidBodyConstructionInfo body=this.geo.p.initPhysics_shape(s, origin, quat);
		body.restitution=1;
		body.friction=1f;
		this.geo.p.doBody(body);
	}
	private float[] data=new float[objectSize];
	private float[] world2viewtemp=new float[16];
	private transient Transform tr=null;
	private transient Matrix4f m=null;
	@Override public void render() {
		if(!this.geo.g.hasAlpha) {
//			if(instancedRenderConfig.instanceAccumulator.getBuffer().position()+1<instancedRenderConfig.instanceAccumulator.getBuffer().capacity()) {
//				if(instancedRenderConfig.instanceAccumulator.getBuffer().get(instancedRenderConfig.instanceAccumulator.getBuffer().position()+19)==Main.activePortalTransform) {
//					instancedRenderConfig.instanceAccumulator.reserveObject();
//					return;
//				}
//			}
			if(tr==null) {tr=new Transform();}
			if(m==null) {m=new Matrix4f();}
			this.geo.p.body.getWorldTransform(tr).getMatrix(m);
			m.transpose();
			LeptonUtil.openGLMatrix(m,data);
			m.set(1);
			tr.set(m);
			if(geo.g.viewMatrixModifier!=null) {
				tr=geo.g.viewMatrixModifier.modifyViewMatrix(tr);
			}
			tr.getOpenGLMatrix(world2viewtemp);
			data[16]=shape.x;
			data[17]=shape.y;
			data[18]=shape.z;
			data[19]=Main.activePortalTransform;
			data[20]=texOffset.x;
			data[21]=texOffset.y;
			data[22]=shape.x/aspect.x;
			data[23]=shape.y/aspect.y;
			for(int i=0;i<16;i++) {
				data[24+i]=world2viewtemp[i];
			}
			instancedRenderConfig.instanceAccumulator.add(data);
		}
	}
	@Override public void alphaRender() {
		if(this.geo.g.hasAlpha) {
			Logger.log(4,"Instanced Thing with type "+type+" cannot have a non-1 alpha component on any piece of its geometry.");
		}
	}
	private void initTexture() {
		if(this.geo.g.vmap.tex.anyLoaded()) {
			this.geo.g.vmap.tex=new Texture(this.geo.g.vmap.tex);
		}
		this.portalable=true;
		if(textureType==1) {
			this.geo.g.setColor(0.2f,0.2f,0.25f);
			this.portalable=false;
		}
		try {
			this.geo.g.loadTexture("assets/"+textures[textureType],"jpg");
			if(textureType==2) {
				this.geo.g.vmap.tex=new Texture(this.geo.g.vmap.tex);
				this.geo.g.vmap.tex.reset(Texture.NORMAL);
				this.geo.g.vmap.tex.create(1,"assets/3d/wall/cropped_border_normal_detail",".jpg");
			}
		} catch (IOException e) {
			Logger.log(4,e.toString(),e);
		}
		instancedRenderConfig=Main.instancedRenderer.loadConfiguration(Main.shaderLoader.load("genericInstanced"),this.geo.g.vmap.tex,this.geo.g,objectSize,"info_buffer");
	}
	public void initGeo() {
		if(this.geo==null) {this.geo=new WorldObject();}
		if(this.geo.g==null) {this.geo.g=new GObject();}
		this.geo.g.unlock();
		this.geo.g.useTex=true;
		this.geo.g.vmap.vertices=new ArrayList<Vector3f>();
		this.geo.g.vmap.vertices.add(new Vector3f(-1,-1,-1));
		this.geo.g.vmap.vertices.add(new Vector3f(+1,-1,-1));
		this.geo.g.vmap.vertices.add(new Vector3f(+1,+1,-1));
		this.geo.g.vmap.vertices.add(new Vector3f(-1,+1,-1));
		if(sided>=DOUBLE) {
			this.geo.g.vmap.vertices.add(new Vector3f(-1,-1,+1));
			this.geo.g.vmap.vertices.add(new Vector3f(+1,-1,+1));
			this.geo.g.vmap.vertices.add(new Vector3f(+1,+1,+1));
			this.geo.g.vmap.vertices.add(new Vector3f(-1,+1,+1));
		}
		this.geo.g.vmap.normals=new ArrayList<Vector3f>();
		this.geo.g.vmap.normals.add(new Vector3f(0,0,-1));
		this.geo.g.vmap.normals.add(new Vector3f(0,0, 1));
		this.geo.g.vmap.normals.add(new Vector3f(0,-1,0));
		this.geo.g.vmap.normals.add(new Vector3f(0, 1,0));
		this.geo.g.vmap.normals.add(new Vector3f( 1,0,0));
		this.geo.g.vmap.normals.add(new Vector3f(-1,0,0));
		this.geo.g.vmap.texcoords=new ArrayList<Vector2f>();
		this.geo.g.vmap.texcoords.add(new Vector2f(0,1));
		this.geo.g.vmap.texcoords.add(new Vector2f(1,1));
		this.geo.g.vmap.texcoords.add(new Vector2f(1,0));
		this.geo.g.vmap.texcoords.add(new Vector2f(0,0));
		this.geo.g.clearTris();
		this.geo.g.addTri(new Tri(2,1,0, 0,0,0).setTexCoords(1,2,3));
		this.geo.g.addTri(new Tri(0,3,2, 0,0,0).setTexCoords(3,0,1));
		if(sided>=DOUBLE) {
			this.geo.g.addTri(new Tri(4,5,6, 1,1,1).setTexCoords(3,2,1));
			this.geo.g.addTri(new Tri(6,7,4, 1,1,1).setTexCoords(1,0,3));
		}
		if(sided>=ALL) {
			this.geo.g.addTri(new Tri(2,3,6, 3,3,3).setTexCoords(3,2,1));
			this.geo.g.addTri(new Tri(7,6,3, 3,3,3).setTexCoords(1,0,3));

			this.geo.g.addTri(new Tri(0,1,5, 2,2,2).setTexCoords(3,2,1));
			this.geo.g.addTri(new Tri(5,4,0, 2,2,2).setTexCoords(1,0,3));

			this.geo.g.addTri(new Tri(5,1,6, 5,5,5).setTexCoords(3,2,1));
			this.geo.g.addTri(new Tri(2,6,1, 5,5,5).setTexCoords(1,0,3));

			this.geo.g.addTri(new Tri(0,4,7, 4,4,4).setTexCoords(3,2,1));
			this.geo.g.addTri(new Tri(7,3,0, 4,4,4).setTexCoords(1,0,3));
		}

		this.geo.g.setColor(1.2f,1.2f,1.2f);
		this.geo.g.setMaterial(0.008f,16f,0,0);

		this.geo.g.lock();

		initTexture();
	}
}
