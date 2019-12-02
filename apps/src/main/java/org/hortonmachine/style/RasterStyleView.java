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
   JLabel _selectColorTableLabel = new JLabel();
   JComboBox _colortablesCombo = new JComboBox();
   JButton _applyTableButton = new JButton();
   JLabel _customStyleLabel = new JLabel();
   JTextArea _customStyleArea = new JTextArea();
   JButton _customStyleButton = new JButton();
   JLabel _opacityLabel = new JLabel();
   JComboBox _opacityCombo = new JComboBox();
   JLabel _novalueLabel = new JLabel();
   JTextField _novalueTextfield = new JTextField();
   JLabel _selectColorTableLabel1 = new JLabel();
   JCheckBox _shadedReliefCheck = new JCheckBox();
   JTextField _reliefFactorField = new JTextField();

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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:GROW(0.3),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,FILL:DEFAULT:GROW(0.5),CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _selectColorTableLabel.setName("selectColorTableLabel");
      _selectColorTableLabel.setText("Select Colortable");
      jpanel1.add(_selectColorTableLabel,cc.xy(2,4));

      _colortablesCombo.setName("colortablesCombo");
      jpanel1.add(_colortablesCombo,cc.xywh(4,4,10,1));

      _applyTableButton.setActionCommand("Apply");
      _applyTableButton.setName("applyTableButton");
      _applyTableButton.setText("Apply");
      jpanel1.add(_applyTableButton,cc.xy(15,4));

      _customStyleLabel.setName("customStyleLabel");
      _customStyleLabel.setText("Define custom");
      jpanel1.add(_customStyleLabel,new CellConstraints(2,6,1,1,CellConstraints.DEFAULT,CellConstraints.CENTER));

      _customStyleArea.setName("customStyleArea");
      _customStyleArea.setRows(8);
      JScrollPane jscrollpane1 = new JScrollPane();
      jscrollpane1.setViewportView(_customStyleArea);
      jscrollpane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      jscrollpane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      jpanel1.add(jscrollpane1,cc.xywh(4,6,10,1));

      _customStyleButton.setActionCommand("Apply");
      _customStyleButton.setName("customStyleButton");
      _customStyleButton.setText("Apply");
      jpanel1.add(_customStyleButton,new CellConstraints(15,6,1,1,CellConstraints.DEFAULT,CellConstraints.CENTER));

      jpanel1.add(createPanel1(),cc.xywh(2,2,14,1));
      _selectColorTableLabel1.setName("selectColorTableLabel");
      _selectColorTableLabel1.setText("Add shaded relief");
      jpanel1.add(_selectColorTableLabel1,cc.xy(2,9));

      _shadedReliefCheck.setName("shadedReliefCheck");
      jpanel1.add(_shadedReliefCheck,cc.xy(4,9));

      jpanel1.add(createPanel2(),cc.xywh(7,9,7,1));
      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11 });
      return jpanel1;
   }

   public JPanel createPanel1()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:40.0MM:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:40.0MM:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0)","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _opacityLabel.setName("opacityLabel");
      _opacityLabel.setText("Opacity");
      _opacityLabel.setToolTipText("0% is invisible");
      jpanel1.add(_opacityLabel,cc.xy(1,1));

      _opacityCombo.setName("opacityCombo");
      jpanel1.add(_opacityCombo,cc.xy(3,1));

      _novalueLabel.setName("novalueLabel");
      _novalueLabel.setText("Optional Novalue");
      jpanel1.add(_novalueLabel,cc.xy(5,1));

      _novalueTextfield.setName("novalueTextfield");
      _novalueTextfield.setToolTipText("An optional novalue to consider.");
      jpanel1.add(_novalueTextfield,cc.xy(7,1));

      addFillComponents(jpanel1,new int[]{ 2,4,6,8,9 },new int[0]);
      return jpanel1;
   }

   public JPanel createPanel2()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:24DLU:NONE","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      JLabel jlabel1 = new JLabel();
      jlabel1.setText("relief factor");
      jpanel1.add(jlabel1,cc.xy(1,1));

      _reliefFactorField.setName("reliefFactorField");
      jpanel1.add(_reliefFactorField,cc.xy(3,1));

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
