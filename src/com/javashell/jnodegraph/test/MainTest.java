package com.javashell.jnodegraph.test;

import javax.swing.JFrame;

import com.javashell.jnodegraph.JNodeComponent;
import com.javashell.jnodegraph.JNodeFlowPane;

public class MainTest {

	public static void main(String[] args) {
		JFrame testFrame = new JFrame("Test");
		JNodeFlowPane flowPane = new JNodeFlowPane();
		testFrame.setContentPane(flowPane);

		JNodeComponent test1 = new JNodeComponent(flowPane) {

			@Override
			public void addOriginLinkage(JNodeComponent origin) {
			}

			@Override
			public void addChildLinkage(JNodeComponent child) {
			}

			@Override
			public void removeChildLinkage(JNodeComponent child) {
			}

			@Override
			public void removeOriginLinkage(JNodeComponent origin) {
			}

		};
		JNodeComponent test2 = new JNodeComponent(flowPane) {

			@Override
			public void addOriginLinkage(JNodeComponent origin) {
			}

			@Override
			public void addChildLinkage(JNodeComponent child) {
			}

			@Override
			public void removeChildLinkage(JNodeComponent child) {
			}

			@Override
			public void removeOriginLinkage(JNodeComponent origin) {
			}

		};

		test1.setNodeName("Test1");
		test2.setNodeName("Test2");

		test1.setLocation(10, 50);
		test2.setLocation(400, 25);

		flowPane.add(test1);
		flowPane.add(test2);

		testFrame.setSize(500, 500);
		testFrame.setVisible(true);

		try {
			while (testFrame.isVisible()) {
				Thread.sleep(33);
				testFrame.repaint();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
