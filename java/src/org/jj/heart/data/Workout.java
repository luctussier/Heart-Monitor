package org.jj.heart.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class for storing and manipulating work-out session/lap/segment
 * @author jjones
 */
public class Workout {
	/** how many samples are needed to establish a valid heart rate */
	static final int sampleSize = 7;
	// may be too small with real data (polar takes about 8 seconds)

	/** only recognized starting time format */
	final static SimpleDateFormat hourFormat = new SimpleDateFormat("H:m:s.SSS");
	/** text read from log */
	protected String timeStamp;
	/** Beginning of work-out */
	protected Date begin;
	/** collection of heart beats */
	protected ArrayList<Beat> beats;
	/** estimated total of beats for calculating the average */
	protected long totalBeats;
	/** position of first valid heart beat */
	protected int firstValid;
	/** position of last valid heart beat */
	protected int lastValid;
	protected int max,min;

	/**
	 * Initializes everything to zero/default values
	 */
	public Workout() {
		timeStamp = "0:0:0";
		begin = new Date(0);
		beats = new ArrayList<Beat>();
		totalBeats = 0;
		max = min = lastValid = firstValid = -1;
	}

	/**
	 * Creates new work-out object from file data read.
	 */
	public Workout(String contents) {
		this();
		BufferedReader readBuffer = new BufferedReader(new StringReader(
				contents));
		String line;
		try {
			// first line in string should be the timestamp; this may need to be verified
			timeStamp = readBuffer.readLine();
			if(!timeStamp.equals("0:0:0")){
				try {
					begin = hourFormat.parse(timeStamp);
				} catch (ParseException e) {
				}
			}
			while (null != (line = readBuffer.readLine())) {
				beats.add(new Beat(Long.parseLong(line)));
			}
		} catch (IOException io) {
		} catch (NumberFormatException num) {
		}

		try {
			readBuffer.close();
		} catch (IOException e) {
		}
	}

	// getters and setters
	public String getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}

	public Date getBegin() {
		return begin;
	}

	public void setBegin(Date begin) {
		this.begin = begin;
	}

	public ArrayList<Beat> getBeats() {
		return beats;
	}

	public void setBeats(ArrayList<Beat> beats) {
		this.beats = beats;
	}

	public long getTotalBeats() {
		return totalBeats;
	}

	public int getFirstValid() {
		return firstValid;
	}

	public int getLastValid() {
		return lastValid;
	}

	public int getMax() {
		return max;
	}

	public int getMin() {
		return min;
	}

	/**
	 * Validates all points and average are human and the points don't vary more than 20% from average.  
	 * @param beats
	 * @return
	 */
	static public boolean validateSample(List<Beat> beats) {
		long sum = 0;
		for (Beat beat : beats) {
			sum += beat.periode;
		}
		int average = (int) sum / beats.size();
		if (!isHumanRate(average)) { return false; }

		for (Beat beat : beats) {
			if (!isHumanRate(beat.periode)) { return false; }
			if (average * 0.8 > beat.periode || beat.periode > average * 1.2) { return false; }
		}

		return true;
	}

	/**
	 * Determine if period is with normal human rates<br/> 
	 * range = (40 < bpm < 220) or (~275ms < period < 1500ms)
	 * 6e4/period(ms) = bpm; 6e4/bpm = period in ms
	 * @param period  the period to verify
	 * @return
	 */
	static public boolean isHumanRate(int period) {
		return (275 < period && period < 1500);
	}

	/**
	 * may need to return some information (firstPos,lastValid,success/fail...)
	 */
	public boolean validate(){
		// calculate each period
		if(beats.size()>0){
			beats.get(0).periode = (int) beats.get(0).time;
		}
		for (int i = 1; i < beats.size(); i++) {
			beats.get(i).periode = (int) (beats.get(i).time - beats.get(i - 1).time);
		}

		// verify each period is human and comparable to the average of neighbors 
		for (int i = 1; i < beats.size(); i++) {
			if (i+1 >= sampleSize) { // validate current point with those that precede 
				beats.get(i).valid = validateSample(beats.subList(i+1-sampleSize, i+1));
			} // validate current point with those that follow 
			if (!beats.get(i).valid && i <= beats.size()-sampleSize) {
				beats.get(i).valid = validateSample(beats.subList(i, i+sampleSize));
			}
//			if(!beats.get(i).valid){
//				System.out.println("bad point "+beats.get(i).time);
//			}
		}
		
		// first pass find first and last valid elements
		for (int i = 0; i < beats.size() && !beats.get(i).valid; i++){
			firstValid = i;
		}
		lastValid = firstValid;
		
		// find & calculate missing beats
		totalBeats = 0; // one should round the calculated/missed beats
		for (int i = Math.max(firstValid, 1); i < beats.size(); i++) {
			if (beats.get(i).valid) {
				totalBeats++;
				lastValid = i;
			}else{
				int j = i;
				while (++j < beats.size() && !beats.get(j).valid);
				if (j < beats.size()) {
					// add the missed beats to a running total
					totalBeats += Math.round(2 * (beats.get(j).time - beats.get(i-1).time) / (beats.get(i-1).periode + beats.get(j).periode));
					lastValid = i = j;
				}
			}
		}
		
		if(hasValidBeat()){
			min = max = beats.get(firstValid).periode;
			for (int i = firstValid; i <= lastValid; i++) {
				if (beats.get(i).valid) {
					max = Math.max(max, beats.get(i).periode);
					min = Math.min(min, beats.get(i).periode);
				}
			}
		}
		return hasValidBeat();
	}
	
	public boolean hasValidBeat(){
		return Math.min(firstValid, lastValid)>=0;
	}
	
	public float getAverage(){
		if(hasValidBeat()){
			return (beats.get(lastValid).time - beats.get(firstValid).time) / totalBeats;
		}
		return 0;
	}
	
	public List<Beat> getValidBeats(){
		if(hasValidBeat()){
			return beats.subList(firstValid, lastValid + 1);
		}
		return new ArrayList<Beat>();
	}

	/**
	 * Merges two consecutive work-outs
	 * @param workout
	 * @return
	 */
	public Workout merge(Workout workout) {
		Workout earliest=this, latest=workout;
		// make sure the earliest workout session happened first
		if(latest.begin.before(earliest.begin)){
			earliest = workout;
			latest = this;
		}

		if(earliest.beats.isEmpty()){
			return latest;
		}
		
		long timeGap = latest.begin.getTime() - earliest.begin.getTime();
		// when the times are the same like when the clock is not set "0:0:0"
		if(timeGap==0 && earliest.beats.size()>0){
			timeGap = earliest.beats.get(earliest.beats.size()-1).time;
		}
		for(Beat beat:latest.beats){
			beat.time += timeGap;
			earliest.beats.add(beat);
		}

		return earliest;
	}

	/**
	 * Calculates duration in milliseconds
	 * @return duration of valid work-out or 0 otherwise
	 */
	public long getDuration(){
		if(hasValidBeat()){
			return beats.get(lastValid).time - beats.get(firstValid).time;
		}
		return 0;
	}
}