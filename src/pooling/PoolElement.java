package pooling;

import graphics.RenderUtils;

public class PoolElement<T> {
	public long tmout=-1; //Timeout while alive
	private boolean used=false;
	private long lastToggle; //microseconds
	private T o; //o? Why o? Idk. That's the internal object, though
	public Object mdo=null; //Just some misc data
	public void free() {setUsed(false);}
	public PoolElement(T no) { //no -> new object
		o=no;
		lastToggle=RenderUtils.micros();
	}
	public PoolElement<T> setInternalObject(T no) {
		o=no;
		return this;
	}
	protected PoolElement<T> setUsed(boolean nused) {
		used=nused;
		lastToggle=RenderUtils.micros();
		return this;
	}
	public boolean isUsed() {return used;}
	public long getLastToggle() {return lastToggle;}
	public T o() {return o;}
}
