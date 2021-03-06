/*
 * Copyright (C) 2005 Jordan Kiang
 * jordan-at-kiang.org
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package hanzilookup.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

/**
 * CharacterCanvas is the Swing component on which that accepts handwriting input.
 * It holds an internal WrittenCharacter instance, which it adds new strokes to when they are input.
 * 
 * WrittenCharacters are composed of WrittenStrokes, which the wrap a List of WrittenPoints.  The points
 * are generated by mouse action.  Dragging the mouse while clicked a certain distance
 * will add another point to the current WrittenStroke.  The WrittenPoints of the WrittenStrokes can
 * later be examined to do comparison.
 * 
 * It extends JLabel so that subclasses could possibly choose to display some text in the background.
 * This is mostly so that we can trace characters with a Character entry tool.
 */
public class CharacterCanvas extends JLabel implements MouseInputListener {
	
	// The WrittenCharacter that is operated on as mouse input is recorded.
	private WrittenCharacter inputCharacter = new WrittenCharacter();
	
	// We collect a current stroke of input and add whole strokes at a time to the inputCharacter.
	private WrittenCharacter.WrittenStroke currentStroke;
	// Need to keep track of the previous point as we are building a new WrittenStroke.
	private WrittenCharacter.WrittenPoint previousPoint;
	
	// The awt.Stroke that is used to paint the lines on the Canvas.
	// An awt.Stroke should not be confused with a WrittenStroke.
	private Stroke paintStroke = new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	
	   // listeners that are notified when a stroke is finished, LinkedHashSet ensures uniqueness, iterates in order.
	private Set strokesListeners = new LinkedHashSet();
	
	
	////////////////////
	
	/**
	 * Builds a new CharacterCanvas.
	 */
	public CharacterCanvas() {
		this.setOpaque(true);
		this.setBackground(Color.WHITE);
		
		this.addMouseListener(this);		// for MousePressed and MouseReleased
		this.addMouseMotionListener(this);	// for MouseDragged
	}
	
	////////////////////
	
	/**
	 * Clears the Canvas and resets it for entry of another Character.
	 */
	public void clear() {
		this.inputCharacter.clear();	// wipe the WrittenCharacter of all its WrittenStrokes.
		this.currentStroke = null;
	}
	
	/**
	 * "Undo" the last stroke added to the character.
	 */
	public void undo() {
		List strokesList = this.inputCharacter.getStrokeList();
		if(strokesList.size() > 0) {
			strokesList.remove(strokesList.size() - 1);
		}
	}
	
	/**
	 * Getter for the WrittenCharacter.
	 * Other components that actually do the analysis will need access to it.
	 * 
	 * @return the WrittenCharacter operated on by this Canvas
	 */
	public WrittenCharacter getCharacter() {
		return this.inputCharacter;
	}
	
	////////////////////

	/**
	 * The mouse being pressed signals the beginning of a new WrittenStroke.
	 * 
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent e) {
		// When run as an applet, the top-level seems to get the focus rather than,
		// a sub-component, and the macros won't work unless a component in the
		// window has the focus.  So we take the focus if the top-level has the focus.
		// We don't always take the focus so that we don't steal the focus if we don't
		// have to.  Surely there is a better way?  How to give initial focus in applet?
		if(SwingUtilities.getRoot(this).isFocusOwner()) {
			this.requestFocus();
		}

		this.previousPoint = this.inputCharacter.new WrittenPoint(e.getX(), e.getY());
	}
	
	// The minimum pixel distance the mouse must be dragged before a new point is added.
	// Making this too small will result in lots of points that need to be analyzed.
	// Making this too large results in halting mouse input with characters composed of lines.
	static private final double MIN_STROKE_SEGMENT_LENGTH = 5.0;
	
	/**
	 * We add another WrittenPoint to a WrittenStroke when the mouse has been dragged a certain distance.
	 * 
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	public void mouseDragged(MouseEvent e) {
		WrittenCharacter.WrittenPoint nextPoint = this.inputCharacter.new WrittenPoint(e.getX(), e.getY());
		
		if(null != this.previousPoint && this.previousPoint.distance(nextPoint) >= MIN_STROKE_SEGMENT_LENGTH) {
			// If the mouse has not been dragged a minimal distance, then we ignore this event.
			
			if(null == this.currentStroke) {
				// If the current stroke is null, that means that the this is the second point of the stroke.
				// The first point is stored this.previousPoint.
				// Now that we know that there is a second point we can add both points to a newly initialized stroke.
				this.currentStroke = this.inputCharacter.new WrittenStroke();
				this.currentStroke.addPoint(this.previousPoint);
			}
			
			// Add the new point to the WrittenStroke, and cycle the previousPoint.
			this.currentStroke.addPoint(nextPoint);
			this.previousPoint = nextPoint;
			this.repaint();
		}
	}
	
	/**
	 * Release of the mouse button indicates that the current stroke has ended.
	 * 
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent e) {
		this.mouseDragged(e);	// do the same stuff we do when the mouse is dragged
	
		if(null != this.currentStroke) {
			// The current stroke will still be null if the mouse wasn't dragged far enough for a new stroke.
			
			// Add the new WrittenStroke to the WrittenCharacter, and reset input variables.
			this.inputCharacter.addStroke(this.currentStroke);
			this.previousPoint = null;
			this.currentStroke = null;
			this.repaint();
		}
		
		this.notifyStrokesListeners();
	}
	
	// unused
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}
	
	////////////////////
	
	/**
	 * Paints this Canvas.
	 * Painting involves painting lines between all the points of a WrittenCharacter.
	 * 
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		// The object passed in is always a Graphics2D, so we can cast it.
		// We need a Graphics2D to set the painting stroke, and to set aliasing hints.
		Graphics2D g2D = (Graphics2D)g;
		g2D.setStroke(this.paintStroke);
		g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		if(null != this.currentStroke) {
			// A non-null currentStroke means that a new WrittenStroke is being made (the mouse button is held down).
			// Since the stroke is not yet part of the character, we have to paint it separately.
			this.paintStroke(this.currentStroke, g);
		}
		
		// Paints all the WrittenStrokes that have already been added to the WrittenCharacter.
		this.paintCharacter(g);
	}
	
	/**
	 * Paint the instance character on the canvas.
	 * 
	 * @param g the Graphics object passed to paintComponent
	 */
	protected void paintCharacter(Graphics g) {
		// Just iterate over each of the WrittenStrokes and pass them to a stroke painting method.
		List strokeList = this.inputCharacter.getStrokeList();
		for(Iterator strokeIter = strokeList.iterator(); strokeIter.hasNext();) {
			WrittenCharacter.WrittenStroke nextStroke = (WrittenCharacter.WrittenStroke)strokeIter.next();
			this.paintStroke(nextStroke, g);
		}
	}
	
	/**
	 * Paints the given WrittenStroke on the canvas.
	 * 
	 * @param stroke the WrittenStroke to paint
	 * @param g the Graphics object passed to paintComponent
	 */
	protected void paintStroke(WrittenCharacter.WrittenStroke stroke, Graphics g) {
		// To paint a WrittenStroke, we just want to paint lines between each of its WrittenPoints.
		
		// Paint the strokes in black.
		g.setColor(Color.BLACK);
		
		// We take it for granted that each WrittenStroke has at least two points.
		// This means we should be able to safely call next() without checking.
		Iterator pointIter = stroke.getPointList().iterator();
		WrittenCharacter.WrittenPoint previousPoint = (WrittenCharacter.WrittenPoint)pointIter.next();
		
		// Iterate over all the WrittenPoints, drawing lines between the next point and the previous point.
		while(pointIter.hasNext()) {
			WrittenCharacter.WrittenPoint nextPoint = (WrittenCharacter.WrittenPoint)pointIter.next();
			
			g.drawLine((int)previousPoint.getX(), (int)previousPoint.getY(), (int)nextPoint.getX(), (int)nextPoint.getY());
			previousPoint = nextPoint;
		}
	}
	
	///////////////////
	// StrokesListener stuff
	// Probably not really necessary to abstract this stuff out, probably only one consumer of strokes...
	
    /**
     * Register the given StrokesListener so that its strokeFinished method
     * will be invoked whenever a new Stroke is added.
     * @param listener the StrokesListener to add
     */
    public void addStrokesListener(StrokesListener listener) {
        if(null != listener) {	
            synchronized(this.strokesListeners) {
        	    this.strokesListeners.add(listener);
	        }
        }
        
        // No effect if listener is null.
    }
    
    /**
     * Deregisters the given StrokesListener
     * @param listener the StrokesListener to remove
     */
    public void removeStrokesListener(StrokesListener listener) {
        if(null != listener) {
            synchronized(this.strokesListeners) {
	            this.strokesListeners.remove(listener);
	        }
        }
        
        // No effect if listener is null.
    }
    
    /**
     * Invokes the strokeFinished method on all registered StrokesListeners.
     */
    private void notifyStrokesListeners() {
        synchronized(this.strokesListeners) {
            for(Iterator listenerIter = this.strokesListeners.iterator(); listenerIter.hasNext();) {
                StrokesListener nextListener = (StrokesListener)listenerIter.next();
                nextListener.strokeFinished(new StrokeEvent());
            }
        }
    }
	
    /**
     * Components that want to know when a new stroke has been added will need to implement
     * this interface and register the listener via the addStrokesListener method.
     */
	static public interface StrokesListener {
	    public void strokeFinished(StrokeEvent e);
	}
	
	/**
	 * A simple event that serves as the argument for the strokeFinished method.
	 */
	public class StrokeEvent extends EventObject {
	    private StrokeEvent() {
	        // the source is always this canvas.
	        super(CharacterCanvas.this);
	    }
	}
}

