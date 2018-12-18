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


public class TextSymbolizerView extends JPanel
{
   JButton _applyButton = new JButton();
   JLabel _opacityLabel = new JLabel();
   JSpinner _opacitySpinner = new JSpinner();
   JLabel _rotationLabel = new JLabel();
   JSpinner _rotationSpinner = new JSpinner();
   JLabel _fontLabel = new JLabel();
   JLabel _fontColorLabel = new JLabel();
   JComboBox _labelCombo = new JComboBox();
   JLabel _labelLabel = new JLabel();
   JButton _colorFontButton = new JButton();
   JButton _fontButton = new JButton();
   JButton _haloColorButton = new JButton();
   JLabel _haloColorLabel = new JLabel();
   JLabel _haloSizeLabel = new JLabel();
   JSpinner _haloSizeSpinner = new JSpinner();
   JSpinner _displacementXSpinner = new JSpinner();
   JLabel _displacementYLabel = new JLabel();
   JSpinner _displacementYSpinner = new JSpinner();
   JLabel _displacementXLabel = new JLabel();
   JLabel _anchorXLabel = new JLabel();
   JComboBox _anchorXCombo = new JComboBox();
   JLabel _anchorYLabel = new JLabel();
   JComboBox _anchorYCombo = new JComboBox();
   JLabel _voAutoWrapPixelsLabel = new JLabel();
   JLabel _voSpaceAroundPixelsLabel = new JLabel();
   JLabel _voRepeatEveryPixelLabel = new JLabel();
   JLabel _voFollowLineLabel = new JLabel();
   JLabel _voMaxDispPixelLabel = new JLabel();
   JLabel _voMaxAngleAllowedLabel = new JLabel();
   JTextField _voMaxDispPixelText = new JTextField();
   JTextField _voAutoWrapPixelsText = new JTextField();
   JTextField _voSpaceAroundPixelsText = new JTextField();
   JTextField _voRepeatEveryPixelText = new JTextField();
   JTextField _voFollowLineText = new JTextField();
   JTextField _voMaxAngleAllowedText = new JTextField();
   JLabel _perpenOffsetLabel = new JLabel();
   JTextField _perpenOffsetText = new JTextField();
   JLabel _initialGapLabel = new JLabel();
   JTextField _initialGapText = new JTextField();

   /**
    * Default constructor
    */
   public TextSymbolizerView()
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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _applyButton.setActionCommand("Apply");
      _applyButton.setName("applyButton");
      _applyButton.setText("Apply");
      jpanel1.add(_applyButton,cc.xywh(7,7,13,1));

      jpanel1.add(createPanel1(),cc.xywh(2,2,18,1));
      jpanel1.add(createPanel2(),cc.xywh(2,4,18,1));
      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20 },new int[]{ 1,2,3,4,5,6,7,8 });
      return jpanel1;
   }

   public JPanel createPanel1()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Labelling",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:6.0CM:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _opacityLabel.setName("opacityLabel");
      _opacityLabel.setText("opacity");
      jpanel1.add(_opacityLabel,cc.xy(2,4));

      _opacitySpinner.setName("opacitySpinner");
      jpanel1.add(_opacitySpinner,cc.xy(4,4));

      _rotationLabel.setName("rotationLabel");
      _rotationLabel.setText("rotation");
      jpanel1.add(_rotationLabel,cc.xy(2,6));

      _rotationSpinner.setName("rotationSpinner");
      jpanel1.add(_rotationSpinner,cc.xy(4,6));

      _fontLabel.setName("fontLabel");
      _fontLabel.setText("font");
      jpanel1.add(_fontLabel,cc.xy(2,8));

      _fontColorLabel.setName("fontColorLabel");
      _fontColorLabel.setText("font color");
      jpanel1.add(_fontColorLabel,cc.xy(2,10));

      _labelCombo.setName("labelCombo");
      jpanel1.add(_labelCombo,cc.xy(4,2));

      _labelLabel.setName("labelLabel");
      _labelLabel.setText("label");
      jpanel1.add(_labelLabel,cc.xy(2,2));

      _colorFontButton.setActionCommand("border color");
      _colorFontButton.setName("colorFontButton");
      _colorFontButton.setText("font color");
      jpanel1.add(_colorFontButton,cc.xy(4,10));

      _fontButton.setActionCommand("choose font");
      _fontButton.setName("fontButton");
      _fontButton.setText("choose font");
      jpanel1.add(_fontButton,cc.xy(4,8));

      _haloColorButton.setActionCommand("fill color");
      _haloColorButton.setName("haloColorButton");
      _haloColorButton.setText("halo color");
      jpanel1.add(_haloColorButton,cc.xy(4,14));

      _haloColorLabel.setName("haloColorLabel");
      _haloColorLabel.setText("halo color");
      jpanel1.add(_haloColorLabel,cc.xy(2,14));

      _haloSizeLabel.setName("haloSizeLabel");
      _haloSizeLabel.setText("halo size");
      jpanel1.add(_haloSizeLabel,cc.xy(2,12));

      _haloSizeSpinner.setName("haloSizeSpinner");
      jpanel1.add(_haloSizeSpinner,cc.xy(4,12));

      _displacementXSpinner.setName("displacementXSpinner");
      jpanel1.add(_displacementXSpinner,cc.xy(4,20));

      _displacementYLabel.setName("displacementYLabel");
      _displacementYLabel.setText("displacement y");
      jpanel1.add(_displacementYLabel,cc.xy(2,22));

      _displacementYSpinner.setName("displacementYSpinner");
      jpanel1.add(_displacementYSpinner,cc.xy(4,22));

      _displacementXLabel.setName("displacementXLabel");
      _displacementXLabel.setText("displacement x");
      jpanel1.add(_displacementXLabel,cc.xy(2,20));

      _anchorXLabel.setName("anchorXLabel");
      _anchorXLabel.setText("anchor x");
      jpanel1.add(_anchorXLabel,cc.xy(2,16));

      _anchorXCombo.setName("anchorXCombo");
      jpanel1.add(_anchorXCombo,cc.xy(4,16));

      _anchorYLabel.setName("anchorYLabel");
      _anchorYLabel.setText("anchor y");
      jpanel1.add(_anchorYLabel,cc.xy(2,18));

      _anchorYCombo.setName("anchorYCombo");
      jpanel1.add(_anchorYCombo,cc.xy(4,18));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23 });
      return jpanel1;
   }

   public JPanel createPanel2()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Other options (if applicable)",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:6.0CM:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _voAutoWrapPixelsLabel.setName("voAutoWrapPixelsLabel");
      _voAutoWrapPixelsLabel.setText("auto wrap pixels (VO)");
      jpanel1.add(_voAutoWrapPixelsLabel,cc.xy(2,4));

      _voSpaceAroundPixelsLabel.setName("voSpaceAroundPixelsLabel");
      _voSpaceAroundPixelsLabel.setText("space around pixels (VO)");
      jpanel1.add(_voSpaceAroundPixelsLabel,cc.xy(2,6));

      _voRepeatEveryPixelLabel.setName("voRepeatEveryPixelLabel");
      _voRepeatEveryPixelLabel.setText("repeat every pixels (VO)");
      jpanel1.add(_voRepeatEveryPixelLabel,cc.xy(2,8));

      _voFollowLineLabel.setName("voFollowLineLabel");
      _voFollowLineLabel.setText("follow line (VO)");
      jpanel1.add(_voFollowLineLabel,cc.xy(2,10));

      _voMaxDispPixelLabel.setName("voMaxDispPixelLabel");
      _voMaxDispPixelLabel.setText("max displacement pixels (VO)");
      jpanel1.add(_voMaxDispPixelLabel,cc.xy(2,2));

      _voMaxAngleAllowedLabel.setName("voMaxAngleAllowedLabel");
      _voMaxAngleAllowedLabel.setText("max angle allowed (VO)");
      jpanel1.add(_voMaxAngleAllowedLabel,cc.xy(2,12));

      _voMaxDispPixelText.setName("voMaxDispPixelText");
      jpanel1.add(_voMaxDispPixelText,cc.xy(4,2));

      _voAutoWrapPixelsText.setName("voAutoWrapPixelsText");
      jpanel1.add(_voAutoWrapPixelsText,cc.xy(4,4));

      _voSpaceAroundPixelsText.setName("voSpaceAroundPixelsText");
      jpanel1.add(_voSpaceAroundPixelsText,cc.xy(4,6));

      _voRepeatEveryPixelText.setName("voRepeatEveryPixelText");
      jpanel1.add(_voRepeatEveryPixelText,cc.xy(4,8));

      _voFollowLineText.setName("voFollowLineText");
      jpanel1.add(_voFollowLineText,cc.xy(4,10));

      _voMaxAngleAllowedText.setName("voMaxAngleAllowedText");
      jpanel1.add(_voMaxAngleAllowedText,cc.xy(4,12));

      _perpenOffsetLabel.setName("perpenOffsetLabel");
      _perpenOffsetLabel.setText("perpendicular offset");
      jpanel1.add(_perpenOffsetLabel,cc.xy(2,14));

      _perpenOffsetText.setName("perpenOffsetText");
      jpanel1.add(_perpenOffsetText,cc.xy(4,14));

      _initialGapLabel.setName("initialGapLabel");
      _initialGapLabel.setText("initial gap");
      jpanel1.add(_initialGapLabel,cc.xy(2,16));

      _initialGapText.setName("initialGapText");
      jpanel1.add(_initialGapText,cc.xy(4,16));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17 });
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
