package org.jj.heart;

import gnu.io.CommPortIdentifier;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jj.heart.data.Workout;

/**
 * A custom JFrame for displaying heart rate data
 * @author jjones
 */
public class GraphorWindow extends JFrame {
	private static final long serialVersionUID = 1L;
	private ImagePanel imagePanel;
	private JLabel summary;
	private File source;
	private Workout workout;
	private JFileChooser logChooser;
	private JMenu serialMenu;

	public GraphorWindow() {
		super("Heart rate data");
		try {
			setIconImage(ImageIO.read(new File("heart.png")));
		} catch (IOException e) {
		}
		initializeWindow();
	}

	/**
	 * Initializes all panels and objects needed
	 */
	private void initializeWindow(){
		// evol I should add some nice UI manager (ice/metal/..)
		getContentPane().setLayout(new BorderLayout());
		// TODO load persistent position/size data
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		logChooser = new JFileChooser();
		// TODO load path from persistent data
		logChooser.setCurrentDirectory(new File("d:/jjo/logs"));

		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.getAccessibleContext().setAccessibleDescription("file stuff...");

		JMenuItem openFile = new JMenuItem("Open",KeyEvent.VK_O);
		openFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    int returnVal = logChooser.showOpenDialog(getContentPane());
			    if(returnVal == JFileChooser.APPROVE_OPTION) {
			    	source = logChooser.getSelectedFile();
			    	// update path when manually typed
			    	logChooser.setCurrentDirectory(source.getParentFile());
			    	graphFile(source);
			    }
			}
		});
		fileMenu.add(openFile);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		menuBar.add(fileMenu);
		
		JMenuItem saveFile = new JMenuItem("Save image",KeyEvent.VK_S);
		saveFile.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				// work-out must have been parsed/set first
				if (workout == null || !workout.hasValidBeat()) {
					return;
				}
				JFileChooser imgChooser = new JFileChooser();
				// TODO the source could be null if data comes from serial port
				imgChooser.setCurrentDirectory(logChooser.getCurrentDirectory());
//				imgChooser.setCurrentDirectory(source.getParentFile());
				imgChooser.setFileFilter(new FileNameExtensionFilter("gif image","gif"));
				int returnVal = imgChooser.showSaveDialog(getContentPane());
			    if(returnVal == JFileChooser.APPROVE_OPTION) {
			    	File f = imgChooser.getSelectedFile();
			    	BufferedImage img = DataAnalizer.visualize(workout, new Dimension(1440, 1080));
					try {
						// seems logical to automatically add the gif extension when missing
						if(!f.getName().toLowerCase().matches(".*gif")){
							f = new File(f.getParent(),f.getName()+".gif");
						}
						ImageIO.write(img, "gif",f);
					} catch (IOException e1) {
					}
			    }
				
			}
		});
		fileMenu.add(saveFile);
		
		serialMenu = new JMenu("Serial Ports");
		serialMenu.setMnemonic(KeyEvent.VK_P);
		fileMenu.addSeparator();
		fileMenu.add(serialMenu);
        for(CommPortIdentifier port : discoverPorts()){
        	serialMenu.add(new JMenuItem(port.getName()));
        	// TODO add action: communicate with port
        	// open chooser for fake files on CD card
        	// once a file chosen, read file + pass contents to parse
        }
		
		// TODO work-out menu for switching between work-outs

		imagePanel = new ImagePanel(getContentPane());
		getContentPane().add(imagePanel, BorderLayout.CENTER);

		summary = new JLabel();
		getContentPane().add(new JPanel().add(summary), BorderLayout.SOUTH);

		pack();
		// TODO load persistent position/size data
		// windows adds 8px for the sides and 57px for top and bottom
		setBounds(new Rectangle(808, 657));
	}

	/**
	 * Read file
	 * @param f File to parse and graph
	 */
	protected void graphFile(File f) {
		if (f == null || !f.exists()) {
			return;
		}
		System.out.print(f.getAbsoluteFile());
		setTitle(f.getAbsolutePath());
		
		String contents = null;
		try {
			contents = loadFile(f);
		} catch (IOException e) {
			return; // evol add some sort of error msg system
		}
		
		List<Workout> workouts = DataAnalizer.parseLog(contents);
		System.out.print("  workouts:" + workouts.size());
		if (workouts.size() == 0) {
			return; // evol add some sort of error msg system
		}

		graphWorkout(workouts.get(0));
		source = f;
	}
	
	/**
	 * Displays work-out data in window
	 * @param w the work-out statistics to display
	 */
	protected void graphWorkout(Workout w){
		// make sure that the work-out has valid data before displaying it
		if (w == null || !w.hasValidBeat()) {
			return; // evol add some sort of error msg system
		}
		
		workout = w;
		imagePanel.setImage(DataAnalizer.visualize(w));
		int time = Math.round(w.getDuration() / 60000); // in minutes
		int ave = Math.round(60000 / w.getAverage()); // in bpm
		int max = 60000 / w.getMin(); // in bpm
		int min = 60000 / w.getMax(); // in bpm
		String label = "  time:" + time + "  ave:" + ave + "  min:" + min + "  max:" + max;
//		System.out.println(label);// debug info
		summary.setText(label);
	}

	/**
	 * Discovers what serial ports are available
	 * @return collection of port identifiers
	 */
	protected List<CommPortIdentifier> discoverPorts(){
        List<CommPortIdentifier> serialPorts = new ArrayList<CommPortIdentifier>();
        @SuppressWarnings("unchecked")
		Enumeration<CommPortIdentifier> allPorts = CommPortIdentifier.getPortIdentifiers();
        for (CommPortIdentifier port : Collections.list(allPorts)) {
        	// filter for serial ports
			if (port.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				serialPorts.add(port);
            }
        }
		
		return serialPorts;
	}

	/**
	 * Loads the contents of a file into a String
	 * @param f the file to load
	 * @return a string with the contents of the file.
	 * @throws IOException
	 */
	public static String loadFile(File f) throws IOException{
		StringBuffer buffer = new StringBuffer();
		FileReader in = new FileReader(f);
		int letter;
		while(-1<(letter=in.read())){
			buffer.append((char)letter);
		}
		return buffer.toString();
	}

}