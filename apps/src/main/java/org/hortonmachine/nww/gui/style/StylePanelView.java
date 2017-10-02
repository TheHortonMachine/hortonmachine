package org.hortonmachine.nww.gui.style;

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


public class StylePanelView extends JPanel
{
   JPanel _fillPanel = new JPanel();
   JLabel _fillColorLabel = new JLabel();
   JButton _fillColorButton = new JButton();
   JLabel _fillOpacityLabel = new JLabel();
   JComboBox _fillOpacityCombo = new JComboBox();
   JPanel _strokelPanel = new JPanel();
   JLabel _strokeColorLabel = new JLabel();
   JButton _strokeColorButton = new JButton();
   JLabel _strokeWidthLabel = new JLabel();
   JTextField _strokeWidthText = new JTextField();
   JPanel _markerPanel = new JPanel();
   JLabel _markerTypeLabel = new JLabel();
   JLabel _markerSizeLabel = new JLabel();
   JComboBox _markerTypeCombo = new JComboBox();
   JTextField _markerSizeText = new JTextField();
   JButton _okButton = new JButton();
   JButton _cancelButton = new JButton();

   /**
    * Default constructor
    */
   public StylePanelView()
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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,FILL:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      jpanel1.add(createfillPanel(),cc.xy(2,2));
      jpanel1.add(createstrokelPanel(),cc.xy(2,4));
      jpanel1.add(createmarkerPanel(),cc.xy(2,6));
      jpanel1.add(createPanel1(),new CellConstraints(2,8,1,1,CellConstraints.CENTER,CellConstraints.CENTER));
      addFillComponents(jpanel1,new int[]{ 1,2,3 },new int[]{ 1,2,3,4,5,6,7,8,9 });
      return jpanel1;
   }

   public JPanel createfillPanel()
   {
      _fillPanel.setName("fillPanel");
      TitledBorder titledborder1 = new TitledBorder(null,"Fill",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(49,106,196));
      _fillPanel.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(0.7),FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(0.3),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      _fillPanel.setLayout(formlayout1);

      _fillColorLabel.setName("fillColorLabel");
      _fillColorLabel.setText("Fill Color");
      _fillPanel.add(_fillColorLabel,cc.xy(2,2));

      _fillColorButton.setActionCommand("...");
      _fillColorButton.setName("fillColorButton");
      _fillColorButton.setText("...");
      _fillPanel.add(_fillColorButton,cc.xy(4,2));

      _fillOpacityLabel.setName("fillOpacityLabel");
      _fillOpacityLabel.setText("Fill Opacity");
      _fillPanel.add(_fillOpacityLabel,cc.xy(2,4));

      _fillOpacityCombo.setName("fillOpacityCombo");
      _fillPanel.add(_fillOpacityCombo,cc.xy(4,4));

      addFillComponents(_fillPanel,new int[]{ 1,2,3,4,5 },new int[]{ 1,2,3,4,5 });
      return _fillPanel;
   }

   public JPanel createstrokelPanel()
   {
      _strokelPanel.setName("strokelPanel");
      TitledBorder titledborder1 = new TitledBorder(null,"Stroke",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(49,106,196));
      _strokelPanel.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(0.7),FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(0.3),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      _strokelPanel.setLayout(formlayout1);

      _strokeColorLabel.setName("strokeColorLabel");
      _strokeColorLabel.setText("Strokel Color");
      _strokelPanel.add(_strokeColorLabel,cc.xy(2,2));

      _strokeColorButton.setActionCommand("...");
      _strokeColorButton.setName("strokeColorButton");
      _strokeColorButton.setText("...");
      _strokelPanel.add(_strokeColorButton,cc.xy(4,2));

      _strokeWidthLabel.setName("strokeWidthLabel");
      _strokeWidthLabel.setText("Stroke Width");
      _strokelPanel.add(_strokeWidthLabel,cc.xy(2,4));

      _strokeWidthText.setName("strokeWidthText");
      _strokelPanel.add(_strokeWidthText,cc.xy(4,4));

      addFillComponents(_strokelPanel,new int[]{ 1,2,3,4,5 },new int[]{ 1,2,3,4,5 });
      return _strokelPanel;
   }

   public JPanel createmarkerPanel()
   {
      _markerPanel.setName("markerPanel");
      TitledBorder titledborder1 = new TitledBorder(null,"Marker",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(49,106,196));
      _markerPanel.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(0.7),FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(0.3),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      _markerPanel.setLayout(formlayout1);

      _markerTypeLabel.setName("markerTypeLabel");
      _markerTypeLabel.setText("Marker Type");
      _markerPanel.add(_markerTypeLabel,cc.xy(2,2));

      _markerSizeLabel.setName("markerSizeLabel");
      _markerSizeLabel.setText("Marker Size");
      _markerPanel.add(_markerSizeLabel,cc.xy(2,4));

      _markerTypeCombo.setName("markerTypeCombo");
      _markerPanel.add(_markerTypeCombo,cc.xy(4,2));

      _markerSizeText.setName("markerSizeText");
      _markerPanel.add(_markerSizeText,cc.xy(4,4));

      addFillComponents(_markerPanel,new int[]{ 1,2,3,4,5 },new int[]{ 1,2,3,4,5 });
      return _markerPanel;
   }

   public JPanel createPanel1()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _okButton.setActionCommand("OK");
      _okButton.setName("okButton");
      _okButton.setText("OK");
      jpanel1.add(_okButton,cc.xy(2,1));

      _cancelButton.setActionCommand("Cancel");
      _cancelButton.setName("cancelButton");
      _cancelButton.setText("Cancel");
      jpanel1.add(_cancelButton,cc.xy(1,1));

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
