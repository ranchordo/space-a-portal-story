package pooling;

public class LinkedPoolElement<T> {
	PoolElement<?> e;
	public LinkedPoolElement(PoolElement<?> e) {
		this.e=e;
	}
	//@SuppressWarnings("unchecked")
	public T o() {
		return (T)e.o();
	}
	public void free() {e.setUsed(false);}
	public boolean isUsed() {return e.isUsed();}
	public long getLastToggle() {return e.getLastToggle();}
}
