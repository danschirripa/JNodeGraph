package com.javashell.jnodegraph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.javashell.jnodegraph.JNodeComponent.NodePoint;
import com.javashell.jnodegraph.exceptions.IncorrectLinkageException;

public class JNodeFlowPane extends JComponent {
	private static final long serialVersionUID = -4163272461603981518L;
	private boolean isLinking = false;
	private JNodeComponent currentLinkage = null, currentMove = null, currentSelection = null;
	private Hashtable<JNodeComponent, HashSet<Linkage>> links;
	private Linkage selectedLinkage = null;
	private boolean debugLinkages = false;

	public JNodeFlowPane() {
		links = new Hashtable<>();
		final FlowNodeActionListener actionListener = new FlowNodeActionListener();
		addMouseListener(actionListener);
		addMouseMotionListener(actionListener);
		getInputMap().put(KeyStroke.getKeyStroke("DELETE"), "delete");
		getInputMap().put(KeyStroke.getKeyStroke("BACK_SPACE"), "delete");
		getActionMap().put("delete", new AbstractAction() {
			private static final long serialVersionUID = 7750679448158046120L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (selectedLinkage != null) {
					removeLinkage(selectedLinkage);
					selectedLinkage = null;
				}
			}
		});
	}

	public void createLinkage(JNodeComponent origin, JNodeComponent child) throws IncorrectLinkageException {
		if (!links.containsKey(origin)) {
			links.put(origin, new HashSet<>());
		}

		links.get(origin).add(new Linkage(child, new Path2D.Float(), origin));
		origin.addChildLinkage(child, true);
	}

	public void startLinkage(JNodeComponent origin) {
		isLinking = true;
		currentLinkage = origin;
	}

	public void stopLinkage(JNodeComponent linkTerminator) {
		if (isLinking && currentLinkage != linkTerminator) {
			// Linkages stored with transmitter as key, receivers as value set
			try {
				if (linkTerminator.getNodeType() == NodeType.Transmitter
						|| (linkTerminator.getNodeType() == NodeType.Transceiver
								&& currentLinkage.getNodeType() == NodeType.Receiver)) {
					if (!links.containsKey(linkTerminator)) {
						links.put(linkTerminator, new HashSet<>());
					}

					links.get(linkTerminator).add(new Linkage(currentLinkage, new Path2D.Float(), linkTerminator));
					linkTerminator.addChildLinkage(currentLinkage, true);
				} else {
					if (!links.containsKey(currentLinkage)) {
						links.put(currentLinkage, new HashSet<>());
					}
					links.get(currentLinkage).add(new Linkage(linkTerminator, new Path2D.Float(), currentLinkage));
					currentLinkage.addChildLinkage(linkTerminator, true);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		isLinking = false;
		if (currentLinkage instanceof NodePoint) {
			((NodePoint) currentLinkage).setLinkingInput(false);
			((NodePoint) currentLinkage).setLinkingOutput(false);
		}
		currentLinkage = null;
	}

	public void stopLinkage() {
		isLinking = false;
		if (currentLinkage instanceof NodePoint) {
			((NodePoint) currentLinkage).setLinkingInput(false);
			((NodePoint) currentLinkage).setLinkingOutput(false);
		}
		currentLinkage = null;
	}

	public Hashtable<JNodeComponent, HashSet<Linkage>> getLinkages() {
		return links;
	}

	public void removeLinkage(Linkage link) {
		links.get(link.origin).remove(link);
		link.origin.removeChildLinkage(link.node);
	}

	public void startMoving(JNodeComponent mover) {
		if (mover == currentMove)
			stopMoving();
		else
			currentMove = mover;
	}

	public void stopMoving() {
		currentMove = null;
	}

	public void setSelected(JNodeComponent selected) {
		if (this.currentSelection != null)
			this.currentSelection.setBorder(null);
		this.currentSelection = selected;
		currentSelection.setBorder(BorderFactory.createLineBorder(Color.RED, 2, true));
	}

	public JNodeComponent getSelection() {
		return currentSelection;
	}

	public void clearSelection() {
		if (this.currentSelection != null) {
			this.currentSelection.setBorder(null);
			this.currentSelection = null;
		}
	}

	public boolean isLinking() {
		return isLinking;
	}

	public boolean isMoving() {
		return currentMove != null;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		drawLinkages(g);
		if (isLinking)
			drawCurrentLinkage(g);
		if (currentMove != null) {
			final Point centerPoint = getMousePosition();
			centerPoint.translate(-(currentMove.getWidth() / 2), -(currentMove.getHeight() / 2));
			currentMove.setLocation(centerPoint);
		}

	}

	private void drawCurrentLinkage(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		int x, y;
		if (currentLinkage instanceof NodePoint) {
			NodePoint selectedNodePoint = (NodePoint) currentLinkage;
			JNodeComponent parent = selectedNodePoint.getParentNodeComponent();
			RoundRectangle2D.Float selectedBounds;

			if (selectedNodePoint.isLinkingInput())
				selectedBounds = selectedNodePoint.getInputPointBounds();
			else
				selectedBounds = selectedNodePoint.getOutputPointBounds();

			x = parent.getX() + currentLinkage.getX() + (int) selectedBounds.getX();
			y = parent.getY() + currentLinkage.getY() + (int) selectedBounds.getY();
			y = y + (currentLinkage.getHeight() / 2);
		} else {
			x = currentLinkage.getX();
			y = currentLinkage.getY() + (currentLinkage.getHeight() / 2);
		}
		g2.drawLine(x, y, getMousePosition().x, getMousePosition().y);
	}

	private void drawLinkages(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		Enumeration<JNodeComponent> keys = links.keys();
		final Color originalColor = g2.getColor();
		g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		while (keys.hasMoreElements()) {
			JNodeComponent key = keys.nextElement();
			int startX, startY;

			if (key instanceof JNodeComponent.NodePoint) {
				startX = ((JNodeComponent.NodePoint) key).getParentNodeComponent().getX() + key.getX();
				startY = ((JNodeComponent.NodePoint) key).getParentNodeComponent().getY() + key.getY();
				if (key.getNodeType() != NodeType.Receiver) {
					startX = startX + key.getWidth();
				}
			} else {
				startX = key.getX();
				startY = key.getY() + (key.getHeight() / 2);
			}
			HashSet<Linkage> linkages = links.get(key);
			for (Linkage link : linkages) {
				link.link = new Path2D.Float();
				link.link.moveTo(startX, startY);
				int endX = link.node.getX();
				int endY = link.node.getY() + (link.node.getHeight() / 2);
				if (link.node instanceof JNodeComponent.NodePoint) {
					endX = ((JNodeComponent.NodePoint) link.node).getParentNodeComponent().getX() + link.node.getX();
					endY = ((JNodeComponent.NodePoint) link.node).getParentNodeComponent().getY() + link.node.getY();
				}
				link.link.lineTo(endX, endY);
				link.link.closePath();
				GradientPaint gp = new GradientPaint(startX, startY, Color.RED, endX, endY, Color.BLUE);
				if (debugLinkages)
					g2.setPaint(gp);
				else
					g2.setColor(key.getLinkColor());

				if (selectedLinkage == link) {
					g2.setColor(Color.RED);
				}
				g2.draw(link.link);
				g2.setColor(originalColor);
			}
		}
	}

	public void setDebugLinkages(boolean doDebug) {
		debugLinkages = doDebug;
	}

	public class Linkage {
		private JNodeComponent node, origin;
		private Path2D link;

		public Linkage(JNodeComponent node, Path2D link, JNodeComponent origin) {
			this.node = node;
			this.link = link;
			this.origin = origin;
		}

		public JNodeComponent getNode() {
			return node;
		}

		public JNodeComponent getOrigin() {
			return origin;
		}
	}

	private class FlowNodeActionListener implements MouseListener, MouseMotionListener {

		@Override
		public void mouseDragged(MouseEvent e) {
		}

		@Override
		public void mouseMoved(MouseEvent e) {
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (SwingUtilities.isRightMouseButton(e)) {

			} else {
				clearSelection();
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			Enumeration<JNodeComponent> keys = links.keys();
			while (keys.hasMoreElements()) {
				JNodeComponent key = keys.nextElement();
				HashSet<Linkage> linkages = links.get(key);
				Point topLeft = (Point) e.getPoint().clone();
				topLeft.translate(-4, -4);
				Rectangle roi = new Rectangle(topLeft, new Dimension(8, 8));
				for (Linkage link : linkages) {
					if (link.link.intersects(roi)) {
						selectedLinkage = link;
						return;
					}
				}
				selectedLinkage = null;
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

	}
}
