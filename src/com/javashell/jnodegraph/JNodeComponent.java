package com.javashell.jnodegraph;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.RoundRectangle2D;
import java.util.HashSet;
import java.util.UUID;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import com.javashell.jnodegraph.exceptions.IncorrectLinkageException;

public abstract class JNodeComponent extends JComponent {
	private static final long serialVersionUID = 1L;

	private int padding = 10;
	private NodeActionListener nal;
	private JNodeFlowPane flow;
	private NodeType type = NodeType.Transceiver;
	private final HashSet<NodePoint> nodePoints;
	private final UUID uuid = UUID.randomUUID();

	private Image icon = null;

	private Font calibri = new Font("Calibri", Font.BOLD, 10);
	private String nodeName = "Node";

	private final Class<?> objClass;

	public JNodeComponent(JNodeFlowPane flow, Class<?> objClass) {
		this.objClass = objClass;

		nal = new NodeActionListener(this);
		this.flow = flow;
		this.nodePoints = new HashSet<>();
		if (!(this instanceof NodePoint)) {
			addMouseListener(nal);
			addMouseMotionListener(nal);
		}
		setSize(90, 65);
	}

	public Class<?> getObjectClass() {
		return objClass;
	}

	public UUID getUUID() {
		return uuid;
	}

	public void setNodeName(String name) {
		this.nodeName = name;
		this.setToolTipText(name);
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setIcon(Image icon) {
		this.icon = icon;
	}

	public Image getIcon() {
		return icon;
	}

	public void setNodeType(NodeType type) {
		this.type = type;
	}

	public void addNodePoint(NodePoint point) {
		setSize(getWidth(), getHeight() + point.getHeight());
		point.setBounds(0, getHeight() - point.getHeight(), point.getWidth(), point.getHeight());
		nodePoints.add(point);
		add(point);
	}

	public HashSet<NodePoint> getNodePoints() {
		return nodePoints;
	}

	public NodeType getNodeType() {
		return type;
	}

	@Override
	public void paintComponent(Graphics g) {
		g.setFont(calibri);
		g.setColor(Color.LIGHT_GRAY);
		g.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

		if (icon != null) {
			int iconW, iconH;
			if (getWidth() > getHeight()) {
				iconW = getHeight() / 3;
				iconH = getHeight() / 3;
			} else {
				iconW = getWidth() / 3;
				iconH = getWidth() / 3;
			}
			g.drawImage(icon, 5, 15, iconW, iconH, this);
		}

		g.setColor(Color.black);
		g.drawString(nodeName, 5, 15);
	}

	public abstract void addOriginLinkage(JNodeComponent origin, boolean cascade) throws IncorrectLinkageException;

	public abstract void addChildLinkage(JNodeComponent child, boolean cascade) throws IncorrectLinkageException;

	public abstract void removeChildLinkage(JNodeComponent child);

	public abstract void removeOriginLinkage(JNodeComponent origin);

	public Color getLinkColor() {
		return Color.BLACK;
	}

	public static abstract class NodePoint extends JNodeComponent {
		private final RoundRectangle2D.Float inputPoint, outputPoint;
		private NodePointActionListener inputNal, outputNal;
		private JNodeComponent parent;
		private boolean linkingInput = false, linkingOutput = false;
		private Color color;

		public NodePoint(JNodeFlowPane flow, JNodeComponent parent, Class<?> objClass) {
			super(flow, objClass);
			setSize(parent.getWidth(), 10);
			inputPoint = new RoundRectangle2D.Float(2, 0, getWidth() / 4, getHeight() - 2, 2, 2);
			outputPoint = new RoundRectangle2D.Float(getWidth() - (inputPoint.width) - 2, 1, getWidth() / 4,
					getHeight() - 2, 2, 2);
			inputNal = new NodePointActionListener(this, inputPoint, true);
			outputNal = new NodePointActionListener(this, outputPoint, false);
			addMouseMotionListener(inputNal);
			addMouseMotionListener(outputNal);
			addMouseListener(inputNal);
			addMouseListener(outputNal);
			parent.addNodePoint(this);
			this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			this.parent = parent;
		}

		public JNodeComponent getParentNodeComponent() {
			return parent;
		}

		public void setColor(Color color) {
			this.color = color;
		}

		@Override
		public Color getLinkColor() {
			return color;
		}

		public void setLinkingOutput(boolean linking) {
			if (isLinkingInput())
				setLinkingInput(false);
			linkingOutput = linking;
		}

		public void setLinkingInput(boolean linking) {
			if (isLinkingOutput())
				setLinkingOutput(false);
			linkingInput = linking;
		}

		public boolean isLinkingOutput() {
			return linkingOutput;
		}

		public boolean isLinkingInput() {
			return linkingInput;
		}

		public RoundRectangle2D.Float getInputPointBounds() {
			return inputPoint;
		}

		public RoundRectangle2D.Float getOutputPointBounds() {
			return outputPoint;
		}

		@Override
		public void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			inputPoint.setRoundRect(2, 1, getWidth() / 4, getHeight() - 2, 2, 2);
			outputPoint.setRoundRect(getWidth() - (inputPoint.width) - 2, 1, getWidth() / 4, getHeight() - 2, 2, 2);
			Color original = g2.getColor();
			if (this.getNodeType() == NodeType.Transceiver) {
				// Add node points to either side (input and output)
				g2.draw(inputPoint);
				g2.draw(outputPoint);
				g2.setColor(color);
				g2.fill(inputPoint);
				g2.fill(outputPoint);
			} else if (this.getNodeType() == NodeType.Transmitter) {
				// Add node points to right side only (output)
				g2.draw(outputPoint);
				g2.setColor(color);
				g2.fill(outputPoint);
			} else {
				// Add node points to left side only (input)
				g2.draw(inputPoint);
				g2.setColor(color);
				g2.fill(inputPoint);
			}
			g2.setColor(original);
		}

	}

	private class NodeActionListener implements MouseListener, MouseMotionListener, ActionListener {
		private final JNodeComponent thisNode;

		public NodeActionListener(JNodeComponent thisNode) {
			this.thisNode = thisNode;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
		}

		@Override
		public void mouseDragged(MouseEvent e) {
		}

		@Override
		public void mouseMoved(MouseEvent e) {

		}

		@Override
		public void mouseClicked(MouseEvent e) {
			flow.setSelected(thisNode);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (isWithinPadding(e.getPoint())) {
				if (flow.isLinking()) {
					// Complete the linkage
					flow.stopLinkage(thisNode);
				} else {
					// Start a linkage
					flow.startLinkage(thisNode);
				}
			} else {
				flow.startMoving(thisNode);
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			flow.stopMoving();
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			if (isWithinPadding(e.getPoint())) {
				// Change cursor to "Plus" to indicate option to create a linkage
			}
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

		private boolean isWithinPadding(Point p) {
			if (p.x > (thisNode.getWidth() - padding) || p.x < padding || p.y > (thisNode.getHeight() - padding)
					|| p.y < (padding))
				return true;
			return false;
		}

	}

	private class NodePointActionListener implements MouseListener, MouseMotionListener, ActionListener {
		private final NodePoint thisNode;
		private final RoundRectangle2D.Float bounds;
		private boolean input = false;

		public NodePointActionListener(NodePoint thisNode, RoundRectangle2D.Float bounds, boolean input) {
			this.thisNode = thisNode;
			this.bounds = bounds;
			this.input = input;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
		}

		@Override
		public void mouseDragged(MouseEvent e) {
		}

		@Override
		public void mouseMoved(MouseEvent e) {

		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (SwingUtilities.isRightMouseButton(e) && getComponentPopupMenu() != null) {
				getComponentPopupMenu().show(thisNode, e.getX(), e.getY());
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (isInBounds(e.getPoint())) {
				if (flow.isLinking()) {
					// Complete the linkage
					flow.stopLinkage(thisNode);
				} else {
					// Start a linkage
					if (input)
						thisNode.setLinkingInput(true);
					else
						thisNode.setLinkingOutput(true);
					flow.startLinkage(thisNode);
				}
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			if (isInBounds(e.getPoint())) {
				// Change cursor to "Plus" to indicate option to create a linkage
				// System.out.println("IN BOUNDS");
			}
		}

		@Override
		public void mouseExited(MouseEvent e) {

		}

		public boolean isInBounds(Point p) {
			// System.out.println(p.x + " " + p.y);
			if (p.x >= bounds.x && p.x <= (bounds.x + bounds.width) && p.y >= bounds.y
					&& p.y <= (bounds.y + bounds.height))
				return true;
			return false;
		}

	}

}
