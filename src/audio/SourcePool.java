package audio;

import java.util.HashMap;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import pooling.AbstractObjectPool;
import pooling.PoolElement;
import pooling.PoolInitCreator;

public class SourcePool extends AbstractObjectPool<Source> {
	private HashMap<String, PoolElement<Source>> playing=new HashMap<String, PoolElement<Source>>();
	private Vector3f unifiedPos=new Vector3f();
	private Vector3f unifiedVel=new Vector3f();
	public SourcePool(String type) {
		super(type+".source", null);
		PoolInitCreator<Source> p=new PoolInitCreator<Source>() {
			@Override
			public Source allocateInitValue() {
				Source a=new Source();
				a.setPosVel(SourcePool.this.unifiedPos,SourcePool.this.unifiedVel);
				return a;
			}
		};
		this.setInitCreator(p);
		this.freeThshld=30000000l;
	}
	public PoolElement<Source> getPlaying(String key) {
		return playing.get(key);
	}
	public void logic(Point3f pos) {
		freeDone();
		unifiedPos.set(pos);
		unifiedVel.set(0,0,0);
		for(int i=0;i<pool.size();i++) {
			pool.get(i).o().updatePosVel();
		}
	}
	private void freeDone() {
		for(int i=0;i<pool.size();i++) {
			PoolElement<Source> a=pool.get(i);
			if(!a.o().isPlaying()) {
				playing.remove((String)a.mdo);
				a.free();
			}
		}
	}
	public void play(Sound sound, String key) {
		PoolElement<Source> a=this.alloc();
		a.mdo=key;
		playing.put(key,a);
		a.o().play(sound);
	}
	public void play(String key, Soundtrack soundtrack) {
		this.play(soundtrack.get(key),key);
	}
	@Override
	public void handleDeletion(PoolElement<Source> i) {
		i.o().close();
	}
}
