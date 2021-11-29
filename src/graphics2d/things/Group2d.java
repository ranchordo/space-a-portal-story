package graphics2d.things;

import java.util.ArrayList;

import javax.vecmath.Vector4f;

public class Group2d extends Thing2d {
	private ArrayList<Thing2d> things=new ArrayList<Thing2d>();
	@Override public void init() {
		for(Thing2d thing : things) {
			thing.init();
		}
	}
	@Override public void render() {
		for(Thing2d thing : things) {
			thing.render();
		}
	}
	@Override public void logic() {
		for(Thing2d thing : things) {
			thing.logic();
		}
	}
	private Vector4f bb=new Vector4f(0,0,0,0);
	public Thing2d add(Thing2d i) {
		things.add(i);
		return i;
	}
	public ArrayList<Thing2d> getList() {
		return things;
	}
	@Override public Thing2d setParent(Thing2d parent) {
		for(Thing2d thing : things) {
			thing.setParent(parent);
		}
		this.parent=parent;
		return this;
	}
	@Override
	public Vector4f getBoundingBox() {
		bb.set(0,0,0,0);
		for(Thing2d i : things) {
			Vector4f ibb=i.getBoundingBox();
			if(bb.x==0&&bb.y==0&&bb.z==0&&bb.w==0) {
				bb.set(ibb);
			} else {
				if(ibb.x<bb.x) {
					bb.x=ibb.x;
				}
				if(ibb.y<bb.y) {
					bb.y=ibb.y;
				}
				if(ibb.z>bb.z) {
					bb.z=ibb.z;
				}
				if(ibb.w>bb.w) {
					bb.w=ibb.w;
				}
			}
		}
		return bb;
	}
}
