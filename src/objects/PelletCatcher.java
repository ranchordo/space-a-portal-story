package objects;

import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.Transform;

import graphics.GObject;
import graphics.Renderer;

public class PelletCatcher extends Thing {
	private static final long serialVersionUID = 1237435512380248141L;
	public static final float CATCHING_RADIUS=3.0f;
	private Quat4f quat;
	private Vector3f origin;
	private boolean caught=false;
	public PelletCatcher(Vector3f origin, Quat4f quat) {
		this.shape=new Vector3f();
		this.type="Pellet_Catcher";
		//this.geo.vmap.tex=w;
		this.origin=origin;
		this.quat=quat;
	}
	@Override
	public void initPhysics() {
		this.geo.mass=0;
		RigidBodyConstructionInfo body=this.geo.initPhysics_mesh(origin,quat);
		this.geo.doBody(body);
	}
	private Vector3f pelletPos=new Vector3f();
	@Override
	public void processActivation() {
		if(!caught) {
			for(int i=0;i<Renderer.things.size();i++) {
				Thing thing=Renderer.things.get(i);
				if(!thing.type.equals("Pellet")) {
					continue;
				}
				pelletPos.set(thing.geo.getTransform().origin);
				pelletPos.sub(geo.getTransform().origin);
				//System.out.println(geo.getTransform().getMatrix(new Matrix4f()));
				//System.out.println(pelletPos);
				if(pelletPos.length()<=CATCHING_RADIUS) {
					((Pellet)thing).die();
					caught=true;
					this.geo.setColor(0,1,1);
					this.geo.copyData(GObject.COLOR_DATA,GL_STATIC_DRAW);
				}
			}
		}
		sendingActivations=caught;
	}
	@Override
	public void render() {
		this.geo.highRender();
	}
	@Override
	public void initGeo() {
		this.geo=new GObject();
		this.geo.loadOBJ("laser_emitter/laser_emitter");
		this.geo.useTex=false;
		this.geo.setColor(0.3f,0.3f,0.31f);
		geo.lock();
	}

}
