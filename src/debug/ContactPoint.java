package debug;

import javax.vecmath.Vector3f;

import lepton.optim.objpoollib.ObjectPool;
import lepton.optim.objpoollib.PoolInitCreator_clone;

public class ContactPoint implements Cloneable {
	public static ObjectPool<ContactPoint> contactPointPool=new ObjectPool<ContactPoint>("contactPoint",new PoolInitCreator_clone<ContactPoint>(new ContactPoint()));
	public Object clone() {
		return new ContactPoint(this);
	}
	public ContactPoint() {
		posA=new Vector3f();
		posB=new Vector3f();
	}
	public ContactPoint(ContactPoint i) {
		posA=new Vector3f(i.posA);
		posB=new Vector3f(i.posB);
		removed=i.removed;
	}
	public ContactPoint(Vector3f a, Vector3f b, boolean r) {
		posA=new Vector3f(a);
		posB=new Vector3f(b);
		removed=r;
	}
	public Vector3f posA;
	public Vector3f posB;
	public boolean removed=false;
	public ContactPoint set(Vector3f a, Vector3f b, boolean r) {
		if(posA==null) {posA=new Vector3f();}
		if(posB==null) {posB=new Vector3f();}
		posA.set(a);
		posB.set(b);
		removed=r;
		return this;
	}
}
