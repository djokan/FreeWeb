package GUI;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import DataStructures.Settings;
import Utilities.*;

public class MainWindow extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JSpinner spinner;
	private JSlider slider;
	private SystemTray sysTray;
	boolean l1=true,l2=true;
	/**
	 * 
	 */
	//private static final long serialVersionUID = 1L;
	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	
	public void makePacFile() throws IOException
	{
		
		byte[] pac = new byte[200];
		int a = getClass().getResourceAsStream("/resources/Proxy.pac").read(pac);
        FileOutputStream fout = new FileOutputStream("Proxy.pac");
        fout.write(pac, 0, a);
        fout.close();
	}
	
	public void loadTrayIcon() throws IOException {
		Image iconImage;
	    PopupMenu menu;
	    MenuItem item0;
	    TrayIcon trayIcon;

        if (SystemTray.isSupported()) {
            sysTray = SystemTray.getSystemTray();

            //iconImage  = ImageIO.read(new File("resources/trayIcon.png"));

            iconImage = ImageIO.read(getClass().getResourceAsStream("/resources/trayIcon.png"));
            
            menu = new PopupMenu();

            item0 = new MenuItem("Exit");

            item0.addActionListener(new ActionListener(){

				@Override
				public void actionPerformed(ActionEvent arg0) 
				{
					System.exit(0);
				}
            	
            	
            });
            
            menu.add(item0);

            trayIcon = new TrayIcon(iconImage, "FreeWeb", menu);
            trayIcon.addMouseListener(new MouseListener(){

				@Override
				public void mouseClicked(MouseEvent arg0) {
					if (arg0.getButton()== MouseEvent.BUTTON1) {
						MainWindow.this.setVisible(!MainWindow.this.isVisible());
						MainWindow.this.setExtendedState(JFrame.NORMAL);
					}
					
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					
					
				}

				@Override
				public void mouseExited(MouseEvent e) {
					
					
				}

				@Override
				public void mousePressed(MouseEvent e) {
					
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					
				}
            	
            });
            
            try {
                sysTray.add(trayIcon);
            } catch(AWTException e) {
                System.out.println(e.getMessage());
            }
        }
    }

	public void loadSettings()
	{
		File f = new File("settings.fwf");
		FileInputStream fin=null;
		ObjectInputStream ois;
		if(f.exists() && !f.isDirectory()) { 
			try {
				fin = new FileInputStream(f);
				Settings s;
				ois = new ObjectInputStream(fin);
				s= (Settings)ois.readObject();
				spinner.setValue(s.allocSpace);
				long size = Math.round(s.allocSpace*Math.pow(2,30));
				Utilities.updateSpace(size);
			} catch (Exception e) {
			} 
		}
		
		 
	}

	/**
	 * Create the frame.
	 */
	public MainWindow() {
		setTitle("FreeWeb");
		if (OSValidator.isWindows())
			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);// <- prevent closing
		else
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);// <- prevent closing
			
		addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {
		        MainWindow.this.setExtendedState(JFrame.ICONIFIED);
		    }
		});
		setBounds(100, 100, 549, 263);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JLabel lblNewLabel = new JLabel("FreeWeb service is working");
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel.setFont(new Font("Times New Roman", Font.PLAIN, 15));
		contentPane.add(lblNewLabel, BorderLayout.NORTH);
		
		JPanel panel = new JPanel();
		contentPane.add(panel, BorderLayout.CENTER);
		panel.setLayout(null);
		
		JPanel panel_2 = new JPanel();
		panel_2.setBounds(0, 0, 261, 131);
		panel.add(panel_2);
		panel_2.setLayout(null);
		
		JLabel lblNewLabel_1 = new JLabel("Space used for program (default: 10 GB):");
		lblNewLabel_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel_1.setBounds(0, 30, 261, 14);
		panel_2.add(lblNewLabel_1);
		
		JPanel panel_3 = new JPanel();
		panel_3.setBounds(261, 0, 261, 131);
		panel.add(panel_3);
		panel_3.setLayout(null);
		
		slider = new JSlider();
		slider.setMinorTickSpacing(5);
		slider.setMajorTickSpacing(25);
		slider.setPaintTicks(true);
		slider.setBounds(10, 5, 225, 42);
		slider.addChangeListener(new CL1());
		panel_3.add(slider);
		
		
		JLabel lblGb = new JLabel("0.1 GB");
		lblGb.setBounds(10, 44, 42, 14);
		panel_3.add(lblGb);
		
		JLabel lblGb_1 = new JLabel("1 GB");
		lblGb_1.setBounds(62, 44, 42, 14);
		panel_3.add(lblGb_1);
		
		JLabel lblGb_2 = new JLabel("10 GB");
		lblGb_2.setBounds(114, 44, 38, 14);
		panel_3.add(lblGb_2);
		
		JLabel lblGb_3 = new JLabel("100 GB");
		lblGb_3.setBounds(162, 44, 50, 14);
		panel_3.add(lblGb_3);
		
		JLabel lblGb_4 = new JLabel("1000 GB");
		lblGb_4.setBounds(211, 44, 50, 14);
		panel_3.add(lblGb_4);
		
		spinner = new JSpinner();
		spinner.setModel(new SpinnerNumberModel(new Double(10), new Double(0), null, new Double(1)));
		spinner.setBounds(94, 69, 63, 20);
		spinner.addChangeListener(new CL2());
		      
		panel_3.add(spinner);
		
		JButton btnSubmit = new JButton("Submit");
		btnSubmit.setBounds(211, 150, 115, 23);
		btnSubmit.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent e)
		    {
		    	Settings sett = new Settings();
		    	ObjectOutputStream oos=null;
		    	sett.allocSpace = (double) MainWindow.this.spinner.getValue();
		       try {
				FileOutputStream fout = new FileOutputStream("settings.fwf");
				oos = new ObjectOutputStream(fout);
				oos.writeObject(sett);
				Utilities.updateSpace(Math.round(sett.allocSpace*Math.pow(2,30)));
			} catch (Exception e1) {
				new ErrorDialog(MainWindow.this,"Error in saving settings!");
				e1.printStackTrace();
			}
		       finally{
		    	   try {
					oos.close();
				} catch (IOException e1) {
				}
		       }
		    }
		});
		panel.add(btnSubmit);
		
		addWindowStateListener(new WindowStateListener() {
			   public void windowStateChanged(WindowEvent e) {
				   if (OSValidator.isWindows())
				   if ((e.getNewState() & Frame.ICONIFIED) == Frame.ICONIFIED){
					      MainWindow.this.setVisible(false);
					   }
			   }
			});
		setResizable(false);
		
	}
	
	class CL1 implements ChangeListener
	{
		public void stateChanged(ChangeEvent event) {
			l2=false;
			if (l1){
	          int value = slider.getValue();
	          
	          double val = Math.pow( (double)10, (((double)value)-25)/25 );
	          
	          int stepen = (int)Math.log10(val);
	          
	          double f = (double)Math.round(val/Math.pow(10,(stepen-2)));
	          
	          val = f*Math.pow(10,(stepen-2));
	          
	          //System.out.println(val+"s"+val);
	          
	          MainWindow.this.spinner.setValue(val);
	          //System.out.println(value + " " + Math.pow( (double)10, (((double)value)-25)/25 ) );
	        }
			l2 = true;
			}
	}
	
	class CL2 implements ChangeListener
	{
		public void stateChanged(ChangeEvent event) {
			l1=false;
			if (l2){
	          double value = (double)spinner.getValue();
	          double slide = (double)Math.log10(value)*25+25;
	          int s = Math.round((float)slide);
	          if (s>100) s=100;
	          if (s<0) s=0;
	          MainWindow.this.slider.setValue(s);
	          //System.out.println(value + " " + s );
	        }
			l1 = true;
		}
	}
}
