package pooling;

import graphics.RenderUtils;

public class PoolElement<T> {
	public long tmout=-1;
	private boolean used=false;
	private long lastToggle;
	private T o;
	public Object mdo=null; //MiscDataObject
	public void free() {setUsed(false);}
	public PoolElement(T no) {
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
