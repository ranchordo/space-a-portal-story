package objects;

public class ConnectionFilter extends Thing {
	private static final long serialVersionUID = -7979784390699090641L;
	public ConnectionFilter() {
		this.type="Connection_filter";
	}
	@Override
	public void initPhysics() {}
	@Override
	public void initVBO() {}
	@Override
	public void initGeo() {}
	@Override
	public void refresh() {
	}
	@Override
	public void render() {
		//Do nothing
	}
	@Override
	public void alphaRender() {
		
	}
	@Override
	public void interact() {
		this.interacted=false;
	}
	@Override
	public void logic() {
		if(activations>=activationThreshold) {
			sendingActivations=true;
		} else {
			sendingActivations=false;
		}
		portalCounter++;
	}
	@Override
	public void addPhysics() {
	}
	@Override
	public void addPhysics(short group, short mask) {
	}
}
