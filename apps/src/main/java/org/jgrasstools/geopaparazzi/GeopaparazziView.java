package org.jgrasstools.geopaparazzi;

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
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.border.TitledBorder;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class GeopaparazziView extends JPanel
{
   JPanel _databaseTreeView = new JPanel();
   JTree _databaseTree = new JTree();
   JLabel _projectsFolderLabel = new JLabel();
   JTextField _projectsFolderTextfield = new JTextField();
   JButton _projectsFolderBrowseButton = new JButton();
   JLabel _filterLabel = new JLabel();
   JTextField _filterTextfield = new JTextField();
   JScrollPane _infoScroll = new JScrollPane();
   JPanel _chartHolder = new JPanel();
   JButton _loadFolderButton = new JButton();
   JToggleButton _httpServerButton = new JToggleButton();
   JPanel _nwwHolder = new JPanel();
   JCheckBox _useGpsElevationsCheckbox = new JCheckBox();

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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:GROW(0.4),FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(0.6),FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,FILL:DEFAULT:GROW(0.5),CENTER:2DLU:NONE,FILL:DEFAULT:GROW(0.5),FILL:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      jpanel1.add(createdatabaseTreeView(),cc.xywh(2,6,3,1));
      _projectsFolderLabel.setName("projectsFolderLabel");
      _projectsFolderLabel.setText("Projects folder");
      jpanel1.add(_projectsFolderLabel,cc.xy(2,2));

      _projectsFolderTextfield.setName("projectsFolderTextfield");
      jpanel1.add(_projectsFolderTextfield,cc.xywh(4,2,3,1));

      _projectsFolderBrowseButton.setActionCommand("...");
      _projectsFolderBrowseButton.setName("projectsFolderBrowseButton");
      _projectsFolderBrowseButton.setText("...");
      jpanel1.add(_projectsFolderBrowseButton,cc.xy(8,2));

      _filterLabel.setName("filterLabel");
      _filterLabel.setText("Filter projects");
      jpanel1.add(_filterLabel,cc.xy(2,4));

      _filterTextfield.setName("filterTextfield");
      jpanel1.add(_filterTextfield,cc.xy(4,4));

      jpanel1.add(createPanel1(),cc.xywh(2,8,3,1));
      jpanel1.add(createPanel2(),cc.xywh(6,8,7,1));
      _loadFolderButton.setActionCommand("Load");
      _loadFolderButton.setName("loadFolderButton");
      _loadFolderButton.setText("Load");
      jpanel1.add(_loadFolderButton,cc.xy(10,2));

      _httpServerButton.setActionCommand("HTTP");
      _httpServerButton.setName("httpServerButton");
      _httpServerButton.setText("HTTP");
      jpanel1.add(_httpServerButton,cc.xy(12,2));

      jpanel1.add(createPanel3(),cc.xywh(6,6,7,1));
      _useGpsElevationsCheckbox.setActionCommand("use GPS elevations for logs");
      _useGpsElevationsCheckbox.setName("useGpsElevationsCheckbox");
      _useGpsElevationsCheckbox.setText("use GPS elevations for logs");
      jpanel1.add(_useGpsElevationsCheckbox,cc.xywh(6,4,7,1));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13 },new int[]{ 1,2,3,4,5,6,7,8,9 });
      return jpanel1;
   }

   public JPanel createdatabaseTreeView()
   {
      _databaseTreeView.setName("databaseTreeView");
      TitledBorder titledborder1 = new TitledBorder(null,"Projects",TitledBorder.LEFT,TitledBorder.DEFAULT_POSITION,null,new Color(49,106,196));
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
      TitledBorder titledborder1 = new TitledBorder(null,"Info",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(49,106,196));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","FILL:DEFAULT:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _infoScroll.setName("infoScroll");
      jpanel1.add(_infoScroll,cc.xy(1,1));

      addFillComponents(jpanel1,new int[0],new int[0]);
      return jpanel1;
   }

   public JPanel createPanel2()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Charts",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(49,106,196));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","CENTER:DEFAULT:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _chartHolder.setName("chartHolder");
      jpanel1.add(_chartHolder,cc.xy(1,1));

      addFillComponents(jpanel1,new int[0],new int[0]);
      return jpanel1;
   }

   public JPanel createPanel3()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Viewer",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(49,106,196));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","FILL:DEFAULT:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _nwwHolder.setName("nwwHolder");
      jpanel1.add(_nwwHolder,cc.xy(1,1));

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
