package org.hortonmachine.database;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class DataTableView extends JPanel
{
   JPanel _dataViewerPanel = new JPanel();
   JLabel _recordCountLabel = new JLabel();
   JTextField _recordCountTextfield = new JTextField();
   JCheckBox _formatDatesCheckbox = new JCheckBox();
   JTextField _formatDatesPatternTextField = new JTextField();

   /**
    * Default constructor
    */
   public DataTableView()
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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(0.7)","FILL:DEFAULT:GROW(1.0),CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      jpanel1.add(createPanel1(),cc.xy(1,1));
      jpanel1.add(createPanel2(),cc.xy(1,5));
      jpanel1.add(createPanel3(),cc.xy(1,3));
      addFillComponents(jpanel1,new int[]{ 1 },new int[]{ 1,2,3,4,5 });
      return jpanel1;
   }

   public JPanel createPanel1()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Data Viewer",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(0,0,0));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","FILL:DEFAULT:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _dataViewerPanel.setName("dataViewerPanel");
      jpanel1.add(_dataViewerPanel,cc.xy(1,1));

      addFillComponents(jpanel1,new int[0],new int[0]);
      return jpanel1;
   }

   public JPanel createPanel2()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:GROW(0.4),FILL:4DLU:NONE,FILL:DEFAULT:GROW(0.5)","CENTER:2DLU:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _recordCountLabel.setName("recordCountLabel");
      _recordCountLabel.setText("Resulting records");
      jpanel1.add(_recordCountLabel,cc.xy(1,2));

      _recordCountTextfield.setName("recordCountTextfield");
      jpanel1.add(_recordCountTextfield,cc.xy(3,2));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5 },new int[]{ 1 });
      return jpanel1;
   }

   public JPanel createPanel3()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:8DLU:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:GROW(1.0)","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      JLabel jlabel1 = new JLabel();
      jlabel1.setText("Format dates");
      jpanel1.add(jlabel1,cc.xy(1,1));

      _formatDatesCheckbox.setName("formatDatesCheckbox");
      jpanel1.add(_formatDatesCheckbox,cc.xy(3,1));

      JLabel jlabel2 = new JLabel();
      jlabel2.setText("patterns");
      jpanel1.add(jlabel2,cc.xy(5,1));

      _formatDatesPatternTextField.setName("formatDatesPatternTextField");
      jpanel1.add(_formatDatesPatternTextField,cc.xy(7,1));

      addFillComponents(jpanel1,new int[]{ 2,4,6 },new int[0]);
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
