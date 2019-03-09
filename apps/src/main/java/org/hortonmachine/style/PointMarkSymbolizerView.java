package org.hortonmachine.style;

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
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class PointMarkSymbolizerView extends JPanel
{
   JButton _applyButton = new JButton();
   JLabel _widthLabel = new JLabel();
   JSpinner _widthSpinner = new JSpinner();
   JLabel _opacityBorderLabel = new JLabel();
   JSpinner _opacityBorderSpinner = new JSpinner();
   JLabel _colorBorderLabel = new JLabel();
   JButton _colorBorderButton = new JButton();
   JLabel _opacityFillLabel = new JLabel();
   JSpinner _opacityFillSpinner = new JSpinner();
   JLabel _colorFillLabel = new JLabel();
   JButton _colorFilleButton = new JButton();
   JComboBox _wkmarkCombo = new JComboBox();
   JLabel _sizeLabel = new JLabel();
   JSpinner _markSizeSpinner = new JSpinner();
   JLabel _rotationLabel = new JLabel();
   JSpinner _rotationSpinner = new JSpinner();
   JSpinner _offsetXSpinner = new JSpinner();
   JLabel _offsetXLabel = new JLabel();
   JLabel _offsetYLabel = new JLabel();
   JSpinner _offsetYSpinner = new JSpinner();
   JTextField _graphicPathField = new JTextField();
   JButton _browseGraphicButton = new JButton();
   JLabel _sizeLabel1 = new JLabel();

   /**
    * Default constructor
    */
   public PointMarkSymbolizerView()
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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _applyButton.setActionCommand("Apply");
      _applyButton.setName("applyButton");
      _applyButton.setText("Apply");
      jpanel1.add(_applyButton,cc.xywh(7,8,13,1));

      jpanel1.add(createPanel1(),cc.xywh(2,4,19,1));
      jpanel1.add(createPanel2(),cc.xywh(2,6,19,1));
      jpanel1.add(createPanel3(),cc.xywh(2,2,19,1));
      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20 },new int[]{ 1,2,3,4,5,6,7,8,9 });
      return jpanel1;
   }

   public JPanel createPanel1()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Border",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:6.0CM:NONE,FILL:4DLU:NONE,FILL:DEFAULT:GROW(1.0)","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _widthLabel.setName("widthLabel");
      _widthLabel.setText("width");
      jpanel1.add(_widthLabel,cc.xy(2,2));

      _widthSpinner.setName("widthSpinner");
      jpanel1.add(_widthSpinner,cc.xy(4,2));

      _opacityBorderLabel.setName("opacityBorderLabel");
      _opacityBorderLabel.setText("opacity");
      jpanel1.add(_opacityBorderLabel,cc.xy(2,4));

      _opacityBorderSpinner.setName("opacityBorderSpinner");
      jpanel1.add(_opacityBorderSpinner,cc.xy(4,4));

      _colorBorderLabel.setName("colorBorderLabel");
      _colorBorderLabel.setText("color");
      jpanel1.add(_colorBorderLabel,cc.xy(2,6));

      _colorBorderButton.setActionCommand("border color");
      _colorBorderButton.setName("colorBorderButton");
      _colorBorderButton.setText("border color");
      jpanel1.add(_colorBorderButton,cc.xy(4,6));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4 },new int[]{ 1,2,3,4,5,6,7 });
      return jpanel1;
   }

   public JPanel createPanel2()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Fill",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:6.0CM:NONE,FILL:4DLU:NONE,FILL:DEFAULT:GROW(1.0)","FILL:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _opacityFillLabel.setName("opacityFillLabel");
      _opacityFillLabel.setText("opacity");
      jpanel1.add(_opacityFillLabel,cc.xy(2,2));

      _opacityFillSpinner.setName("opacityFillSpinner");
      jpanel1.add(_opacityFillSpinner,cc.xy(4,2));

      _colorFillLabel.setName("colorFillLabel");
      _colorFillLabel.setText("color");
      jpanel1.add(_colorFillLabel,cc.xy(2,4));

      _colorFilleButton.setActionCommand("fill color");
      _colorFilleButton.setName("colorFilleButton");
      _colorFilleButton.setText("fill color");
      jpanel1.add(_colorFilleButton,cc.xy(4,4));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4 },new int[]{ 1,2,3,4,5 });
      return jpanel1;
   }

   public JPanel createPanel3()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Mark",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:6.0CM:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0)","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:12DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _wkmarkCombo.setName("wkmarkCombo");
      jpanel1.add(_wkmarkCombo,cc.xywh(2,2,3,1));

      _sizeLabel.setName("sizeLabel");
      _sizeLabel.setText("size");
      jpanel1.add(_sizeLabel,cc.xy(2,6));

      _markSizeSpinner.setName("markSizeSpinner");
      jpanel1.add(_markSizeSpinner,cc.xy(4,6));

      _rotationLabel.setName("rotationLabel");
      _rotationLabel.setText("rotation");
      jpanel1.add(_rotationLabel,cc.xy(2,8));

      _rotationSpinner.setName("rotationSpinner");
      jpanel1.add(_rotationSpinner,cc.xy(4,8));

      _offsetXSpinner.setName("offsetXSpinner");
      jpanel1.add(_offsetXSpinner,cc.xy(4,10));

      _offsetXLabel.setName("offsetXLabel");
      _offsetXLabel.setText("offset x");
      jpanel1.add(_offsetXLabel,cc.xy(2,10));

      _offsetYLabel.setName("offsetYLabel");
      _offsetYLabel.setText("offset y");
      jpanel1.add(_offsetYLabel,cc.xy(2,12));

      _offsetYSpinner.setName("offsetYSpinner");
      jpanel1.add(_offsetYSpinner,cc.xy(4,12));

      jpanel1.add(createPanel4(),cc.xy(4,4));
      _sizeLabel1.setName("sizeLabel");
      _sizeLabel1.setText("from graphic");
      jpanel1.add(_sizeLabel1,cc.xy(2,4));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13 });
      return jpanel1;
   }

   public JPanel createPanel4()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:24DLU:NONE","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _graphicPathField.setName("graphicPathField");
      jpanel1.add(_graphicPathField,cc.xy(1,1));

      _browseGraphicButton.setActionCommand("...");
      _browseGraphicButton.setName("browseGraphicButton");
      _browseGraphicButton.setText("...");
      jpanel1.add(_browseGraphicButton,cc.xy(3,1));

      addFillComponents(jpanel1,new int[]{ 2 },new int[0]);
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
