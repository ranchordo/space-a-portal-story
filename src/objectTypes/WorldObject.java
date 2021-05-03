package objectTypes;

public class WorldObject {
	public PhysicsObject p;
	public GObject g;
	public WorldObject(PhysicsObject p_, GObject g_) {
		p=p_;
		g=g_;
	}
	public WorldObject(boolean ig, boolean ip) {
		if(ig) {ig();}
		if(ip) {ip();}
	}
	public WorldObject(boolean ia) {
		if(ia) {ia();}
	}
	public WorldObject() {}
	public void highRender() {
		g.highRender(p);
	}
	public static boolean gnull(WorldObject w) {
		if(w!=null) {
			return w.g==null;
		}
		return true;
	}
	public static boolean anull(WorldObject w) {
		if(w!=null) {
			return w.g==null || w.p==null;
		}
		return true;
	}
	public static boolean pnull(WorldObject w) {
		if(w!=null) {
			return w.p==null;
		}
		return true;
	}
	public void ip() {
		p=new PhysicsObject();
	}
	public void ig() {
		g=new GObject();
	}
	public void ia() {ip(); ig();}
}
