package pooling;

public class LinkedPoolElement<T> {
	//A linkedPoolElement is like a pool element, but it will cast to something else on output. This is if you want to use the pool of something more generic and cast it to something specific as you get stuff.
	PoolElement<?> e;
	public LinkedPoolElement(PoolElement<?> e) {
		this.e=e;
	}
	@SuppressWarnings("unchecked")
	public T o() {
		return (T)e.o();
	}
	public void free() {e.setUsed(false);}
	public boolean isUsed() {return e.isUsed();}
	public long getLastToggle() {return e.getLastToggle();}
}
