package pooling;

import java.util.HashSet;

import graphics.RenderUtils;
import logger.Logger;

public class PoolStrainer {
	public static long CLEAN_PERIOD=10000000;
	public static HashSet<AbstractObjectPool<?>> activePools=new HashSet<AbstractObjectPool<?>>();
	private static long lastClean=0;
	private static int cleanAll_noCheck() {
		int r=0;
		for(AbstractObjectPool<?> p : activePools) {
			r+=p.cleanOld();
		}
		return r;
	}
	public static void clean() {
		if(CLEAN_PERIOD<=0) {
			return;
		}
		if((RenderUtils.micros()-lastClean)>CLEAN_PERIOD) {
			int r=cleanAll_noCheck();
			if(r!=0) {Logger.log(0,"Pool cleaning: Cleaned "+r+" old elements.");}
			lastClean=RenderUtils.micros();
		}
	}
}
