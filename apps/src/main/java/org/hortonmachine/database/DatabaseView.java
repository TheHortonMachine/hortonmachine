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
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.border.TitledBorder;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class DatabaseView extends JPanel
{
   JPanel _databaseTreeView = new JPanel();
   JTree _databaseTree = new JTree();
   JButton _runQueryButton = new JButton();
   JButton _runQueryAndStoreButton = new JButton();
   JButton _runQueryAndStoreShapefileButton = new JButton();
   JButton _clearSqlEditorbutton = new JButton();
   JButton _viewQueryButton = new JButton();
   JPanel _sqlEditorAreaPanel = new JPanel();
   JPanel _dataViewerPanel = new JPanel();
   JButton _newDbButton = new JButton();
   JButton _connectDbButton = new JButton();
   JButton _disconnectDbButton = new JButton();
   JButton _historyButton = new JButton();
   JButton _templatesButton = new JButton();
   JButton _connectRemoteDbButton = new JButton();
   JButton _settingsButton = new JButton();
   JCheckBox _refreshTreeAfterQueryCheckbox = new JCheckBox();
   JLabel _limitCountLabel = new JLabel();
   JTextField _limitCountTextfield = new JTextField();
   JLabel _recordCountLabel = new JLabel();
   JTextField _recordCountTextfield = new JTextField();

   /**
    * Default constructor
    */
   public DatabaseView()
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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:250PX:GROW(0.3),FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(0.7),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,FILL:DEFAULT:GROW(1.0),CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,FILL:DEFAULT:GROW(1.0),CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,FILL:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      jpanel1.add(createdatabaseTreeView(),new CellConstraints(2,4,1,7,CellConstraints.FILL,CellConstraints.FILL));
      jpanel1.add(createPanel1(),cc.xy(4,4));
      jpanel1.add(createPanel2(),cc.xy(4,8));
      jpanel1.add(createPanel3(),cc.xywh(2,2,3,1));
      jpanel1.add(createPanel4(),cc.xy(4,6));
      jpanel1.add(createPanel5(),cc.xy(4,10));
      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11 });
      return jpanel1;
   }

   public JPanel createdatabaseTreeView()
   {
      _databaseTreeView.setName("databaseTreeView");
      TitledBorder titledborder1 = new TitledBorder(null,"Database Connection",TitledBorder.LEFT,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
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
      TitledBorder titledborder1 = new TitledBorder(null,"SQL Editor",TitledBorder.LEFT,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
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
      TitledBorder titledborder1 = new TitledBorder(null,"Data Viewer",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","FILL:DEFAULT:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _dataViewerPanel.setName("dataViewerPanel");
      jpanel1.add(_dataViewerPanel,cc.xy(1,1));

      addFillComponents(jpanel1,new int[0],new int[0]);
      return jpanel1;
   }

   public JPanel createPanel3()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE");
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
      jpanel1.add(_disconnectDbButton,cc.xy(7,1));

      _historyButton.setActionCommand("History");
      _historyButton.setName("historyButton");
      _historyButton.setText("History");
      jpanel1.add(_historyButton,cc.xy(9,1));

      _templatesButton.setActionCommand("Templates");
      _templatesButton.setName("templatesButton");
      _templatesButton.setText("Templates");
      jpanel1.add(_templatesButton,cc.xy(11,1));

      _connectRemoteDbButton.setActionCommand("Connect");
      _connectRemoteDbButton.setName("connectRemoteDbButton");
      _connectRemoteDbButton.setText("Connect Remote");
      jpanel1.add(_connectRemoteDbButton,cc.xy(5,1));

      _settingsButton.setActionCommand("Templates");
      _settingsButton.setName("settingsButton");
      _settingsButton.setText("Settings");
      jpanel1.add(_settingsButton,cc.xy(13,1));

      addFillComponents(jpanel1,new int[]{ 2,4,6,8,10,12,14,15 },new int[0]);
      return jpanel1;
   }

   public JPanel createPanel4()
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

   public JPanel createPanel5()
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

   /**
    * Initializer
    */
   protected void initializePanel()
   {
      setLayout(new BorderLayout());
      add(createPanel(), BorderLayout.CENTER);
   }


}
