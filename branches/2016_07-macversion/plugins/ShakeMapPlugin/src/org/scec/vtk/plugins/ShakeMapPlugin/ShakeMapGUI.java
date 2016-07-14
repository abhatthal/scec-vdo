package org.scec.vtk.plugins.ShakeMapPlugin;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.zip.ZipInputStream;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.scec.vtk.drawingTools.DefaultLocationsGUI.PresetLocationGroup;
import org.scec.vtk.main.Info;
import org.scec.vtk.plugins.PluginActors;
import org.scec.vtk.plugins.ShakeMapPlugin.Component.ShakeMap;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import vtk.vtkActor;


/*
 * This class reads the .xyz files from USGS.
 * Whenever you donwload a new .xyz file
 * 	1. Remove the header
 * 	2. Convert it to a .txt file
 * 	3. Move it to the data/ShakeMapPlugin directory
 * The files in the data/ShakeMapPlugin directory will automatically 
 * be loaded when the ShakeMap plugin is opened in the program.
 */
public class ShakeMapGUI extends JPanel implements ItemListener{


	private static final long serialVersionUID = 1L;
	
	static final String dataPath = Info.getMainGUI().getCWD()+File.separator+"data/ShakeMapPlugin"; //path to directory with local folders
	static final String moreMaps = "More_USGS_Maps"; //the extra maps that the user may download
	static final String openSHAFile = "openSHA.txt";
	static final String openSHAMapURL = "http://zero.usc.edu/gmtData/1468263306257/map_data.txt"; //custom shakemaps which may be uploaded to this link

	private JPanel shakeMapLibraryPanel;
	private JTabbedPane tabbedPane = new JTabbedPane();
	
	ArrayList<JCheckBox> defaultList; //for the local files in ShakeMapPlugin directory
	ArrayList<ShakeMap> shakeMapsList;
	PluginActors pluginActors;
	private ArrayList<vtkActor> actorList;
	
	//for the usgs download option
	private ButtonGroup calChooser = new ButtonGroup();
	private JRadioButton nc = new JRadioButton("NorCal");
	private JRadioButton sc = new JRadioButton("SoCal");
	JTextField eventIdBox = new JTextField("Insert Earthquake ID");
	JButton downloadUSGSButton = new JButton("Download USGS Shake Map");
	
	
	public ShakeMapGUI(PluginActors pluginActors) {
		shakeMapLibraryPanel = new JPanel();
//		shakeMapLibraryPanel.setLayout(new BoxLayout(shakeMapLibraryPanel, BoxLayout.Y_AXIS));
//		shakeMapLibraryPanel.setLayout(new GridLayout(3,0));

		defaultList = new ArrayList<JCheckBox>();
		this.pluginActors = pluginActors;
		shakeMapsList = new ArrayList<ShakeMap>();
		actorList = new ArrayList<>();
		
		//Make checkboxes of all the presets
		JPanel presets = new JPanel();
		presets.setLayout(new GridLayout(0,2)); //2 per row
		//Initialize all the preset files in the data/ShakeMapPlugin directory
		File dataDirectory = new File(dataPath);
		if (dataDirectory.isDirectory()) {
			// List files in the directory and process each
			File files[] = dataDirectory.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isFile()) {
					String tempName = files[i].getName();
					JCheckBox tempCheckbox = new JCheckBox(tempName);
					tempCheckbox.setName(tempName);
					tempCheckbox.addItemListener(this);
					presets.add(tempCheckbox);
					defaultList.add(tempCheckbox); //add the JCheckBox to the list
					shakeMapsList.add(null); //for now, initialize to null
					actorList.add(null);
				}
			}
		}
		
		//Checkboxes for the USGS Table
//		JPanel usgsDownloads = new JPanel();
//		usgsDownloads.setLayout(new GridLayout(0,2)); //2 per row
//		//Initialize all the preset files in the data/ShakeMapPlugin directory
//		File usgsDirectory = new File(dataPath + "/" + moreMaps);
//		if (usgsDirectory.isDirectory()) {
//			// List files in the directory and process each
//			File files[] = usgsDirectory.listFiles();
//			for (int i = 0; i < files.length; i++) {
//				if (files[i].isFile()) {
//					String tempName = files[i].getName();
//					System.out.println(tempName);
//					JCheckBox tempCheckbox = new JCheckBox(tempName);
//					tempCheckbox.setName(tempName);
//					tempCheckbox.addItemListener(this);
//					usgsDownloads.add(tempCheckbox);
//					defaultList.add(tempCheckbox); //add the JCheckBox to the list
//					shakeMapsList.add(null); //for now, initialize to null
//					actorList.add(null);
//				}
//			}
//		}
		
		
		JPanel USGSPanel = new JPanel();
		USGSPanel.setLayout(new GridLayout(0,2));
//		USGSPanel.add(usgsDownloads);
//		USGSPanel.add(new JLabel("USGS Map"));
		calChooser.add(nc);
		calChooser.add(sc);
		USGSPanel.add(nc);
		USGSPanel.add(sc);
		USGSPanel.add(eventIdBox);
		USGSPanel.add(downloadUSGSButton);
		
		
		downloadUSGSButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				String id = eventIdBox.getText();
				if(id.length() > 0){
					String network = "";
					if(nc.isSelected()){
						network = "nc";
					}else if(sc.isSelected()){
						network = "sc";
					}
					if(network.length() > 0){
						USGSShakeMapDownloader smd = new USGSShakeMapDownloader(network, id);
						String d = smd.downloadShakeMap("usgsMap.txt");
						System.out.println(d);
						if(d.length() <= 0){
							System.out.println("Failure");
							JOptionPane.showMessageDialog(shakeMapLibraryPanel,
								    "Sorry, file not found on USGS site.");
						}else{
							System.out.println("Loaded!");
							try {
								Files.copy(Paths.get(dataPath+"/"+"usgsMap.txt"), Paths.get(dataPath+"/"+moreMaps +"/"+id+".txt"));
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							showNewUSGSMap();
						}
					}else{
						System.out.println("Make a location selection!");
						JOptionPane.showMessageDialog(shakeMapLibraryPanel,
							    "Select A Region (Northern or Southern California");
					}
				}else{
					System.out.println("Enter an earthquake ID!");
					JOptionPane.showMessageDialog(shakeMapLibraryPanel,
						    "Enter an earthquake ID.");
				}
			}			
		});
		
		tabbedPane.addTab("Presets", presets);
		tabbedPane.addTab("USGS", USGSPanel);
		JButton openSHAButton = new JButton("Download OpenSHA File");
		openSHAButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				try {
					URL openSHA = new URL(openSHAMapURL);
					Files.copy(openSHA.openStream(), Paths.get(dataPath+"/openSHA.txt"), StandardCopyOption.REPLACE_EXISTING);
				} catch (MalformedURLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					JOptionPane.showMessageDialog(shakeMapLibraryPanel,
						    "There is no file on OpenSHA right now.");
				}
			}		
		});
		tabbedPane.addTab("OpenSHA", openSHAButton);
		
		shakeMapLibraryPanel.add(tabbedPane);
		this.add(shakeMapLibraryPanel);

	}



	public JPanel getPanel() {
		// TODO Auto-generated method stub
		return this;
	}

	/*
	 * This immediately shows the map after downloading it
	 * from the USGS website
	 */
	private void showNewUSGSMap(){
		ShakeMap shakeMap = new ShakeMap(Info.getMainGUI().getCWD()+File.separator+"data/ShakeMapPlugin/Extra/colors.cpt");
		shakeMap.loadFromFileToGriddedGeoDataSet(dataPath + "/" + "usgsMap.txt");
		shakeMap.setActor(shakeMap.builtPolygonSurface());
		actorList.add(shakeMap.getActor());
		pluginActors.addActor(shakeMap.getActor());
		shakeMapsList.add(shakeMap);
		Info.getMainGUI().updateRenderWindow();
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		// TODO Auto-generated method stub
		Object src = e.getSource();
		for(int i=0; i<defaultList.size(); i++){
			if(src == defaultList.get(i)){
				if (e.getStateChange()==ItemEvent.SELECTED) {
					if(shakeMapsList.get(i) == null){ //if it has never been selected before, load the file
						ShakeMap shakeMap = new ShakeMap(Info.getMainGUI().getCWD()+File.separator+"data/ShakeMapPlugin/Extra/colors.cpt");
						if(defaultList.get(i).getName().equals("openSHA.txt")){
							//The file format for data from openSHA files are a little different.
							//Open declaration of method for more details
							shakeMap.loadOpenSHAFileToGriddedGeoDataSet(dataPath + "/" + defaultList.get(i).getName());
						}else{
							shakeMap.loadFromFileToGriddedGeoDataSet(dataPath + "/" + defaultList.get(i).getName());
						}
						shakeMap.setActor(shakeMap.builtPolygonSurface());
						actorList.set(i, shakeMap.getActor());
						pluginActors.addActor(shakeMap.getActor());
						shakeMapsList.set(i, shakeMap);
					}else
						shakeMapsList.get(i).getActor().SetVisibility(1);
				}else
					shakeMapsList.get(i).getActor().SetVisibility(0);
				Info.getMainGUI().updateRenderWindow();
			}
		}
	}

	public void unloadPlugin()
	{

		for(vtkActor actor:actorList)
		{
			pluginActors.removeActor(actor);
		}
		Info.getMainGUI().updateRenderWindow();
	}

}
