package GUI;

import java.awt.FlowLayout;
import java.awt.event.*;
import javax.swing.*;

import Utilities.Utilities;

public class ErrorDialog extends JDialog{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public ErrorDialog(JFrame owner,String s) {
		super(owner,"ERROR!",true);
		setLayout(new FlowLayout());
		JLabel text= new JLabel(s);
		add(text);
		addWindowListener(
				new WindowAdapter(){
					public void windowClosing(WindowEvent a){dispose();}
				}
				);
		
		setResizable(false);
		setSize(400,100);
		Utilities.centerScreen(this);
		setVisible(true);
	}
	public ErrorDialog(JDialog owner,String s) {
		super(owner,"ERROR!",true);
		setLayout(new FlowLayout());
		JLabel text= new JLabel(s);
		add(text);
		addWindowListener(
				new WindowAdapter(){
					public void windowClosing(WindowEvent a){dispose();}
				}
				);
		
		setResizable(false);
		setSize(400,100);
		Utilities.centerScreen(this);
		setVisible(true);
	}

}