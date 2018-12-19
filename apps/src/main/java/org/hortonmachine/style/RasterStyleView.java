package org.hortonmachine.style;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class RasterStyleView extends JPanel
{
   JLabel layerToStyleLabel = new JLabel();
   JComboBox rasterLayerCombo = new JComboBox();
   JLabel selectColorTableLabel = new JLabel();
   JComboBox colortablesCombo = new JComboBox();
   JButton applyTableButton = new JButton();
   JLabel customStyleLabel = new JLabel();
   JTextArea customStyleArea = new JTextArea();
   JButton customStyleButton = new JButton();
   JLabel opacityLabel = new JLabel();
   JComboBox opacityCombo = new JComboBox();
   JLabel numFormatLabel = new JLabel();
   JTextField numFormatField = new JTextField();
   JCheckBox interpolatedCheckbox = new JCheckBox();
   JLabel novalueLabel = new JLabel();
   JTextField novalueTextfield = new JTextField();

   /**
    * Default constructor
    */
   public RasterStyleView()
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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:GROW(0.3),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      layerToStyleLabel.setName("layerToStyleLabel");
      layerToStyleLabel.setText("Layer to style");
      jpanel1.add(layerToStyleLabel,cc.xy(2,2));

      rasterLayerCombo.setName("rasterLayerCombo");
      jpanel1.add(rasterLayerCombo,cc.xywh(4,2,12,1));

      selectColorTableLabel.setName("selectColorTableLabel");
      selectColorTableLabel.setText("Select Colortable");
      jpanel1.add(selectColorTableLabel,cc.xy(2,8));

      colortablesCombo.setName("colortablesCombo");
      jpanel1.add(colortablesCombo,cc.xywh(4,8,10,1));

      applyTableButton.setActionCommand("Apply");
      applyTableButton.setName("applyTableButton");
      applyTableButton.setText("Apply");
      jpanel1.add(applyTableButton,cc.xy(15,8));

      customStyleLabel.setName("customStyleLabel");
      customStyleLabel.setText("Define custom");
      jpanel1.add(customStyleLabel,new CellConstraints(2,10,1,1,CellConstraints.DEFAULT,CellConstraints.CENTER));

      customStyleArea.setName("customStyleArea");
      customStyleArea.setRows(8);
      JScrollPane jscrollpane1 = new JScrollPane();
      jscrollpane1.setViewportView(customStyleArea);
      jscrollpane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      jscrollpane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      jpanel1.add(jscrollpane1,cc.xywh(4,10,10,1));

      customStyleButton.setActionCommand("Apply");
      customStyleButton.setName("customStyleButton");
      customStyleButton.setText("Apply");
      jpanel1.add(customStyleButton,new CellConstraints(15,10,1,1,CellConstraints.DEFAULT,CellConstraints.CENTER));

      opacityLabel.setName("opacityLabel");
      opacityLabel.setText("Opacity");
      opacityLabel.setToolTipText("0% is invisible");
      jpanel1.add(opacityLabel,cc.xy(2,4));

      opacityCombo.setName("opacityCombo");
      jpanel1.add(opacityCombo,new CellConstraints(4,4,1,1,CellConstraints.LEFT,CellConstraints.DEFAULT));

      numFormatLabel.setName("numFormatLabel");
      numFormatLabel.setText("Number format");
      jpanel1.add(numFormatLabel,cc.xy(8,4));

      numFormatField.setName("numFormatField");
      jpanel1.add(numFormatField,cc.xy(10,4));

      interpolatedCheckbox.setActionCommand("Interpolated");
      interpolatedCheckbox.setName("interpolatedCheckbox");
      interpolatedCheckbox.setText("Interpolated");
      jpanel1.add(interpolatedCheckbox,cc.xy(12,4));

      novalueLabel.setName("novalueLabel");
      novalueLabel.setText("Optional Novalue");
      jpanel1.add(novalueLabel,cc.xy(2,6));

      novalueTextfield.setName("novalueTextfield");
      novalueTextfield.setToolTipText("An optional novalue to consider.");
      jpanel1.add(novalueTextfield,cc.xywh(4,6,3,1));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11 });
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