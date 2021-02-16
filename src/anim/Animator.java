package anim;

import java.util.HashMap;
import java.util.Map.Entry;

import com.bulletphysics.linearmath.Transform;

public class Animator {
	private HashMap<String, AnimTrack> tracks=new HashMap<String,AnimTrack>();
	private AnimTrack track;
	public Transform synchronizedTransform;
	public Animator() {}
	public void copyTransformPointer() {
		for(Entry<String,AnimTrack> e : tracks.entrySet()) {
			e.getValue().addTransform=synchronizedTransform;
		}
	}
	public void add(String key, AnimTrack t) {
		tracks.put(key,t);
	}
	public void remove(String key) {
		tracks.remove(key);
	}
	public Transform getFrame() {
		if(track==null) {
			throw new IllegalStateException("Active track is null");
		}
		return track.getFrame();
	}
	public AnimTrack getTrack() {
		return track;
	}
	public void clear() {track=null; tracks.clear();}
	public void setActiveAnim(String key) {
		track=tracks.get(key);
		if(track==null) {
			throw new IllegalStateException("Track "+key+" is not contained.");
		}
	}
	public void resetAllTracks() {
		for(Entry<String,AnimTrack> e : tracks.entrySet()) {
			e.getValue().resetFrameCounter();
		}
	}
	public void resetTrack() {
		track.resetFrameCounter();
	}
	public void advanceAllTracks() {
		for(Entry<String,AnimTrack> e : tracks.entrySet()) {
			e.getValue().advance_tc(1);
		}
	}
	public void advanceTrack() {
		track.advance_tc(1);
	}
}
