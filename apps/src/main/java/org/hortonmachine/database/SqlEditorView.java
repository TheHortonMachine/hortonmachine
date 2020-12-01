package org.hortonmachine.database;

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
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class SqlEditorView extends JPanel
{
   JButton _runQueryButton = new JButton();
   JButton _runQueryAndStoreButton = new JButton();
   JButton _runQueryAndStoreShapefileButton = new JButton();
   JButton _clearSqlEditorbutton = new JButton();
   JButton _viewQueryButton = new JButton();
   JPanel _sqlEditorAreaPanel = new JPanel();
   JCheckBox _refreshTreeAfterQueryCheckbox = new JCheckBox();
   JLabel _limitCountLabel = new JLabel();
   JTextField _limitCountTextfield = new JTextField();

   /**
    * Default constructor
    */
   public SqlEditorView()
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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(0.7)","FILL:DEFAULT:GROW(1.0),CENTER:2DLU:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      jpanel1.add(createPanel1(),cc.xy(1,1));
      jpanel1.add(createPanel2(),cc.xy(1,3));
      addFillComponents(jpanel1,new int[]{ 1 },new int[]{ 1,2,3 });
      return jpanel1;
   }

   public JPanel createPanel1()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"SQL Editor",TitledBorder.LEFT,TitledBorder.DEFAULT_POSITION,null,new Color(0,0,0));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0)","CENTER:2DLU:NONE,FILL:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,FILL:2DLU:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _runQueryButton.setActionCommand("1");
      _runQueryButton.setName("runQueryButton");
      _runQueryButton.setText("1");
      jpanel1.add(_runQueryButton,cc.xy(1,2));

      _runQueryAndStoreButton.setActionCommand("2");
      _runQueryAndStoreButton.setName("runQueryAndStoreButton");
      _runQueryAndStoreButton.setText("2");
      jpanel1.add(_runQueryAndStoreButton,cc.xy(1,4));

      _runQueryAndStoreShapefileButton.setActionCommand("3");
      _runQueryAndStoreShapefileButton.setName("runQueryAndStoreShapefileButton");
      _runQueryAndStoreShapefileButton.setText("3");
      jpanel1.add(_runQueryAndStoreShapefileButton,cc.xy(1,6));

      _clearSqlEditorbutton.setActionCommand("4");
      _clearSqlEditorbutton.setName("clearSqlEditorbutton");
      _clearSqlEditorbutton.setText("5");
      jpanel1.add(_clearSqlEditorbutton,cc.xy(1,10));

      _viewQueryButton.setActionCommand("4");
      _viewQueryButton.setName("viewQueryButton");
      _viewQueryButton.setText("4");
      jpanel1.add(_viewQueryButton,cc.xy(1,8));

      _sqlEditorAreaPanel.setName("sqlEditorAreaPanel");
      jpanel1.add(_sqlEditorAreaPanel,cc.xywh(2,2,1,10));

      addFillComponents(jpanel1,new int[]{ 1,2 },new int[]{ 1,3,5,7,9,11 });
      return jpanel1;
   }

   public JPanel createPanel2()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:GROW(0.4),FILL:4DLU:NONE,FILL:DEFAULT:GROW(0.5)","CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _refreshTreeAfterQueryCheckbox.setActionCommand("Refresh tree after query");
      _refreshTreeAfterQueryCheckbox.setName("refreshTreeAfterQueryCheckbox");
      _refreshTreeAfterQueryCheckbox.setText("Refresh tree after query");
      jpanel1.add(_refreshTreeAfterQueryCheckbox,cc.xywh(1,1,3,1));

      _limitCountLabel.setName("limitCountLabel");
      _limitCountLabel.setText("Limit result to");
      jpanel1.add(_limitCountLabel,cc.xy(1,3));

      _limitCountTextfield.setName("limitCountTextfield");
      jpanel1.add(_limitCountTextfield,cc.xy(3,3));

      addFillComponents(jpanel1,new int[]{ 2,3,4,5 },new int[]{ 2 });
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
