package org.jgrasstools.nww.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.TitledBorder;


public class ToolsPanelView extends JPanel
{
   JLabel _loadFileLabel = new JLabel();
   JButton _loadFileButton = new JButton();
   JToggleButton _infoButton = new JToggleButton();
   JToggleButton _selectByBoxButton = new JToggleButton();
   JToggleButton _zoomByBoxButton = new JToggleButton();
   JLabel _globeModeLabel = new JLabel();
   JComboBox _globeModeCombo = new JComboBox();
   JLabel _loadGpsLabel = new JLabel();
   JButton _loadGpsButton = new JButton();
   JLabel _openCacheLabel = new JLabel();
   JButton _openCacheButton = new JButton();

   /**
    * Default constructor
    */
   public ToolsPanelView()
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
      TitledBorder titledborder1 = new TitledBorder(null,"Tools",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(49,106,196));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _loadFileLabel.setName("loadFileLabel");
      _loadFileLabel.setText("Load supported file");
      jpanel1.add(_loadFileLabel,cc.xywh(2,2,13,1));

      _loadFileButton.setActionCommand("...");
      _loadFileButton.setName("loadFileButton");
      _loadFileButton.setText("...");
      jpanel1.add(_loadFileButton,cc.xy(17,2));

      jpanel1.add(createPanel1(),cc.xywh(2,7,16,1));
      _globeModeLabel.setName("globeModeLabel");
      _globeModeLabel.setText("Globe Mode");
      jpanel1.add(_globeModeLabel,cc.xywh(2,4,13,1));

      _globeModeCombo.setName("globeModeCombo");
      jpanel1.add(_globeModeCombo,cc.xywh(16,4,2,1));

      _loadGpsLabel.setName("loadGpsLabel");
      _loadGpsLabel.setText("Load shapefile to fly");
      jpanel1.add(_loadGpsLabel,cc.xywh(2,9,13,1));

      _loadGpsButton.setActionCommand("...");
      _loadGpsButton.setName("loadGpsButton");
      _loadGpsButton.setText("...");
      jpanel1.add(_loadGpsButton,cc.xy(17,9));

      _openCacheLabel.setName("openCacheLabel");
      _openCacheLabel.setText("Open Cache Manager");
      jpanel1.add(_openCacheLabel,cc.xywh(2,11,13,1));

      _openCacheButton.setActionCommand("...");
      _openCacheButton.setName("openCacheButton");
      _openCacheButton.setText("...");
      jpanel1.add(_openCacheButton,cc.xy(17,11));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12 });
      return jpanel1;
   }

   public JPanel createPanel1()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _infoButton.setActionCommand("Info Tool");
      _infoButton.setName("infoButton");
      _infoButton.setText("Info Tool");
      jpanel1.add(_infoButton,cc.xy(1,1));

      _selectByBoxButton.setActionCommand("Select by box");
      _selectByBoxButton.setName("selectByBoxButton");
      _selectByBoxButton.setText("Select by box");
      jpanel1.add(_selectByBoxButton,cc.xy(3,1));

      _zoomByBoxButton.setActionCommand("Zoom By Box");
      _zoomByBoxButton.setName("zoomByBoxButton");
      _zoomByBoxButton.setText("Zoom By Box");
      jpanel1.add(_zoomByBoxButton,cc.xy(1,3));

      addFillComponents(jpanel1,new int[]{ 2,4,5,6,7,8,9,10 },new int[]{ 2 });
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
