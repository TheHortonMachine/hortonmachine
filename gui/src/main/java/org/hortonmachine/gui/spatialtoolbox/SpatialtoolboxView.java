package org.hortonmachine.gui.spatialtoolbox;

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
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class SpatialtoolboxView extends JPanel
{
   JPanel _parametersPanel = new JPanel();
   JTree _modulesTree = new JTree();
   JLabel _parametersLabel = new JLabel();
   JButton _startButton = new JButton();
   JButton _runScriptButton = new JButton();
   JToggleButton _processingRegionButton = new JToggleButton();
   JButton _generateScriptButton = new JButton();
   JButton _viewDataButton = new JButton();
   JTextField _filterField = new JTextField();
   JButton _clearFilterButton = new JButton();
   JCheckBox _loadExperimentalCheckbox = new JCheckBox();
   JCheckBox _debugCheckbox = new JCheckBox();
   JLabel _heapLabel = new JLabel();
   JComboBox _heapCombo = new JComboBox();

   /**
    * Default constructor
    */
   public SpatialtoolboxView()
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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(0.3),FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,FILL:DEFAULT:NONE,CENTER:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      JLabel jlabel1 = new JLabel();
      jlabel1.setText("Modules");
      jpanel1.add(jlabel1,cc.xy(2,2));

      _parametersPanel.setName("parametersPanel");
      jpanel1.add(_parametersPanel,new CellConstraints(4,4,1,9,CellConstraints.FILL,CellConstraints.FILL));

      _modulesTree.setName("modulesTree");
      JScrollPane jscrollpane1 = new JScrollPane();
      jscrollpane1.setViewportView(_modulesTree);
      jscrollpane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      jscrollpane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      jpanel1.add(jscrollpane1,cc.xy(2,6));

      jpanel1.add(createPanel1(),cc.xy(4,2));
      jpanel1.add(createPanel3(),cc.xy(2,4));
      _loadExperimentalCheckbox.setActionCommand("Load Experimental");
      _loadExperimentalCheckbox.setName("loadExperimentalCheckbox");
      _loadExperimentalCheckbox.setText("Load Experimental");
      jpanel1.add(_loadExperimentalCheckbox,cc.xy(2,8));

      _debugCheckbox.setActionCommand("Debug");
      _debugCheckbox.setName("debugCheckbox");
      _debugCheckbox.setText("Debug");
      jpanel1.add(_debugCheckbox,cc.xy(2,10));

      jpanel1.add(createPanel4(),cc.xy(2,12));
      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13 });
      return jpanel1;
   }

   public JPanel createPanel1()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,RIGHT:DEFAULT:GROW(1.0)","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _parametersLabel.setName("parametersLabel");
      _parametersLabel.setText("Parameters");
      jpanel1.add(_parametersLabel,cc.xy(1,1));

      jpanel1.add(createPanel2(),cc.xy(2,1));
      addFillComponents(jpanel1,new int[]{ 2 },new int[0]);
      return jpanel1;
   }

   public JPanel createPanel2()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:NONE","FILL:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _startButton.setName("startButton");
      jpanel1.add(_startButton,cc.xy(3,1));

      _runScriptButton.setName("runScriptButton");
      jpanel1.add(_runScriptButton,cc.xy(5,1));

      _processingRegionButton.setName("processingRegionButton");
      jpanel1.add(_processingRegionButton,cc.xy(1,1));

      _generateScriptButton.setName("generateScriptButton");
      jpanel1.add(_generateScriptButton,cc.xy(7,1));

      _viewDataButton.setName("viewDataButton");
      jpanel1.add(_viewDataButton,cc.xy(9,1));

      addFillComponents(jpanel1,new int[]{ 2,4,6,8 },new int[0]);
      return jpanel1;
   }

   public JPanel createPanel3()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0),FILL:4DLU:NONE,FILL:DEFAULT:NONE","FILL:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _filterField.setName("filterField");
      jpanel1.add(_filterField,cc.xy(1,1));

      _clearFilterButton.setName("clearFilterButton");
      jpanel1.add(_clearFilterButton,cc.xy(3,1));

      addFillComponents(jpanel1,new int[]{ 2 },new int[0]);
      return jpanel1;
   }

   public JPanel createPanel4()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:GROW(1.0)","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _heapLabel.setName("heapLabel");
      _heapLabel.setText("Heap [MB]");
      jpanel1.add(_heapLabel,cc.xy(1,1));

      _heapCombo.setEditable(true);
      _heapCombo.setName("heapCombo");
      _heapCombo.setRequestFocusEnabled(false);
      jpanel1.add(_heapCombo,cc.xy(3,1));

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
