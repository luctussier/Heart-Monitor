package org.jj.heart.data;

/**
 * A class for the heart beat pulse/signal/reading/measurement/data point 
 * @author jjones
 */
public class Beat {
	/** time since the Arduino booted */
	protected long time;
	/** beat interval from previous beat */
	protected int periode;
	/** if this beat has been validated */
	protected boolean valid;

	public Beat(long time) {this.time = time;}

	public long getTime() {
		return time;
	}

	public int getPeriode() {
		return periode;
	}

	public boolean isValid() {
		return valid;
	}
	
}
