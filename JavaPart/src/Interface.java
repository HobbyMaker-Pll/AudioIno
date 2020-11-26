import java.awt.Component;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import java.awt.Font;
import javax.swing.border.LineBorder;

import java.awt.SystemColor;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.ImageIcon;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.awt.event.ItemEvent;
import javax.swing.SwingConstants;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.DefaultComboBoxModel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Interface {

	private JFrame AudioIno;
	private SqLite DbCon = new SqLite();
	private SoundPlayer AudioPlay = new SoundPlayer();

	private String Path;
	
	private Iterator<String> conPortsAsIterator;
	private SerialCon Arduino = new SerialCon();
	private boolean Connected = false;
	
	Timer timer = new Timer();

	private JTable Controltable;
	private DefaultTableModel Controldtm;

	private JTable Setuptable;
	private DefaultTableModel Setupdtm;

	JComboBox<String> SelectedConfig = new JComboBox<String>();
	JComboBox<String> SongNameComboBox = new JComboBox<String>();
	JComboBox<String> PinComboBox = new JComboBox<String>();
	JComboBox<String> PortcomboBox = new JComboBox<String>();
	JButton btnConnect = new JButton("Open");
	
	
	JLabel lblConfigName = new JLabel("");
	JLabel lblSongname = new JLabel("");
	JLabel lblTime = new JLabel("");
	JLabel lblSongstatus = new JLabel("");
	
	JPanel audioControlPanel = new JPanel();
	JPanel ControlPanel = new JPanel();
	JPanel audioSetupPanel = new JPanel();
	JPanel audioStatusPanel = new JPanel();
	JPanel arduinoConPanel = new JPanel();
	
	JTabbedPane Tab_Panel = new JTabbedPane(JTabbedPane.TOP);

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Interface window = new Interface();
					window.AudioIno.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
	}

	/**
	 * Create the application.
	 */
	public Interface() {

		try {
			DbCon.ConnectDB();
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
		initialize();
		verifyChanges CheckStatus = new verifyChanges();
		CheckStatus.start();
		disableAllComponents();
	}

	private void reloadSelectedConfig() {
		try {
			Iterator<String> NamesAsIterator = DbCon.getTableNames().iterator();
			SelectedConfig.removeAllItems();

			while (NamesAsIterator.hasNext()) {
				SelectedConfig.addItem((String) NamesAsIterator.next());
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void disableComponents(JPanel container) {
		for (Component components : container.getComponents()) {
			components.setEnabled(false);
		}
	}
	private void enableComponents(JPanel container) {
		for (Component components : container.getComponents()) {
			components.setEnabled(true);
		}
	}
	
	private void disableAllComponents() {
		
		disableComponents(audioControlPanel);
		disableComponents(audioSetupPanel);
		disableComponents(ControlPanel);
		disableComponents(audioStatusPanel);
		enableComponents(arduinoConPanel);
		Tab_Panel.setSelectedIndex(0);
		SelectedConfig.removeAllItems();
		Controldtm.setRowCount(0);
		AudioPlay.StopAudio();
		Tab_Panel.setEnabled(false);
		
		lblConfigName.setText("");
		lblSongname.setText("");
		lblSongstatus.setText("");
		lblTime.setText("");
		
		btnConnect.setText("Open");
		Connected = false;
				
	}
	private void enableAllComponents() {
		
		reloadSelectedConfig();
		enableComponents(audioControlPanel);
		enableComponents(audioSetupPanel);
		enableComponents(ControlPanel);
		enableComponents(audioStatusPanel);
		disableComponents(arduinoConPanel);
		btnConnect.setEnabled(true);
		Tab_Panel.setEnabled(true);
		
		btnConnect.setText("Close");
		Connected = true;
	}
	
	public void SetSecurity(int ms) {
		timer = new Timer();
		timer.scheduleAtFixedRate(new Security(), 0, ms);
	}
	public void UnsetSecurity() {
		timer.cancel();
	}
	class Security extends TimerTask {
		public void run() {
				if (Arduino.getTimeOut() == true) {
					Arduino.setTimeout(false);
				} else {
					try {
						Arduino.closePort();
					} catch (Exception e) {
						e.printStackTrace();
					}
					UnsetSecurity();
					disableAllComponents();
					PortcomboBox.removeAllItems();
					Arduino.setTimeout(true);
					JOptionPane.showMessageDialog(null, "Connection lost");
				}
				
		}
	}
	
	class verifyChanges extends Thread {
		public void run() {
			while (true) {
				
				if (Arduino.isLoaded()) {
					lblConfigName.setText((String)SelectedConfig.getSelectedItem());
					Arduino.resetLoaded();
				
				}
				
				if (Arduino.pauseRequest()) {
					AudioPlay.StopAudio();
					lblSongstatus.setText("Paused");
					Arduino.ResetpauseRequest();
				}
				
				if (Arduino.resumeRequest()) {
					AudioPlay.ResumeAudio();
					lblSongstatus.setText("Playing");
					Arduino.ResetresumeRequest();
				}
			
				if (!((Arduino.getPlayPin()).isEmpty())) {
				
					String Table = (String) SelectedConfig.getSelectedItem();
					String Songname = "";
					
					for (int i = 0; i < Controldtm.getRowCount(); i++) {
						
						String tablePin = (String)Controldtm.getValueAt(i, 0);
						String arduinoPin = Arduino.getPlayPin();
						
						if (tablePin.equals(arduinoPin)) {
							Songname = (String)Controldtm.getValueAt(i, 1);
							break;
						}
						
					}
				
					try {
					
						String DirectoryPath = DbCon.getPath(Arduino.getPlayPin(), Table);
						AudioPlay.SetAudio(DirectoryPath);
						lblSongstatus.setText("Playing");
						lblSongname.setText(Songname);
						lblTime.setText(AudioPlay.getClipDuration());
					
					} catch (SQLException | LineUnavailableException | UnsupportedAudioFileException | IOException e) {
						e.printStackTrace();
					}
				
					Arduino.resetPlaypin();
				}
				
				if (AudioPlay.isFinished()) {
					Arduino.sendMessage("FINISH");
					AudioPlay.resetIsFinish();
					
					lblSongname.setText("");
					lblTime.setText("");
					lblSongstatus.setText("");
				}
				
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	/**
	 * Initialize the contents of the frame.
	 */
	@SuppressWarnings("serial")
	private void initialize() {

		/*
		 * ---------------------------- JFRAME CONFIGS ----------------------------
		 */

		AudioIno = new JFrame();
		AudioIno.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (JOptionPane.showConfirmDialog(null, 
			            "Are you sure you want to close this window?", "Close Window?", 
			            JOptionPane.YES_NO_OPTION,
			            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
						try {
							DbCon.DisconnectDB();
							if (Connected) {
								Arduino.sendMessage("END");
								Arduino.closePort();
							}
						} catch (SQLException e1) {
							e1.printStackTrace();
						} catch (Exception e1) {
							e1.printStackTrace();
						}
			            System.exit(0);
			        }
			}
		});
		AudioIno.setTitle("Audio player - Arduino");
		AudioIno.setResizable(false);
		AudioIno.setBounds(100, 100, 914, 422);
		AudioIno.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		AudioIno.getContentPane().setLayout(null);

		/*
		 * ---------------------------- CONNECTION - JPANEL ----------------------------
		 */

		arduinoConPanel.setBounds(12, 33, 212, 151);
		arduinoConPanel.setToolTipText("");
		arduinoConPanel.setBorder(new LineBorder(SystemColor.activeCaption, 3, true));
		AudioIno.getContentPane().add(arduinoConPanel);
		arduinoConPanel.setLayout(null);

		JLabel lblPort = new JLabel("Port:");
		lblPort.setBounds(35, 26, 32, 16);
		arduinoConPanel.add(lblPort);

		JLabel lblBaud = new JLabel("Baud:");
		lblBaud.setBounds(35, 69, 32, 16);
		arduinoConPanel.add(lblBaud);

		PortcomboBox.setBounds(70, 22, 106, 25);
		arduinoConPanel.add(PortcomboBox);

		JComboBox<String> BaudcomboBox = new JComboBox<String>();
		BaudcomboBox.setModel(new DefaultComboBoxModel<String>(new String[] {"9600", "11000"}));
		BaudcomboBox.setBounds(70, 65, 106, 25);
		arduinoConPanel.add(BaudcomboBox);

		JButton btnRefresh = new JButton("");
		btnRefresh.setIcon(new ImageIcon(Interface.class.getResource("/com/sun/javafx/scene/web/skin/Redo_16x16_JFX.png")));
		btnRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				conPortsAsIterator = (Arduino.refreshPorts()).iterator();
				PortcomboBox.removeAllItems();
				while (conPortsAsIterator.hasNext()) {
					PortcomboBox.addItem(conPortsAsIterator.next());
				}
			}
		});
		btnRefresh.setBounds(34, 113, 32, 26);
		arduinoConPanel.add(btnRefresh);

		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				if (!Connected) {
					String ComName = (String) PortcomboBox.getSelectedItem();
					Integer Baud = Integer.parseInt( (String)BaudcomboBox.getSelectedItem() );
					
					if (!(ComName == null)) {
						try {
							
							Arduino.initSerialPort(ComName, Baud);
							Arduino.openPort();
							
							Thread.sleep(3000);
							Arduino.sendMessage("START");
							btnConnect.setEnabled(false);
							Thread.sleep(1000);
							SetSecurity(2000);
							btnConnect.setEnabled(true);
							enableAllComponents();

						
						} catch (Exception e1) {

							e1.printStackTrace();
						}
					}
					
				} else {
					try {
						UnsetSecurity();
						Arduino.sendMessage("END");
						Arduino.closePort();

						disableAllComponents();
						
					} catch (Exception e1) {

						e1.printStackTrace();
					}
				}
				
			}
		});
		btnConnect.setBounds(78, 113, 98, 26);
		arduinoConPanel.add(btnConnect);

		/*
		 * ---------------------- TABBED PANEL -------------------
		 */
		
		audioStatusPanel.setBounds(12, 219, 212, 151);
		audioStatusPanel.setLayout(null);
		audioStatusPanel.setToolTipText("");
		audioStatusPanel.setBorder(new LineBorder(SystemColor.activeCaption, 3, true));
		AudioIno.getContentPane().add(audioStatusPanel);
		
		JLabel lblConfiguration = new JLabel("Configuration:");
		lblConfiguration.setBounds(12, 17, 79, 16);
		audioStatusPanel.add(lblConfiguration);
		
		
		lblConfigName.setBounds(97, 17, 103, 16);
		audioStatusPanel.add(lblConfigName);
		
		JLabel lblSong = new JLabel("Song:");
		lblSong.setBounds(12, 50, 32, 16);
		audioStatusPanel.add(lblSong);
		
		
		lblSongname.setBounds(50, 50, 150, 16);
		audioStatusPanel.add(lblSongname);
		
		JLabel lblDuration = new JLabel("Duration:");
		lblDuration.setBounds(12, 83, 55, 16);
		audioStatusPanel.add(lblDuration);
		
		
		lblTime.setBounds(72, 83, 128, 16);
		audioStatusPanel.add(lblTime);
		
		JLabel lblStatus = new JLabel("Status:");
		lblStatus.setBounds(12, 116, 40, 16);
		audioStatusPanel.add(lblStatus);
		
		lblSongstatus.setBounds(61, 116, 139, 16);
		audioStatusPanel.add(lblSongstatus);

		Tab_Panel.setEnabled(false);
		Tab_Panel.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e1) {
				String title = Tab_Panel.getTitleAt(Tab_Panel.getSelectedIndex());

				if (title == "Audio Control") {

					reloadSelectedConfig();

				} else if (title == "Audio Setup") {
					SongNameComboBox.removeAllItems();
					PinComboBox.removeAllItems();
					Setupdtm.setRowCount(0);
					
					int choice = JOptionPane.showConfirmDialog(null, "Create a new Audio Configuration?", "Selector",
							JOptionPane.YES_NO_OPTION);

					if (choice == JOptionPane.YES_OPTION) {

						JFileChooser chooser = new JFileChooser();
						chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
						int returnVal = chooser.showOpenDialog(null);

						if (returnVal == JFileChooser.APPROVE_OPTION) {
							Path = chooser.getSelectedFile().getAbsolutePath();
							File Directory = chooser.getSelectedFile();
							String[] Files = Directory.list(new FilenameFilter() {
								@Override
								public boolean accept(File dir, String name) {
									return name.endsWith(".wav");
								}
							});

							
							for (String File : Files) {
								SongNameComboBox.addItem(File);
							}
							
							for (Integer i = 2; i <= 13; i++) {
								PinComboBox.addItem(i.toString());
							}

						} else {
							Tab_Panel.setSelectedIndex(0);
						}

					} else {
						Tab_Panel.setSelectedIndex(0);
					}

				}
			}
		});

		Tab_Panel.setBounds(236, 12, 661, 358);
		Tab_Panel.setFont(new Font("Dialog", Font.BOLD, 15));
		AudioIno.getContentPane().add(Tab_Panel);

		/*
		 * ---------------------- AUDIO CONTROL - TABBED PANEL -------------------
		 */

		Tab_Panel.addTab("Audio Control", null, audioControlPanel, null);
		audioControlPanel.setLayout(null);

		JScrollPane AudioScrollPane = new JScrollPane();
		AudioScrollPane.setEnabled(false);
		AudioScrollPane.setBounds(12, 12, 445, 302);
		audioControlPanel.add(AudioScrollPane);

		Controltable = new JTable();
		Controltable.setRowSelectionAllowed(false);
		Controldtm = new DefaultTableModel(new Object[][] {}, new String[] { "Pin", "Song Name" }) {
			@SuppressWarnings("rawtypes")
			Class[] columnTypes = new Class[] { String.class, String.class };

			public Class<?> getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}

			boolean[] columnEditables = new boolean[] { false, false };

			public boolean isCellEditable(int row, int column) {
				return columnEditables[column];
			}
		};
		Controltable.setModel(Controldtm);
		Controltable.getColumnModel().getColumn(0).setResizable(false);
		Controltable.getColumnModel().getColumn(1).setResizable(false);
		Controltable.getTableHeader().setReorderingAllowed(false);
		AudioScrollPane.setViewportView(Controltable);
		Controltable.getTableHeader().setReorderingAllowed(false);

		
		ControlPanel.setBounds(474, 79, 170, 174);
		audioControlPanel.add(ControlPanel);

		/*
		 * ---------------------------- CONTROL - JPANEL ----------------------------
		 */
		ControlPanel.setLayout(null);

		JButton btnStop = new JButton("");
		btnStop.setEnabled(false);
		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Arduino.sendMessage("PAUSE");
				AudioPlay.StopAudio();
				lblSongstatus.setText("Paused");
			}
		});
		btnStop.setBounds(5, 139, 50, 26);
		ControlPanel.add(btnStop);
		btnStop.setBackground(SystemColor.activeCaption);
		btnStop.setIcon(
				new ImageIcon(Interface.class.getResource("/com/sun/javafx/webkit/prism/resources/mediaPause.png")));

		JButton btnStart = new JButton("");
		btnStart.setEnabled(false);
		btnStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Arduino.sendMessage("RESUME");
				AudioPlay.ResumeAudio();
				lblSongstatus.setText("Playing");
			}
		});
		btnStart.setBounds(60, 139, 50, 26);
		btnStart.setBackground(SystemColor.activeCaption);
		btnStart.setIcon(
				new ImageIcon(Interface.class.getResource("/com/sun/javafx/webkit/prism/resources/mediaPlay.png")));
		ControlPanel.add(btnStart);

		SelectedConfig.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e1) {
				Controldtm.setRowCount(0);
				if (e1.getStateChange() == ItemEvent.SELECTED) {
					try {
						String SelectedItem = e1.getItem().toString();
						System.out.println(SelectedItem);
						int Arraysize = DbCon.getTablesize(SelectedItem);
						for (int i = 1; i <= Arraysize; i++) {
							Iterator<String> RowAsIterator = DbCon.getRow(SelectedItem, i).iterator();
							String[] Split;
							String row = "";
							while (RowAsIterator.hasNext()) {
								row += RowAsIterator.next() + ",";
							}
							Split = row.split(",");
							Controldtm.addRow(new Object[] { Split[0], Split[1] });
						}

					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
		});
		SelectedConfig.setBounds(5, 35, 160, 25);
		ControlPanel.add(SelectedConfig);

		JButton btnSelect = new JButton("");
		btnSelect.setEnabled(false);
		btnSelect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				String PinNumber =  (String) Controldtm.getValueAt(Controltable.getSelectedRow(), 0);
				String Songname =  (String) Controldtm.getValueAt(Controltable.getSelectedRow(), 1);
				String Table = (String) SelectedConfig.getSelectedItem();
				
				try {
					String DirectoryPath = DbCon.getPath(PinNumber, Table);
					AudioPlay.SetAudio(DirectoryPath);
					lblSongname.setText(Songname);
					lblTime.setText(AudioPlay.getClipDuration());
					lblSongstatus.setText("Playing");
					Arduino.sendMessage("FORCE"+ (String) PinNumber);
					
				} catch (SQLException | LineUnavailableException | UnsupportedAudioFileException | IOException  fuck) {
					fuck.printStackTrace();
					JOptionPane.showConfirmDialog(null, "Failed to read database");
				}
				
			}
			
		});
		btnSelect.setBounds(115, 139, 50, 26);
		btnSelect.setBackground(SystemColor.activeCaption);
		btnSelect.setIcon(new ImageIcon(
				Interface.class.getResource("/com/sun/javafx/webkit/prism/resources/mediaVolumeThumb.png")));
		ControlPanel.add(btnSelect);

		JLabel lblSelectAudioSetup = new JLabel("Select Audio Setup");
		lblSelectAudioSetup.setEnabled(false);
		lblSelectAudioSetup.setBounds(32, 12, 107, 16);
		ControlPanel.add(lblSelectAudioSetup);

		JButton btnLoad = new JButton("Load");
		btnLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Arduino.sendMessage("CONFIG");
					Thread.sleep(100);
					for (int i = 0; i < Controldtm.getRowCount(); i++) {
						String pin = (String) Controldtm.getValueAt(i, 0);
						Arduino.sendMessage("DI" + pin);
						Thread.sleep(10);
					}
					
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				
				Arduino.sendMessage("SENT");
				
			}
				
		});
		btnLoad.setEnabled(false);
		btnLoad.setBounds(36, 72, 98, 26);
		ControlPanel.add(btnLoad);
		
		JButton btnDelete = new JButton("Delete");
		btnDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					String TableSelected = (String) SelectedConfig.getSelectedItem();
					if (TableSelected != null) {
						
						DbCon.DeleteTable((String) (SelectedConfig.getSelectedItem()));
						reloadSelectedConfig();
					}
					
				} catch (SQLException e) {
					
					e.printStackTrace();
				}
			}
		});
		btnDelete.setEnabled(false);
		btnDelete.setBounds(36, 101, 98, 26);
		ControlPanel.add(btnDelete);

		/*
		 * --------------------- AUDIO SETUP - TABBED PANEL --------------------
		 */

		audioSetupPanel.setBorder(new LineBorder(SystemColor.activeCaption, 1, true));
		Tab_Panel.addTab("Audio Setup", null, audioSetupPanel, null);
		audioSetupPanel.setLayout(null);

		JScrollPane AudioSetupScrollPane = new JScrollPane();
		AudioSetupScrollPane.setBounds(12, 12, 445, 302);
		audioSetupPanel.add(AudioSetupScrollPane);

		Setuptable = new JTable();
		Setuptable.setRowSelectionAllowed(false);
		Setupdtm = new DefaultTableModel(new Object[][] {}, new String[] { "Pin", "Song Name" }) {
			@SuppressWarnings("rawtypes")
			Class[] columnTypes = new Class[] { String.class, String.class };

			public Class<?> getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}

			boolean[] columnEditables = new boolean[] { false, false };

			public boolean isCellEditable(int row, int column) {
				return columnEditables[column];
			}
		};
		Setuptable.setModel(Setupdtm);
		Setuptable.getColumnModel().getColumn(0).setResizable(false);
		Setuptable.getColumnModel().getColumn(1).setResizable(false);
		Setuptable.getTableHeader().setReorderingAllowed(false);
		AudioSetupScrollPane.setViewportView(Setuptable);

		/*
		 * --------------------- SETUP - JPANEL --------------------
		 */
		JPanel panel = new JPanel();
		panel.setBounds(459, 59, 185, 210);
		audioSetupPanel.add(panel);
		panel.setLayout(null);

		JButton btnAdd = new JButton("Add");
		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String Song = (String) SongNameComboBox.getSelectedItem();
				String Pin = (String) PinComboBox.getSelectedItem();
				if (SongNameComboBox.getSelectedIndex() > -1 && PinComboBox.getSelectedIndex() > -1) {
					Setupdtm.addRow(new Object[] { Pin, Song });

					SongNameComboBox.removeItemAt(SongNameComboBox.getSelectedIndex());
					PinComboBox.removeItemAt(PinComboBox.getSelectedIndex());
				} else {
					JOptionPane.showMessageDialog(null, "Select a valid item to add");
				}
			}
		});
		btnAdd.setBounds(35, 123, 56, 26);
		panel.add(btnAdd);

		JButton btnRemove = new JButton("Del");
		btnRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				int selectedRow = Setuptable.getSelectedRow();
				String removed = "";
				if (selectedRow > -1) {
					for (int i = 0; i < Setuptable.getColumnCount(); i++) {
						removed += Setuptable.getValueAt(Setuptable.getSelectedRow(), i) + ",";

					}
					String[] repose = removed.split(",");
					PinComboBox.addItem(repose[0]);
					SongNameComboBox.addItem(repose[1]);
					Setupdtm.removeRow(selectedRow);
				} else {
					JOptionPane.showMessageDialog(null, "Select before removing");
				}

			}
		});
		btnRemove.setBounds(103, 123, 56, 26);
		panel.add(btnRemove);

		SongNameComboBox.setBounds(12, 31, 161, 26);
		panel.add(SongNameComboBox);

		PinComboBox.setBounds(12, 85, 161, 26);
		panel.add(PinComboBox);

		JLabel lblSongName = new JLabel("Song Name:");
		lblSongName.setHorizontalAlignment(SwingConstants.LEFT);
		lblSongName.setBounds(12, 12, 68, 16);
		panel.add(lblSongName);

		JLabel lblPin = new JLabel("Pin:");
		lblPin.setBounds(12, 69, 21, 16);
		panel.add(lblPin);

		JButton btnCreateConfig = new JButton("Creat Config");
		btnCreateConfig.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String UserNameTable = JOptionPane.showInputDialog("Select a name for the configuration");
				if (!(UserNameTable == null) && !(UserNameTable.isEmpty())) {
					try {
						DbCon.CreateTable(UserNameTable);

						for (Object row : Setupdtm.getDataVector()) {

							Vector<?> rowVector = (Vector<?>) row;
							Iterator<?> rowAsIterator = rowVector.iterator();
							String TableRowData = "";

							while (rowAsIterator.hasNext()) {
								TableRowData += (String) rowAsIterator.next() + ",";
							}
							String[] SplitedRowData = TableRowData.split(",");

							DbCon.addSong(UserNameTable, SplitedRowData[0], SplitedRowData[1],
									Path + "\\" + SplitedRowData[1]);

						}
						reloadSelectedConfig();
						Tab_Panel.setSelectedIndex(0);

					} catch (Exception e1) {
						e1.printStackTrace();
					}
				} else {
					JOptionPane.showMessageDialog(null, "Invalid name");
				}
			}
		});
		btnCreateConfig.setBounds(12, 172, 161, 26);
		panel.add(btnCreateConfig);

		JLabel lblConnection = new JLabel("Connection");
		lblConnection.setFont(new Font("Dialog", Font.BOLD, 15));
		lblConnection.setBounds(77, 12, 82, 16);
		AudioIno.getContentPane().add(lblConnection);

		/*
		 * --------------------- AUDIO STATUS - JPANEL --------------------
		 */

		JLabel lblArduinoStatus = new JLabel("Audio status");
		lblArduinoStatus.setEnabled(false);
		lblArduinoStatus.setFont(new Font("Dialog", Font.BOLD, 15));
		lblArduinoStatus.setBounds(69, 196, 98, 16);
		AudioIno.getContentPane().add(lblArduinoStatus);
	}
}
