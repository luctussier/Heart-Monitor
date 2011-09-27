package org.jj.heart;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
	private JFileChooser chooser;

	public GraphorWindow() {
		super("Heart rate data");
		// TODO put a heart icon on the menu bar
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
		// TODO can I add some nice UI manager (ice/metal/..)
		getContentPane().setLayout(new BorderLayout());
		// TODO load persistent position/size data
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JMenuBar menuBar;
		JMenu fileMenu;
		JMenuItem fileOpen;
		
		menuBar = new JMenuBar();
		
		fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.getAccessibleContext().setAccessibleDescription("file stuff...");
		menuBar.add(fileMenu);

		fileOpen = new JMenuItem("Open",KeyEvent.VK_O);
		fileOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    int returnVal = chooser.showOpenDialog(getContentPane());
			    if(returnVal == JFileChooser.APPROVE_OPTION) {
			    	source = chooser.getSelectedFile();
			    	graphFile(source);
			    }
//				CommPortIdentifier commPort = initSerial();
//				if (commPort == null) { // file chooser
//				} else { // TODO chooser listing of files on SD card
//				}
			}
		});
		fileMenu.add(fileOpen);

		setJMenuBar(menuBar);
		
		// TODO open-USB; save visualization gif
		// TODO work-out menu for switching between work-outs

		chooser = new JFileChooser();
		// TODO debug remove this in later version or save it to user's workspace
		chooser.setCurrentDirectory(new File("d:/jjo/logs"));

		imagePanel = new ImagePanel(getContentPane());
		getContentPane().add(imagePanel, BorderLayout.CENTER);

		summary = new JLabel();
		getContentPane().add(new JPanel().add(summary), BorderLayout.SOUTH);

		pack();
		// TODO load persistent position/size data
		// windows adds 8px for the sides and 57px for top and bottom
		setBounds(new Rectangle(808, 657));
	}

	protected void graphFile(File f){
		if(f==null || !f.exists()){
			return;
		}
		System.out.print(f.getAbsoluteFile());
		setTitle(f.getAbsolutePath());
		
		String contents=null;
		List<Workout> workouts;
		try {
			contents = loadFile(f);
		} catch (IOException e) {
			return; // evol add some sort of error msg system
		}
		
		workouts = DataAnalizer.parseLog(contents);
		System.out.print("  workouts:" + workouts.size());
		if(workouts.size()==0){
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
		if(w.hasValidBeat()){
			// make sure that the work-out has valid data before displaying it
			imagePanel.setImage(DataAnalizer.visualize(w));
			int time = Math.round(w.getDuration() / 60000); // in minutes
			int ave = Math.round(60000/w.getAverage()); // in bpm
			int max = 60000/w.getMin(); // in bpm
			int min = 60000/w.getMax(); // in bpm
			String label = "  time:" + time + "  ave:" + ave + "  min:" + min + "  max:" + max;
			// TODO debug info
			System.out.println(label);
			summary.setText(label);
		}
	}

	// TODO initialize serial port 
	protected CommPortIdentifier initSerial(){
		CommPortIdentifier commPort = null;
        Enumeration<CommPortIdentifier> thePorts = CommPortIdentifier.getPortIdentifiers();
//        for (CommPortIdentifier com : Collections.list(thePorts)) {
		while (thePorts.hasMoreElements() && commPort == null) {
            CommPortIdentifier com = thePorts.nextElement();
			if (com.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                try {
                    CommPort thePort = com.open("CommUtil", 50);
                    // TODO write(requestAck), read & validate(Ack), commPort=com, timeout 
                    thePort.close();
                } catch (PortInUseException e) {
//                    System.out.println("Port, "  + com.getName() + ", is in use.");
                } catch (Exception e) {
//                    System.err.println("Failed to open port " +  com.getName());
//                    e.printStackTrace();
                }
            }
        }
		
		return commPort;
	}
	
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