package org.jj.heart;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.jj.heart.data.Beat;
import org.jj.heart.data.Workout;

/**
 * Helper/service class for work-outs
 * @author jjones
 */
public class DataAnalizer {

	/**
	 * Parses log contents to create work-outs
	 * log:
	 *    header: "---23:59:59"
	 *    the four dashes signal a new entry (device rebooted)
	 *    time is formatted H:m:s
	 *    log: "123456789"
	 *    the time since reboot that beat was received
	 * @param log  the contents of the log file
	 * @return a list of work-outs found in string
	 */
	public static List<Workout> parseLog(String log) {
		List<Workout> workouts = new ArrayList<Workout>();
		// this delimiter is always added to file on arduino boot
		for (String workout : log.split("----")) {
			if (workout.length() == 0) {
				continue;
			}
			workouts.add(new Workout(workout));
		}

		List<Workout> merged = new ArrayList<Workout>();
		Workout prev = null;
		for (Workout workout : workouts) {
			// if first element or more than 10min have elapse since last workout 
			if (prev == null || (!workout.getBegin().equals(prev.getBegin())
					&& (workout.getBegin().getTime() - prev.getBegin().getTime() - prev.getBeats()
							.get(prev.getBeats().size() - 1).getTime()) > 600000)) {
				merged.add(workout);
				prev = workout;
			} else {
				// merge power interruption where gap < 10min  
				prev.merge(workout);
			}
		}
		
		workouts = merged;
		for (Workout workout : workouts) {
			workout.validate();
		}
		
		return workouts;
	}

	/**
	 * Creates a graphic representation of work-out data 
	 * @param workout
	 */
	public static BufferedImage visualize(Workout workout) {
		int displayX = 800, displayY = 600; // originally 800 by 600  
//		int displayX = 1440, displayY = 1080;
		ArrayList<Beat> beats = workout.getBeats();
		int rate, time;
		BufferedImage image = new BufferedImage(displayX, displayY, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setBackground(Color.black);
		graphics.clearRect(0, 0, displayX, displayY);
		
		// draw a pink line for where the work-out average would be
		graphics.setColor(Color.pink);
		graphics.drawLine(0, (int)(relativeRate(workout, Math.round(workout.getAverage()))*displayY), 
				displayX, (int)(relativeRate(workout, Math.round(workout.getAverage()))*displayY));
		
		long startTime = beats.get(workout.getFirstValid()).getTime();
		float timeScale = 1f / (beats.get(workout.getLastValid()).getTime() - startTime);
		Point prev, curr;
		prev = new Point(0, (int) (relativeRate(workout, beats.get(workout.getFirstValid()).getPeriode())) * displayY);
		graphics.setColor(Color.green);
		for (Beat beat : beats.subList(workout.getFirstValid(), workout.getLastValid() + 1)) {
			if (beat.isValid()) {
				rate = (int) (relativeRate(workout, beat.getPeriode()) * displayY);
				time = Math.round((beat.getTime() - startTime) * timeScale * displayX);
				curr = new Point(time, rate);
				graphics.drawLine(prev.x, prev.y, curr.x, curr.y);
				prev = curr;
			}
		}
		graphics.dispose();

		return image;
	}
	
	/**
	 * Converts a period into the percent of max heart rate
	 * (1/period - 1/max) / (1/min - 1/max) -> min*(max-period) / period*(max-min)
	 * 6e4/period(ms) = bpm and 6e4/bpm = period in ms
	 * Note that the max period is the min heart rate
	 */
	public static float relativeRate(Workout workout, int period) {
		return workout.getMin() * (workout.getMax() - period)
				/ (float) (period * (workout.getMax() - workout.getMin()));
	}

	/**
	 * Processes a work-out file
	 * @param logFile  the log file handler
	 */
	public static void processFile(File logFile) throws IOException {
		List<Workout> workouts = DataAnalizer.parseLog(GraphorWindow.loadFile(logFile));
		Workout first = workouts.get(0);
		int duration = Math.round(first.getDuration() / 60000);
		System.out.println("file:" + logFile.getName() + "  workouts:" + workouts.size() + "  duration:" + duration);
		ImageIO.write(DataAnalizer.visualize(first), "gif",
				new File(logFile.getCanonicalPath().replaceAll("LOG$", "gif")));
	}
}
