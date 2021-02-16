package pooling;

import java.util.ArrayList;
import java.util.List;

import graphics.RenderUtils;
import graphics.Renderer;
import logger.Logger;

public class AbstractObjectPool<T> {
	public long freeThshld=2000000l;
	public List<PoolElement<T>> pool;
	private String poolType="";
	private PoolInitCreator<T> prototype;
	
	public void setInitCreator(PoolInitCreator<T> p) {
		prototype=p;
	}
	
	public AbstractObjectPool(String type, PoolInitCreator<T> p) {
		pool=new ArrayList<PoolElement<T>>();
		poolType=type;
		PoolStrainer.activePools.add(this);
		prototype=p;
	}
	public void handleDeletion(PoolElement<T> i) {
		
	}
	public int cleanOld() {
		List<PoolElement<T>> old=new ArrayList<PoolElement<T>>();
		for(int i=0;i<pool.size();i++) {
			PoolElement<T> e=pool.get(i);
			if(e.isUsed()) {
				if(e.tmout>=0) {
					if((RenderUtils.micros()-e.getLastToggle())>e.tmout) {
						old.add(e);
					}
				}
				continue;
			}
			if(freeThshld>0 && ((RenderUtils.micros()-e.getLastToggle())>freeThshld)) {
				old.add(e);
			}
		}
		for(int i=0;i<old.size();i++) {
			handleDeletion(old.get(i));
			if(!pool.remove(old.get(i))) {
				Logger.log(3,poolType+" pool: Old element somehow disappeared! What!?");
			}
		}
		return old.size();
	}
	public PoolElement<T> getFreeElement() {
		for(int i=0;i<pool.size();i++) {
			PoolElement<T> e=pool.get(i);
			if(!e.isUsed()) {
				if(e.o()==null) {
					Logger.log(1,"PoolElement from "+poolType+" pool is null on getFreeElement retrieval");
					e.setInternalObject(prototype.allocateInitValue());
				}
				return e;
			}
		}
		Logger.log(0,"Expanding "+poolType+" pool to "+(pool.size()+1)+" elements.");
		PoolElement<T> ret=new PoolElement<T>(prototype.allocateInitValue());
		pool.add(ret);
		return ret;
	}
	public PoolElement<T> alloc() {
		return getFreeElement().setUsed(true);
	}
	public <T1> LinkedPoolElement<T1> alloc_linked() {
		return Pools.alloc_linked(this);
	}
	public String getPoolType() {return poolType;}
	public void die() {
		PoolStrainer.activePools.remove(this);
	}
	public void free() {
		//Drop all elements and garbage collect
		for(PoolElement<T> pe : pool) {
			handleDeletion(pe);
		}
		pool.clear();
	}
}
