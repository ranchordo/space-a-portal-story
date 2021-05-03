package portalcasting;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.linearmath.Transform;

import graphics.GraphicsInit;
import graphics.Renderer;
import graphics.Tri;
import objects.Player;
import objects.PortalPair;
import objects.Thing;

public abstract class PortalCaster {
	public static final int MAX_SEGMENTS=4;
	public boolean stopOnObject=true;
	public ArrayList<Segment> segments=new ArrayList<Segment>();
	private SingleCastResult singleCastResult=new SingleCastResult();
	//private Transform bodyTransform=new Transform();
	public List<String> DOES_NOT_STOP=new ArrayList<String>();
	public Vector3f initialLocalZ=new Vector3f(0,0,1);
	public List<String> ACTIVATES=new ArrayList<String>();
	private Thing signaling=null;
	private void singleCast(Vector3f start, Vector3f control, PortalPair pp) {
		control.sub(start);
		control.normalize();
		control.add(start);
		singleCastResult.seg.a.set(start);
		singleCastResult.portalhit=0;
		float f=-1;
		if(stopOnObject) {
			for(int i=0;i<Renderer.things.size();i++) {
				Thing thing=Renderer.things.get(i);
				if(DOES_NOT_STOP.contains(thing.type)) {continue;}
				if(thing.geo==null) {continue;}
				if(thing.geo.p.body==null) {continue;}
				float fl=thing.geo.g.rayTest_distance(start,control,thing.geo.p.getTransform());
				if(fl>0.0f && (fl<f || f<0.0f)) {
					f=fl;
					singleCastResult.hit=thing;
				}
			}
		}
		float fl=pp.geo.g.rayTest_distance(start,control,pp.p1);
		if(fl>0.0f && (fl<f || f<0.0f)) {
			f=fl;
			singleCastResult.hit=pp;
			singleCastResult.portalhit=1;
		}
		fl=pp.geo2.g.rayTest_distance(start,control,pp.p2);
		if(fl>0.0f && (fl<f || f<0.0f)) {
			f=fl;
			singleCastResult.hit=pp;
			singleCastResult.portalhit=2;
		}
		if(f<0) {
			f=Tri.CLIP_DISTANCE;
			singleCastResult.hit=null;
		}
		control.sub(start);
		control.normalize();
		control.scale(f);
		control.add(start);
		singleCastResult.seg.b.set(control);
		return;
	}
	public boolean handleSpecial(SingleCastResult r, Vector3f start, Vector3f control) {
		return false;
	}
	private Vector3f start=new Vector3f();
	private Vector3f control=new Vector3f();
	private Matrix3f rs=new Matrix3f();
	private Matrix4f trmat=new Matrix4f();
	private Vector3f currlocalz=new Vector3f();
	public synchronized void cast(Vector3f starto, Vector3f controlo) {
		start.set(starto);
		control.set(controlo);
		control.sub(start);
		control.normalize();
		control.add(start);
		PortalPair pp=((Player)GraphicsInit.player).portalPair;
		segments.clear();
		currlocalz.set(initialLocalZ);
		boolean alive=true;
		while(alive) {
			if(segments.size()>=MAX_SEGMENTS) {
				alive=false;
				continue;
			}
			signaling=null;
			singleCast((Vector3f)start.clone(),(Vector3f)control.clone(),pp);
			singleCastResult.seg.localz.set(currlocalz);
			segments.add(new Segment(singleCastResult.seg));
			if(singleCastResult.portalhit==0) {
				if(singleCastResult.hit!=null && ACTIVATES.contains(singleCastResult.hit.type)) {
					singleCastResult.hit.pcasterHits++;
					if(singleCastResult.hit.pcasterSendOnHit) {
						singleCastResult.hit.sendingActivations=true;
					}
				}
				 
				alive=handleSpecial(singleCastResult,start,control);
			} else if(pp.placed1 && pp.placed2) {
				Transform tr=singleCastResult.portalhit==1?pp.difference_inv():pp.difference();
				control.sub(start);
				start.set(segments.get(segments.size()-1).b);
				tr.transform(start);
//				start.add(tr.origin);
				tr.getMatrix(trmat).getRotationScale(rs);
				rs.transform(control);
				rs.transform(currlocalz);
				//System.out.println(control);
				//System.out.println(start+", "+control);
				control.add(start);
			} else {
				alive=false;
			}
		}
	}
}
