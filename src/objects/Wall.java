package objects;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;

import graphics.Texture;
import graphics.Tri;
import logger.Logger;
import objectTypes.GObject;
import objectTypes.WorldObject;

public class Wall extends Thing {
	private static final long serialVersionUID = -4731923936294407139L;
	public static final int SINGLE=0;
	public static final int DOUBLE=1;
	public static final int ALL=2;
	public static final String[] textures=new String[] {"3d/wall/cropped_border.jpg","3d/wall/cropped_border.jpg","3d/wall/cropped_border.jpg"};
	private Vector3f origin;
	private Quat4f quat;
	public Vector2f aspect=new Vector2f(1,1.6f);
	public Vector2f texOffset=new Vector2f(0,0);
	public int textureType=0;
	public int sided=SINGLE;
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
	public void initGeo() {
		this.geo=new WorldObject(true);
		//this.geo.vmap.tex=walltex;
		this.geo.g.useTex=true;
		this.geo.g.useBump=false;
		this.geo.g.vmap.vertices=new ArrayList<Vector3f>();
		this.geo.g.vmap.vertices.add(new Vector3f(-getShape().x,-getShape().y,-getShape().z));
		this.geo.g.vmap.vertices.add(new Vector3f(+getShape().x,-getShape().y,-getShape().z));
		this.geo.g.vmap.vertices.add(new Vector3f(+getShape().x,+getShape().y,-getShape().z));
		this.geo.g.vmap.vertices.add(new Vector3f(-getShape().x,+getShape().y,-getShape().z));
		if(sided>=DOUBLE) {
			this.geo.g.vmap.vertices.add(new Vector3f(-getShape().x,-getShape().y,+getShape().z));
			this.geo.g.vmap.vertices.add(new Vector3f(+getShape().x,-getShape().y,+getShape().z));
			this.geo.g.vmap.vertices.add(new Vector3f(+getShape().x,+getShape().y,+getShape().z));
			this.geo.g.vmap.vertices.add(new Vector3f(-getShape().x,+getShape().y,+getShape().z));
		}
		this.geo.g.vmap.normals=new ArrayList<Vector3f>();
		this.geo.g.vmap.normals.add(new Vector3f(0,0,-1));
		this.geo.g.vmap.normals.add(new Vector3f(0,0, 1));
		this.geo.g.vmap.normals.add(new Vector3f(0, 1,0));
		this.geo.g.vmap.normals.add(new Vector3f(0,-1,0));
		this.geo.g.vmap.normals.add(new Vector3f( 1,0,0));
		this.geo.g.vmap.normals.add(new Vector3f(-1,0,0));
		this.geo.g.vmap.texcoords=new ArrayList<Vector2f>();
		this.geo.g.vmap.texcoords.add(new Vector2f(texOffset.x                 ,texOffset.y+shape.y/aspect.y));
		this.geo.g.vmap.texcoords.add(new Vector2f(texOffset.x+shape.x/aspect.x,texOffset.y+shape.y/aspect.y));
		this.geo.g.vmap.texcoords.add(new Vector2f(texOffset.x+shape.x/aspect.x,texOffset.y));
		this.geo.g.vmap.texcoords.add(new Vector2f(texOffset.x                 ,texOffset.y));
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
		this.portalable=true;
		if(textureType==1) {
			this.geo.g.setColor(0.2f,0.2f,0.25f);
			this.portalable=false;
		}
		this.geo.g.setMaterial(0.008f,16f,0,0);
		
		geo.g.lock();

		this.geo.g.vmap.tex.colorLoaded=true;
		try {
			this.geo.g.loadTexture(textures[textureType]);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		if(textureType==2) {
			try {
				this.geo.g.vmap.tex=new Texture(this.geo.g.vmap.tex);
				this.geo.g.vmap.tex.reset(Texture.BUMP);
				this.geo.g.vmap.tex.reset(Texture.NORMAL);
				this.geo.g.vmap.tex.create_norm("3d/wall/cropped_border_normal_detail.jpg");
			} catch (FileNotFoundException e) {
				Logger.log(4,e.toString(),e);
			}
		}
		if(this.geo.g.vmap.tex.normLoaded) {
			this.geo.g.useBump=true;
		}
	}
}
