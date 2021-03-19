package org.hortonmachine.gforms;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class FormBuilderView extends JPanel
{
   JTextField _filePathtext = new JTextField();
   JButton _browseButton = new JButton();
   JButton _newButton = new JButton();
   JComboBox _buttonsCombo = new JComboBox();
   JButton _addSectionButton = new JButton();
   JButton _deleteSectionButton = new JButton();
   JButton _addWidgetButton = new JButton();
   JButton _deleteWidgetButton = new JButton();
   JComboBox _widgetsCombo = new JComboBox();
   JComboBox _loadedWidgetsCombo = new JComboBox();
   JTabbedPane _buttonsTabPane = new JTabbedPane();
   JButton _addFormButton = new JButton();
   JButton _deleteFormButton = new JButton();

   /**
    * Default constructor
    */
   public FormBuilderView()
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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:GROW(1.0),CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _filePathtext.setName("filePathtext");
      jpanel1.add(_filePathtext,cc.xywh(2,2,14,1));

      _browseButton.setActionCommand("...");
      _browseButton.setName("browseButton");
      _browseButton.setText("...");
      _browseButton.setToolTipText("Load an existing Geopaparazzi Form");
      jpanel1.add(_browseButton,cc.xy(17,2));

      _newButton.setActionCommand("new");
      _newButton.setName("newButton");
      _newButton.setText("new");
      _newButton.setToolTipText("Create a new Geopaparazzi Form");
      jpanel1.add(_newButton,cc.xy(19,2));

      _buttonsCombo.setName("buttonsCombo");
      jpanel1.add(_buttonsCombo,cc.xywh(2,4,14,1));

      _addSectionButton.setActionCommand("add");
      _addSectionButton.setName("addSectionButton");
      _addSectionButton.setText("add");
      _addSectionButton.setToolTipText("Add new Section (Geopaparazzi Button)");
      jpanel1.add(_addSectionButton,cc.xy(17,4));

      _deleteSectionButton.setActionCommand("del");
      _deleteSectionButton.setName("deleteSectionButton");
      _deleteSectionButton.setText("del");
      _deleteSectionButton.setToolTipText("Delete selected Section  (Geopaparazzi Button)");
      jpanel1.add(_deleteSectionButton,cc.xy(19,4));

      jpanel1.add(createPanel1(),cc.xywh(2,6,18,16));
      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22 });
      return jpanel1;
   }

   public JPanel createPanel1()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE","FILL:DEFAULT:GROW(1.0),CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      jpanel1.add(createPanel2(),cc.xy(1,7));
      _buttonsTabPane.setName("buttonsTabPane");
      _buttonsTabPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
      _buttonsTabPane.setTabPlacement(JTabbedPane.LEFT);
      jpanel1.add(_buttonsTabPane,cc.xywh(1,1,1,5));

      _addFormButton.setActionCommand("add");
      _addFormButton.setName("addFormButton");
      _addFormButton.setText("add");
      _addFormButton.setToolTipText("Add new tab to the current section.");
      jpanel1.add(_addFormButton,cc.xy(2,2));

      _deleteFormButton.setActionCommand("del");
      _deleteFormButton.setName("deleteFormButton");
      _deleteFormButton.setText("del");
      _deleteFormButton.setToolTipText("Delete current tab from the section.");
      jpanel1.add(_deleteFormButton,cc.xy(2,4));

      addFillComponents(jpanel1,new int[]{ 2 },new int[]{ 2,3,4,5,6,7 });
      return jpanel1;
   }

   public JPanel createPanel2()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:100DLU:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:30DLU:NONE,FILL:100DLU:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _addWidgetButton.setActionCommand("add");
      _addWidgetButton.setName("addWidgetButton");
      _addWidgetButton.setText("add");
      _addWidgetButton.setToolTipText("Add a selected widget to the current tab");
      jpanel1.add(_addWidgetButton,cc.xy(3,1));

      _deleteWidgetButton.setActionCommand("del");
      _deleteWidgetButton.setName("deleteWidgetButton");
      _deleteWidgetButton.setText("del");
      _deleteWidgetButton.setToolTipText("Delete selected widget from the current tab");
      jpanel1.add(_deleteWidgetButton,cc.xy(7,1));

      _widgetsCombo.setName("widgetsCombo");
      _widgetsCombo.setToolTipText("List of available widgets to add to the current tab.");
      jpanel1.add(_widgetsCombo,cc.xy(1,1));

      _loadedWidgetsCombo.setName("loadedWidgetsCombo");
      _loadedWidgetsCombo.setToolTipText("List of widgets on the current tab that can be removed.");
      jpanel1.add(_loadedWidgetsCombo,cc.xy(5,1));

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
