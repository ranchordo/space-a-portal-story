package pooling;

import java.util.ArrayList;
import java.util.List;

import graphics.RenderUtils;
import graphics.Renderer;
import logger.Logger;

public abstract class AbstractObjectPool<T> { //Object pool with custom behavior.
	public long freeThshld=2000000l; //Amount of microseconds to keep unused elements
	public List<PoolElement<T>> pool; //The beeg list
	private String poolType="";
	private PoolInitCreator<T> prototype; //When we need to allocate more spaces, this object will control how new objects are created.
	//The name "prototype" is carried over from when I was trying to do something else
	
	public void setInitCreator(PoolInitCreator<T> p) { //Wow
		prototype=p;
	}
	
	public AbstractObjectPool(String type, PoolInitCreator<T> p) {
		pool=new ArrayList<PoolElement<T>>();
		poolType=type;
		PoolStrainer.activePools.add(this);
		prototype=p;
	}
	public void handleDeletion(PoolElement<T> i) { //Override this as a "destructor"
		
	}
	public int cleanOld() { //PoolStrainer keeps track of this. Just call its clean method occasionally.
		List<PoolElement<T>> old=new ArrayList<PoolElement<T>>();
		for(int i=0;i<pool.size();i++) {
			PoolElement<T> e=pool.get(i);
			//If the element's timeout is >0, that's how many microseconds it can remain in use before being freed
			if(e.isUsed()) { //Timing out the used ones
				if(e.tmout>=0) {
					if((RenderUtils.micros()-e.getLastToggle())>e.tmout) {
						old.add(e);
					}
				}
				continue;
			}
			if(freeThshld>0 && ((RenderUtils.micros()-e.getLastToggle())>freeThshld)) { //Timing out the not used ones
				old.add(e);
			}
		}
		for(int i=0;i<old.size();i++) {
			handleDeletion(old.get(i)); //Clean it up
			if(!pool.remove(old.get(i)) || (old.get(i).isUsed() && old.get(i).tmout>0)) {
				Logger.log(4,poolType+" pool: Old element somehow disappeared! What!?");
			}
		}
		return old.size();
	}
	public PoolElement<T> getFreeElement() { //Get a free element
		for(int i=0;i<pool.size();i++) {
			PoolElement<T> e=pool.get(i);
			if(!e.isUsed()) {
				if(e.o()==null) {
					Logger.log(2,"PoolElement from "+poolType+" pool is null on getFreeElement retrieval");
					e.setInternalObject(prototype.allocateInitValue()); //Fix this null thing
				}
				return e;
			}
		}
		//If we weren't able to get something from the pool
		Logger.log(0,"Expanding "+poolType+" pool to "+(pool.size()+1)+" elements.");
		PoolElement<T> ret=new PoolElement<T>(prototype.allocateInitValue());
		pool.add(ret);
		return ret;
	}
	public PoolElement<T> alloc() {
		return getFreeElement().setUsed(true);
	}
	public <T1> LinkedPoolElement<T1> alloc_linked() { //The linked pool element is real weird.
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
