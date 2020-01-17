package org.hortonmachine.webmaps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class WebMapsView extends JPanel
{
   JLabel _serverNameLabel = new JLabel();
   JLabel _serverTitleLabel = new JLabel();
   JTextField _getCapabilitiesField = new JTextField();
   JButton _loadButton = new JButton();
   JLabel _layerNameLabel = new JLabel();
   JLabel _layerTitleLabel = new JLabel();
   JComboBox _stylesCombo = new JComboBox();
   JComboBox _crsCombo = new JComboBox();
   JLabel _northCrsLabel = new JLabel();
   JLabel _southCrsLabel = new JLabel();
   JLabel _westCrsLabel = new JLabel();
   JLabel _eastCrsLabel = new JLabel();
   JComboBox _formatsCombo = new JComboBox();
   JComboBox _layersCombo = new JComboBox();
   JTextField _layerNameFilterField = new JTextField();
   JTextField _boundsFileField = new JTextField();
   JButton _boundsLoadButton = new JButton();
   JLabel _northCrsFileLabel = new JLabel();
   JLabel _southCrsFileLabel = new JLabel();
   JLabel _westCrsFileLabel = new JLabel();
   JLabel _eastCrsFileLabel = new JLabel();
   JButton _loadPreviewButton = new JButton();
   JTextField _outputWithField = new JTextField();
   JTextField _outputHeightField = new JTextField();
   JTextField _outputFileField = new JTextField();
   JButton _outputSaveButton = new JButton();
   JButton _wms2tiffButton = new JButton();
   JLabel _previewImageLabel = new JLabel();

   /**
    * Default constructor
    */
   public WebMapsView()
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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:12DLU:GROW(1.0),FILL:DEFAULT:NONE,FILL:12DLU:GROW(1.0),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,FILL:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      jpanel1.add(createPanel1(),cc.xy(2,2));
      jpanel1.add(createPanel7(),cc.xy(4,2));
      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5 },new int[]{ 1,2,3 });
      return jpanel1;
   }

   public JPanel createPanel1()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      JLabel jlabel1 = new JLabel();
      jlabel1.setText("Name");
      jpanel1.add(jlabel1,cc.xy(2,3));

      JLabel jlabel2 = new JLabel();
      jlabel2.setText("Title");
      jpanel1.add(jlabel2,cc.xy(2,5));

      _serverNameLabel.setName("serverNameLabel");
      _serverNameLabel.setText("- nv -");
      jpanel1.add(_serverNameLabel,cc.xy(4,3));

      _serverTitleLabel.setName("serverTitleLabel");
      _serverTitleLabel.setText("- nv -");
      jpanel1.add(_serverTitleLabel,cc.xy(4,5));

      jpanel1.add(createPanel2(),cc.xywh(2,1,3,1));
      JLabel jlabel3 = new JLabel();
      jlabel3.setText("Name");
      jpanel1.add(jlabel3,cc.xy(2,9));

      JLabel jlabel4 = new JLabel();
      jlabel4.setText("Title");
      jpanel1.add(jlabel4,cc.xy(2,11));

      _layerNameLabel.setName("layerNameLabel");
      _layerNameLabel.setText("- nv -");
      jpanel1.add(_layerNameLabel,cc.xy(4,9));

      _layerTitleLabel.setName("layerTitleLabel");
      _layerTitleLabel.setText("- nv -");
      jpanel1.add(_layerTitleLabel,cc.xy(4,11));

      _stylesCombo.setName("stylesCombo");
      TitledBorder titledborder1 = new TitledBorder(null,"Styles",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
      _stylesCombo.setBorder(titledborder1);
      jpanel1.add(_stylesCombo,cc.xywh(2,13,3,1));

      _crsCombo.setName("crsCombo");
      TitledBorder titledborder2 = new TitledBorder(null,"CRS",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
      _crsCombo.setBorder(titledborder2);
      jpanel1.add(_crsCombo,cc.xywh(2,17,3,1));

      jpanel1.add(createPanel3(),cc.xywh(2,19,3,1));
      _formatsCombo.setName("formatsCombo");
      TitledBorder titledborder3 = new TitledBorder(null,"Formats",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
      _formatsCombo.setBorder(titledborder3);
      jpanel1.add(_formatsCombo,cc.xywh(2,15,3,1));

      jpanel1.add(createPanel4(),cc.xywh(2,7,3,1));
      jpanel1.add(createPanel5(),cc.xywh(2,21,3,1));
      jpanel1.add(createPanel6(),cc.xywh(2,23,3,1));
      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23 });
      return jpanel1;
   }

   public JPanel createPanel2()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:32DLU:GROW(1.0),FILL:DEFAULT:NONE,FILL:32DLU:NONE","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _getCapabilitiesField.setName("getCapabilitiesField");
      jpanel1.add(_getCapabilitiesField,cc.xy(1,1));

      _loadButton.setActionCommand("load");
      _loadButton.setName("loadButton");
      _loadButton.setText("load");
      jpanel1.add(_loadButton,cc.xy(3,1));

      addFillComponents(jpanel1,new int[]{ 2 },new int[0]);
      return jpanel1;
   }

   public JPanel createPanel3()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Bounds",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(0.5),FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0)","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      JLabel jlabel1 = new JLabel();
      jlabel1.setText("North");
      jpanel1.add(jlabel1,cc.xy(1,1));

      JLabel jlabel2 = new JLabel();
      jlabel2.setText("South");
      jpanel1.add(jlabel2,cc.xy(1,3));

      JLabel jlabel3 = new JLabel();
      jlabel3.setText("West");
      jpanel1.add(jlabel3,cc.xy(1,5));

      JLabel jlabel4 = new JLabel();
      jlabel4.setText("East");
      jpanel1.add(jlabel4,cc.xy(1,7));

      _northCrsLabel.setName("northCrsLabel");
      _northCrsLabel.setText("- nv -");
      jpanel1.add(_northCrsLabel,cc.xy(3,1));

      _southCrsLabel.setName("southCrsLabel");
      _southCrsLabel.setText("- nv -");
      jpanel1.add(_southCrsLabel,cc.xy(3,3));

      _westCrsLabel.setName("westCrsLabel");
      _westCrsLabel.setText("- nv -");
      jpanel1.add(_westCrsLabel,cc.xy(3,5));

      _eastCrsLabel.setName("eastCrsLabel");
      _eastCrsLabel.setText("- nv -");
      jpanel1.add(_eastCrsLabel,cc.xy(3,7));

      addFillComponents(jpanel1,new int[]{ 2 },new int[]{ 2,4,6 });
      return jpanel1;
   }

   public JPanel createPanel4()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Layers & Filter",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:50DLU:GROW(1.0),FILL:DEFAULT:NONE,FILL:50DLU:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _layersCombo.setName("layersCombo");
      jpanel1.add(_layersCombo,cc.xy(1,1));

      _layerNameFilterField.setName("layerNameFilterField");
      jpanel1.add(_layerNameFilterField,cc.xy(3,1));

      addFillComponents(jpanel1,new int[]{ 2,4 },new int[0]);
      return jpanel1;
   }

   public JPanel createPanel5()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:24DLU:NONE","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      JLabel jlabel1 = new JLabel();
      jlabel1.setText("Bounds from file");
      jpanel1.add(jlabel1,cc.xy(1,1));

      _boundsFileField.setName("boundsFileField");
      jpanel1.add(_boundsFileField,cc.xy(3,1));

      _boundsLoadButton.setActionCommand("load");
      _boundsLoadButton.setName("boundsLoadButton");
      _boundsLoadButton.setText("...");
      jpanel1.add(_boundsLoadButton,cc.xy(5,1));

      addFillComponents(jpanel1,new int[]{ 2,4 },new int[0]);
      return jpanel1;
   }

   public JPanel createPanel6()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"File/Export Bounds ",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(0.5),FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0)","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      JLabel jlabel1 = new JLabel();
      jlabel1.setText("North");
      jpanel1.add(jlabel1,cc.xy(1,1));

      JLabel jlabel2 = new JLabel();
      jlabel2.setText("South");
      jpanel1.add(jlabel2,cc.xy(1,3));

      JLabel jlabel3 = new JLabel();
      jlabel3.setText("West");
      jpanel1.add(jlabel3,cc.xy(1,5));

      JLabel jlabel4 = new JLabel();
      jlabel4.setText("East");
      jpanel1.add(jlabel4,cc.xy(1,7));

      _northCrsFileLabel.setName("northCrsFileLabel");
      _northCrsFileLabel.setText("- nv -");
      jpanel1.add(_northCrsFileLabel,cc.xy(3,1));

      _southCrsFileLabel.setName("southCrsFileLabel");
      _southCrsFileLabel.setText("- nv -");
      jpanel1.add(_southCrsFileLabel,cc.xy(3,3));

      _westCrsFileLabel.setName("westCrsFileLabel");
      _westCrsFileLabel.setText("- nv -");
      jpanel1.add(_westCrsFileLabel,cc.xy(3,5));

      _eastCrsFileLabel.setName("eastCrsFileLabel");
      _eastCrsFileLabel.setText("- nv -");
      jpanel1.add(_eastCrsFileLabel,cc.xy(3,7));

      addFillComponents(jpanel1,new int[]{ 2 },new int[]{ 2,4,6 });
      return jpanel1;
   }

   public JPanel createPanel7()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE","FILL:300PX:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,FILL:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:GROW(1.0),CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _loadPreviewButton.setActionCommand("Load Preview");
      _loadPreviewButton.setName("loadPreviewButton");
      _loadPreviewButton.setText("Load Preview");
      jpanel1.add(_loadPreviewButton,cc.xy(2,2));

      jpanel1.add(createPanel8(),cc.xywh(2,4,1,17));
      jpanel1.add(createPanel9(),cc.xy(2,1));
      addFillComponents(jpanel1,new int[]{ 1,2,3 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20 });
      return jpanel1;
   }

   public JPanel createPanel8()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Download Geotiff",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:24DLU:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,FILL:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      JLabel jlabel1 = new JLabel();
      jlabel1.setText("Width (Pixels)");
      jpanel1.add(jlabel1,cc.xy(1,1));

      JLabel jlabel2 = new JLabel();
      jlabel2.setText("Height (Pixels)");
      jpanel1.add(jlabel2,cc.xy(1,3));

      _outputWithField.setName("outputWithField");
      jpanel1.add(_outputWithField,cc.xywh(3,1,3,1));

      _outputHeightField.setName("outputHeightField");
      jpanel1.add(_outputHeightField,cc.xywh(3,3,3,1));

      JLabel jlabel3 = new JLabel();
      jlabel3.setText("Output geotiff file");
      jpanel1.add(jlabel3,cc.xy(1,5));

      _outputFileField.setName("outputFileField");
      jpanel1.add(_outputFileField,cc.xy(3,5));

      _outputSaveButton.setActionCommand("load");
      _outputSaveButton.setName("outputSaveButton");
      _outputSaveButton.setText("...");
      jpanel1.add(_outputSaveButton,cc.xy(5,5));

      _wms2tiffButton.setActionCommand("Load Preview");
      _wms2tiffButton.setName("wms2tiffButton");
      _wms2tiffButton.setText("Convert WMS to Geotiff");
      jpanel1.add(_wms2tiffButton,cc.xywh(1,7,5,1));

      addFillComponents(jpanel1,new int[]{ 2,4,5 },new int[]{ 2,4,6,8 });
      return jpanel1;
   }

   public JPanel createPanel9()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Preview",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("CENTER:DEFAULT:GROW(1.0)","CENTER:DEFAULT:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _previewImageLabel.setName("previewImageLabel");
      jpanel1.add(_previewImageLabel,cc.xy(1,1));

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
