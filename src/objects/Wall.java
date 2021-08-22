package objects;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
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
import lepton.util.LeptonUtil;
import lepton.util.advancedLogger.Logger;

public class Wall extends Thing {
	public static InstancedThingParent itp;
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
	 */
	public static final int objectSize=16+4+4;
	public Wall(Vector2f shape, Vector3f origin, Quat4f quat) {
		this.type="Wall";
		this.shape=new Vector3f(shape.x,shape.y,0.3f); //shape.z - wall "thickness"
		this.origin=origin;
		this.quat=quat;
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
		this.geo.p.mass=0;
		CollisionShape s=new BoxShape(getShape());
		RigidBodyConstructionInfo body=this.geo.p.initPhysics_shape(s, origin, quat);
		body.restitution=1;
		body.friction=1f;
		this.geo.p.doBody(body);
		this.portalable=true;
	}
	private float[] data=new float[objectSize];
	private transient Transform tr=null;
	private transient Matrix4f m=null;
	@Override public void render() {
		if(!instancedThingParent.geo.hasAlpha) {
			if(tr==null) {tr=new Transform();}
			if(m==null) {m=new Matrix4f();}
			LeptonUtil.openGLMatrix(this.geo.p.body.getMotionState().getWorldTransform(tr).getMatrix(m),data);
			data[16]=shape.x;
			data[17]=shape.y;
			data[18]=shape.z;
			data[19]=0;
			data[20]=texOffset.x;
			data[21]=texOffset.y;
			data[22]=shape.x/aspect.x;
			data[23]=shape.y/aspect.y;
			instancedRenderConfig.instanceAccumulator.add(data);
		}
	}
	@Override public void alphaRender() {
		if(instancedThingParent.geo.hasAlpha) {
			Logger.log(4,"Instanced Thing with type "+type+" cannot have a non-1 alpha component on any piece of its geometry.");
		}
	}
	public void initGeo() {
		this.geo=new WorldObject();
		this.geo.p=new PhysicsObject();
		if(instancedThingParent==null) {
			if(itp==null) {
				instancedThingParent=new InstancedThingParent();
				instancedThingParent.geo=new GObject();
				instancedThingParent.geo.useTex=true;
				instancedThingParent.geo.vmap.vertices=new ArrayList<Vector3f>();
				instancedThingParent.geo.vmap.vertices.add(new Vector3f(-1,-1,-1));
				instancedThingParent.geo.vmap.vertices.add(new Vector3f(+1,-1,-1));
				instancedThingParent.geo.vmap.vertices.add(new Vector3f(+1,+1,-1));
				instancedThingParent.geo.vmap.vertices.add(new Vector3f(-1,+1,-1));
				if(sided>=DOUBLE) {
					instancedThingParent.geo.vmap.vertices.add(new Vector3f(-1,-1,+1));
					instancedThingParent.geo.vmap.vertices.add(new Vector3f(+1,-1,+1));
					instancedThingParent.geo.vmap.vertices.add(new Vector3f(+1,+1,+1));
					instancedThingParent.geo.vmap.vertices.add(new Vector3f(-1,+1,+1));
				}
				instancedThingParent.geo.vmap.normals=new ArrayList<Vector3f>();
				instancedThingParent.geo.vmap.normals.add(new Vector3f(0,0,-1));
				instancedThingParent.geo.vmap.normals.add(new Vector3f(0,0, 1));
				instancedThingParent.geo.vmap.normals.add(new Vector3f(0, 1,0));
				instancedThingParent.geo.vmap.normals.add(new Vector3f(0,-1,0));
				instancedThingParent.geo.vmap.normals.add(new Vector3f( 1,0,0));
				instancedThingParent.geo.vmap.normals.add(new Vector3f(-1,0,0));
				instancedThingParent.geo.vmap.texcoords=new ArrayList<Vector2f>();
				instancedThingParent.geo.vmap.texcoords.add(new Vector2f(0,1));
				instancedThingParent.geo.vmap.texcoords.add(new Vector2f(1,1));
				instancedThingParent.geo.vmap.texcoords.add(new Vector2f(1,0));
				instancedThingParent.geo.vmap.texcoords.add(new Vector2f(0,0));
				instancedThingParent.geo.clearTris();
				instancedThingParent.geo.addTri(new Tri(2,1,0, 0,0,0).setTexCoords(1,2,3));
				instancedThingParent.geo.addTri(new Tri(0,3,2, 0,0,0).setTexCoords(3,0,1));
				if(sided>=DOUBLE) {
					instancedThingParent.geo.addTri(new Tri(4,5,6, 1,1,1).setTexCoords(3,2,1));
					instancedThingParent.geo.addTri(new Tri(6,7,4, 1,1,1).setTexCoords(1,0,3));
				}
				if(sided>=ALL) {
					instancedThingParent.geo.addTri(new Tri(2,3,6, 3,3,3).setTexCoords(3,2,1));
					instancedThingParent.geo.addTri(new Tri(7,6,3, 3,3,3).setTexCoords(1,0,3));

					instancedThingParent.geo.addTri(new Tri(0,1,5, 2,2,2).setTexCoords(3,2,1));
					instancedThingParent.geo.addTri(new Tri(5,4,0, 2,2,2).setTexCoords(1,0,3));

					instancedThingParent.geo.addTri(new Tri(5,1,6, 5,5,5).setTexCoords(3,2,1));
					instancedThingParent.geo.addTri(new Tri(2,6,1, 5,5,5).setTexCoords(1,0,3));

					instancedThingParent.geo.addTri(new Tri(0,4,7, 4,4,4).setTexCoords(3,2,1));
					instancedThingParent.geo.addTri(new Tri(7,3,0, 4,4,4).setTexCoords(1,0,3));
				}

				instancedThingParent.geo.setColor(1.2f,1.2f,1.2f);
				this.portalable=true;
				if(textureType==1) {
					instancedThingParent.geo.setColor(0.2f,0.2f,0.25f);
					this.portalable=false;
				}
				instancedThingParent.geo.setMaterial(0.008f,16f,0,0);

				instancedThingParent.geo.lock();

				try {
					instancedThingParent.geo.loadTexture("assets/"+textures[textureType],"jpg");
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				if(textureType==2) {
					try {
						instancedThingParent.geo.vmap.tex=new Texture(instancedThingParent.geo.vmap.tex);
						instancedThingParent.geo.vmap.tex.reset(Texture.NORMAL);
						instancedThingParent.geo.vmap.tex.create(1,"assets/3d/wall/cropped_border_normal_detail",".jpg");
					} catch (FileNotFoundException e) {
						Logger.log(4,e.toString(),e);
					}
				}
				instancedRenderConfig=Main.instancedRenderer.loadConfiguration(Main.shaderLoader.load("specificinstanced/InstancedDebug"),null,instancedThingParent.geo,objectSize,"info_buffer");
			}
		}
	}
}
