package org.jgrasstools.gui.spatialite;

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
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.border.TitledBorder;


public class SpatialiteView extends JPanel
{
   JPanel _databaseTreeView = new JPanel();
   JTree _databaseTree = new JTree();
   JButton _runQueryButton = new JButton();
   JButton _runQueryAndStoreButton = new JButton();
   JButton _runQueryAndStoreShapefileButton = new JButton();
   JButton _clearSqlEditorbutton = new JButton();
   JTextArea _sqlEditorArea = new JTextArea();
   JTable _dataViewerTable = new JTable();
   JButton _newDbButton = new JButton();
   JButton _connectDbButton = new JButton();
   JButton _disconnectDbButton = new JButton();

   /**
    * Default constructor
    */
   public SpatialiteView()
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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(0.3),FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,FILL:DEFAULT:GROW(0.1),FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      jpanel1.add(createdatabaseTreeView(),new CellConstraints(2,4,1,2,CellConstraints.FILL,CellConstraints.FILL));
      jpanel1.add(createPanel1(),cc.xy(4,4));
      jpanel1.add(createPanel2(),cc.xy(4,5));
      jpanel1.add(createPanel3(),cc.xywh(2,2,3,1));
      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5 },new int[]{ 1,2,3,4,5,6 });
      return jpanel1;
   }

   public JPanel createdatabaseTreeView()
   {
      _databaseTreeView.setName("databaseTreeView");
      TitledBorder titledborder1 = new TitledBorder(null,"Database Connection",TitledBorder.LEFT,TitledBorder.DEFAULT_POSITION,null,new Color(90,90,90));
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
      TitledBorder titledborder1 = new TitledBorder(null,"SQL Editor",TitledBorder.LEFT,TitledBorder.DEFAULT_POSITION,null,new Color(90,90,90));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0)","CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _runQueryButton.setActionCommand("1");
      _runQueryButton.setName("runQueryButton");
      _runQueryButton.setText("1");
      jpanel1.add(_runQueryButton,cc.xy(1,1));

      _runQueryAndStoreButton.setActionCommand("2");
      _runQueryAndStoreButton.setName("runQueryAndStoreButton");
      _runQueryAndStoreButton.setText("2");
      jpanel1.add(_runQueryAndStoreButton,cc.xy(1,3));

      _runQueryAndStoreShapefileButton.setActionCommand("3");
      _runQueryAndStoreShapefileButton.setName("runQueryAndStoreShapefileButton");
      _runQueryAndStoreShapefileButton.setText("3");
      jpanel1.add(_runQueryAndStoreShapefileButton,cc.xy(1,5));

      _clearSqlEditorbutton.setActionCommand("4");
      _clearSqlEditorbutton.setName("clearSqlEditorbutton");
      _clearSqlEditorbutton.setText("4");
      jpanel1.add(_clearSqlEditorbutton,cc.xy(1,7));

      _sqlEditorArea.setName("sqlEditorArea");
      JScrollPane jscrollpane1 = new JScrollPane();
      jscrollpane1.setViewportView(_sqlEditorArea);
      jscrollpane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      jscrollpane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      jpanel1.add(jscrollpane1,cc.xywh(3,1,1,9));

      addFillComponents(jpanel1,new int[]{ 2 },new int[]{ 2,4,6,8,9 });
      return jpanel1;
   }

   public JPanel createPanel2()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Data Viewer",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(90,90,90));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","FILL:DEFAULT:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _dataViewerTable.setName("dataViewerTable");
      JScrollPane jscrollpane1 = new JScrollPane();
      jscrollpane1.setViewportView(_dataViewerTable);
      jscrollpane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      jscrollpane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      jpanel1.add(jscrollpane1,new CellConstraints(1,1,1,1,CellConstraints.FILL,CellConstraints.FILL));

      addFillComponents(jpanel1,new int[0],new int[0]);
      return jpanel1;
   }

   public JPanel createPanel3()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _newDbButton.setActionCommand("New");
      _newDbButton.setName("newDbButton");
      _newDbButton.setText("New");
      jpanel1.add(_newDbButton,cc.xy(1,1));

      _connectDbButton.setActionCommand("Connect");
      _connectDbButton.setName("connectDbButton");
      _connectDbButton.setText("Connect");
      jpanel1.add(_connectDbButton,cc.xy(3,1));

      _disconnectDbButton.setActionCommand("Disconnect");
      _disconnectDbButton.setName("disconnectDbButton");
      _disconnectDbButton.setText("Disconnect");
      jpanel1.add(_disconnectDbButton,cc.xy(5,1));

      addFillComponents(jpanel1,new int[]{ 2,4,6,7,8,9,10 },new int[0]);
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
