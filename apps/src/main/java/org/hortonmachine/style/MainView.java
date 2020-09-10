package org.hortonmachine.style;

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
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.border.TitledBorder;


public class MainView extends JPanel
{
   JTextField _filepathField = new JTextField();
   JTree _rulesTree = new JTree();
   JButton _browseButton = new JButton();
   JPanel _stylePanelSpace = new JPanel();
   JPanel _stylePanel = new JPanel();
   JPanel _mapPaneHolder = new JPanel();
   JButton _saveButton = new JButton();
   JButton _saveTemplateButton = new JButton();
   JButton _loadTemplateButton = new JButton();
   JButton _deleteTemplateButton = new JButton();
   JButton _loadSldButton = new JButton();

   /**
    * Default constructor
    */
   public MainView()
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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(0.5),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:20DLU:NONE,FILL:4DLU:NONE,FILL:20DLU:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _filepathField.setName("filepathField");
      jpanel1.add(_filepathField,cc.xywh(2,2,5,1));

      _rulesTree.setName("rulesTree");
      TitledBorder titledborder1 = new TitledBorder(null,"Style Tree",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(0,0,0));
      _rulesTree.setBorder(titledborder1);
      JScrollPane jscrollpane1 = new JScrollPane();
      jscrollpane1.setViewportView(_rulesTree);
      jscrollpane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      jscrollpane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      jpanel1.add(jscrollpane1,cc.xywh(2,4,9,6));

      _browseButton.setActionCommand("...");
      _browseButton.setName("browseButton");
      _browseButton.setText("...");
      jpanel1.add(_browseButton,cc.xy(8,2));

      jpanel1.add(createstylePanelSpace(),cc.xywh(12,2,10,19));
      jpanel1.add(createmapPaneHolder(),cc.xywh(2,15,9,5));
      _saveButton.setActionCommand("save");
      _saveButton.setName("saveButton");
      _saveButton.setText("save");
      jpanel1.add(_saveButton,cc.xy(10,2));

      jpanel1.add(createPanel1(),cc.xywh(2,12,9,1));
      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21 });
      return jpanel1;
   }

   public JPanel createstylePanelSpace()
   {
      _stylePanelSpace.setName("stylePanelSpace");
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","FILL:DEFAULT:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      _stylePanelSpace.setLayout(formlayout1);

      _stylePanel.setName("stylePanel");
      _stylePanelSpace.add(_stylePanel,cc.xy(1,1));

      addFillComponents(_stylePanelSpace,new int[0],new int[0]);
      return _stylePanelSpace;
   }

   public JPanel createmapPaneHolder()
   {
      _mapPaneHolder.setName("mapPaneHolder");
      TitledBorder titledborder1 = new TitledBorder(null,"Map Preview",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(0,0,0));
      _mapPaneHolder.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","CENTER:DEFAULT:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      _mapPaneHolder.setLayout(formlayout1);

      addFillComponents(_mapPaneHolder,new int[]{ 1 },new int[]{ 1 });
      return _mapPaneHolder;
   }

   public JPanel createPanel1()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Templates",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(0,0,0));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0),FILL:4DLU:NONE,FILL:DEFAULT:GROW(1.0),FILL:4DLU:NONE,FILL:DEFAULT:GROW(1.0),FILL:4DLU:NONE,FILL:DEFAULT:GROW(1.0)","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _saveTemplateButton.setActionCommand("save");
      _saveTemplateButton.setName("saveTemplateButton");
      _saveTemplateButton.setText("save");
      jpanel1.add(_saveTemplateButton,cc.xy(1,1));

      _loadTemplateButton.setActionCommand("load");
      _loadTemplateButton.setName("loadTemplateButton");
      _loadTemplateButton.setText("load");
      jpanel1.add(_loadTemplateButton,cc.xy(3,1));

      _deleteTemplateButton.setActionCommand("Load from template");
      _deleteTemplateButton.setName("deleteTemplateButton");
      _deleteTemplateButton.setText("delete");
      jpanel1.add(_deleteTemplateButton,cc.xy(7,1));

      _loadSldButton.setActionCommand("load");
      _loadSldButton.setName("loadSldButton");
      _loadSldButton.setText("from sld");
      jpanel1.add(_loadSldButton,cc.xy(5,1));

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
