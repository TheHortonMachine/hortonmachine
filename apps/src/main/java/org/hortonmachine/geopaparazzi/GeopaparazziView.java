package org.hortonmachine.geopaparazzi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.border.TitledBorder;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class GeopaparazziView extends JPanel
{
   JLabel _projectsFolderLabel = new JLabel();
   JTextField _projectsFolderTextfield = new JTextField();
   JButton _projectsFolderBrowseButton = new JButton();
   JLabel _filterLabel = new JLabel();
   JTextField _filterTextfield = new JTextField();
   JPanel _chartHolderFrame = new JPanel();
   JPanel _chartHolder = new JPanel();
   JButton _loadFolderButton = new JButton();
   JPanel _nwwHolderFrame = new JPanel();
   JPanel _nwwHolder = new JPanel();
   JCheckBox _useGpsElevationsCheckbox = new JCheckBox();
   JPanel _leftPanel = new JPanel();
   JPanel _databaseTreeView = new JPanel();
   JTree _databaseTree = new JTree();
   JScrollPane _infoScroll = new JScrollPane();

   /**
    * Default constructor
    */
   public GeopaparazziView()
   {
      initializePanel();
   }

   /**
    * Adds fill components to empty cells in the first row and first column of the grid.
    * This ensures that the grid spacing will be the same as shown in the designer.
    * @param cols an array of column indices in the first row where fill components should be added.
    * @param rows an array of row indices in the first column where fill components should be added.
    */
   void addFillComponents( Container panel, int[] cols, int[] rows )
   {
      Dimension filler = new Dimension(10,10);

      boolean filled_cell_11 = false;
      CellConstraints cc = new CellConstraints();
      if ( cols.length > 0 && rows.length > 0 )
      {
         if ( cols[0] == 1 && rows[0] == 1 )
         {
            /** add a rigid area  */
            panel.add( Box.createRigidArea( filler ), cc.xy(1,1) );
            filled_cell_11 = true;
         }
      }

      for( int index = 0; index < cols.length; index++ )
      {
         if ( cols[index] == 1 && filled_cell_11 )
         {
            continue;
         }
         panel.add( Box.createRigidArea( filler ), cc.xy(cols[index],1) );
      }

      for( int index = 0; index < rows.length; index++ )
      {
         if ( rows[index] == 1 && filled_cell_11 )
         {
            continue;
         }
         panel.add( Box.createRigidArea( filler ), cc.xy(1,rows[index]) );
      }

   }

   /**
    * Helper method to load an image file from the CLASSPATH
    * @param imageName the package and name of the file to load relative to the CLASSPATH
    * @return an ImageIcon instance with the specified image file
    * @throws IllegalArgumentException if the image resource cannot be loaded.
    */
   public ImageIcon loadImage( String imageName )
   {
      try
      {
         ClassLoader classloader = getClass().getClassLoader();
         java.net.URL url = classloader.getResource( imageName );
         if ( url != null )
         {
            ImageIcon icon = new ImageIcon( url );
            return icon;
         }
      }
      catch( Exception e )
      {
         e.printStackTrace();
      }
      throw new IllegalArgumentException( "Unable to load image: " + imageName );
   }

   /**
    * Method for recalculating the component orientation for 
    * right-to-left Locales.
    * @param orientation the component orientation to be applied
    */
   public void applyComponentOrientation( ComponentOrientation orientation )
   {
      // Not yet implemented...
      // I18NUtils.applyComponentOrientation(this, orientation);
      super.applyComponentOrientation(orientation);
   }

   public JPanel createPanel()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:GROW(0.4),FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(0.6),FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,FILL:DEFAULT:GROW(1.0),CENTER:2DLU:NONE,FILL:DEFAULT:GROW(0.5),FILL:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _projectsFolderLabel.setName("projectsFolderLabel");
      _projectsFolderLabel.setText("Projects folder");
      jpanel1.add(_projectsFolderLabel,cc.xy(2,2));

      _projectsFolderTextfield.setName("projectsFolderTextfield");
      jpanel1.add(_projectsFolderTextfield,cc.xywh(5,2,3,1));

      _projectsFolderBrowseButton.setActionCommand("...");
      _projectsFolderBrowseButton.setName("projectsFolderBrowseButton");
      _projectsFolderBrowseButton.setText("...");
      jpanel1.add(_projectsFolderBrowseButton,cc.xy(9,2));

      _filterLabel.setName("filterLabel");
      _filterLabel.setText("Filter projects");
      jpanel1.add(_filterLabel,cc.xy(2,4));

      _filterTextfield.setName("filterTextfield");
      jpanel1.add(_filterTextfield,cc.xy(5,4));

      jpanel1.add(createchartHolderFrame(),cc.xywh(7,8,5,1));
      _loadFolderButton.setActionCommand("Load");
      _loadFolderButton.setName("loadFolderButton");
      _loadFolderButton.setText("Load");
      jpanel1.add(_loadFolderButton,cc.xy(11,2));

      jpanel1.add(createnwwHolderFrame(),cc.xywh(7,6,5,1));
      _useGpsElevationsCheckbox.setActionCommand("use GPS elevations for logs");
      _useGpsElevationsCheckbox.setName("useGpsElevationsCheckbox");
      _useGpsElevationsCheckbox.setText("use GPS elevations for logs");
      jpanel1.add(_useGpsElevationsCheckbox,cc.xywh(7,4,5,1));

      jpanel1.add(createleftPanel(),cc.xywh(2,6,4,3));
      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12 },new int[]{ 1,2,3,4,5,6,7,8,9 });
      return jpanel1;
   }

   public JPanel createchartHolderFrame()
   {
      _chartHolderFrame.setName("chartHolderFrame");
      TitledBorder titledborder1 = new TitledBorder(null,"Charts",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
      _chartHolderFrame.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","CENTER:DEFAULT:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      _chartHolderFrame.setLayout(formlayout1);

      _chartHolder.setName("chartHolder");
      _chartHolderFrame.add(_chartHolder,cc.xy(1,1));

      addFillComponents(_chartHolderFrame,new int[0],new int[0]);
      return _chartHolderFrame;
   }

   public JPanel createnwwHolderFrame()
   {
      _nwwHolderFrame.setName("nwwHolderFrame");
      TitledBorder titledborder1 = new TitledBorder(null,"Viewer",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
      _nwwHolderFrame.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","FILL:DEFAULT:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      _nwwHolderFrame.setLayout(formlayout1);

      _nwwHolder.setName("nwwHolder");
      _nwwHolderFrame.add(_nwwHolder,cc.xy(1,1));

      addFillComponents(_nwwHolderFrame,new int[0],new int[0]);
      return _nwwHolderFrame;
   }

   public JPanel createleftPanel()
   {
      _leftPanel.setName("leftPanel");
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","CENTER:DEFAULT:GROW(0.6),CENTER:DEFAULT:GROW(0.4)");
      CellConstraints cc = new CellConstraints();
      _leftPanel.setLayout(formlayout1);

      _leftPanel.add(createdatabaseTreeView(),new CellConstraints(1,1,1,1,CellConstraints.FILL,CellConstraints.FILL));
      _leftPanel.add(createPanel1(),new CellConstraints(1,2,1,1,CellConstraints.FILL,CellConstraints.FILL));
      addFillComponents(_leftPanel,new int[]{ 1 },new int[]{ 1,2 });
      return _leftPanel;
   }

   public JPanel createdatabaseTreeView()
   {
      _databaseTreeView.setName("databaseTreeView");
      TitledBorder titledborder1 = new TitledBorder(null,"Projects",TitledBorder.LEFT,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
      _databaseTreeView.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","FILL:DEFAULT:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      _databaseTreeView.setLayout(formlayout1);

      _databaseTree.setName("databaseTree");
      JScrollPane jscrollpane1 = new JScrollPane();
      jscrollpane1.setViewportView(_databaseTree);
      jscrollpane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      jscrollpane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      _databaseTreeView.add(jscrollpane1,new CellConstraints(1,1,1,1,CellConstraints.FILL,CellConstraints.FILL));

      addFillComponents(_databaseTreeView,new int[0],new int[0]);
      return _databaseTreeView;
   }

   public JPanel createPanel1()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Info",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","FILL:DEFAULT:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _infoScroll.setName("infoScroll");
      jpanel1.add(_infoScroll,cc.xy(1,1));

      addFillComponents(jpanel1,new int[0],new int[0]);
      return jpanel1;
   }

   /**
    * Initializer
    */
   protected void initializePanel()
   {
      setLayout(new BorderLayout());
      add(createPanel(), BorderLayout.CENTER);
   }


}
