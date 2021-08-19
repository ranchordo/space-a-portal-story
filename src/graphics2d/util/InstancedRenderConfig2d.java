package graphics2d.util;

import lepton.engine.rendering.Shader;
import lepton.engine.rendering.TextureImage;

public class InstancedRenderConfig2d {
	public Shader shader;
	public TextureImage image;
	public InstancedRenderConfig2d(Shader s, TextureImage i) {
		shader=s;
		image=i;
	}
	@Override
	public int hashCode() {
		return shader.hashCode()+(image==null?0:image.hashCode());
	}
	@Override
	public boolean equals(Object o) {
		if(o instanceof InstancedRenderConfig2d) {
			if(((InstancedRenderConfig2d)o).shader==this.shader) {
				if(((InstancedRenderConfig2d)o).image==this.image) {
					return true;
				}
			}
		}
		return false;
	}
}
