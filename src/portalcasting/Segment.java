package portalcasting;

import javax.vecmath.Vector3f;

public class Segment {
	//PDR segment
	public Vector3f a=new Vector3f();
	public Vector3f b=new Vector3f();
	public Vector3f localz=new Vector3f();
	public Segment() {};
	public Segment(Segment i) {
		a.set(i.a);
		b.set(i.b);
		localz.set(i.localz);
	}
	public Segment set(Segment i) {
		a.set(i.a);
		b.set(i.b);
		localz.set(i.localz);
		return this;
	}
}
