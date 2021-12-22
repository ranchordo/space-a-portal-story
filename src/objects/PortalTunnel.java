package objects;
import javax.vecmath.Vector3f;

import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.Transform;

import game.PlayerInitializer;
import lepton.engine.physics.WorldObject;

public class PortalTunnel extends Thing {
	private static final long serialVersionUID = -3205970798412713592L;
	public PortalTunnel() {
		this.type="Wall";
		this.shape=new Vector3f(PortalPair.PORTAL_WIDTH,PortalPair.PORTAL_HEIGHT,0.16625f);
	}
	@Override
	public void initPhysics() {
		this.geo.p.mass=0;
		RigidBodyConstructionInfo body=this.geo.p.initPhysics_mesh(this.geo.g,new Transform(PlayerInitializer.player.portalPair.p1()));
		body.restitution=1;
		body.friction=1f;
		this.geo.p.doBody(body);
		this.portalable=true;
		this.exemptFromChamberFeed=true;
	}
	@Override public void render() {
		
	}
	@Override public void alphaRender() {
		
	}
	@Override
	public void initGeo() {
		this.geo=new WorldObject(true);
		this.geo.g.useTex=false;
		this.geo.g.useLighting=false;
		
		this.geo.g.loadOBJ("assets/3d/portal/portal_tunnel");
		this.geo.g.scale(PortalPair.PORTAL_WIDTH,PortalPair.PORTAL_HEIGHT,1);
		
		geo.g.lock();
	}
}
