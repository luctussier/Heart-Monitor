package org.jj.heart;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.JPanel;

public class ImagePanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private Image bi;
	private Container parent;
	public ImagePanel(Container parent){
		super();
		this.parent = parent;
	}
	
	public void setImage(Image image){
		this.bi = image;
		invalidate();
		repaint();
	}
	
	public void paint(Graphics g){
		Graphics2D g2 = (Graphics2D) g;
		Dimension d = parent.getSize();
		g2.drawImage(bi, 0, 0, (int)d.getWidth(), (int)d.getHeight(), null);
	}

}
