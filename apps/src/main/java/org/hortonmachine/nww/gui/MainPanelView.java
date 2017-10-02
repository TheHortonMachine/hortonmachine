package org.hortonmachine.nww.gui;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class MainPanelView extends JPanel
{
   JPanel _mainGridView = new JPanel();
   JPanel _nwwPanel = new JPanel();
   JPanel _layersPanel = new JPanel();
   JPanel _toolsPanel = new JPanel();

   /**
    * Default constructor
    */
   public MainPanelView()
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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","CENTER:DEFAULT:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      jpanel1.add(createmainGridView(),new CellConstraints(1,1,1,1,CellConstraints.FILL,CellConstraints.FILL));
      addFillComponents(jpanel1,new int[]{ 1 },new int[]{ 1 });
      return jpanel1;
   }

   public JPanel createmainGridView()
   {
      _mainGridView.setName("mainGridView");
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(0.3),FILL:DEFAULT:GROW(0.7)","FILL:DEFAULT:GROW(0.8),FILL:DEFAULT:GROW(0.2)");
      CellConstraints cc = new CellConstraints();
      _mainGridView.setLayout(formlayout1);

      _mainGridView.add(createnwwPanel(),cc.xywh(2,1,1,2));
      _mainGridView.add(createlayersPanel(),cc.xy(1,1));
      _mainGridView.add(createtoolsPanel(),cc.xy(1,2));
      addFillComponents(_mainGridView,new int[]{ 1,2 },new int[]{ 1,2 });
      return _mainGridView;
   }

   public JPanel createnwwPanel()
   {
      _nwwPanel.setName("nwwPanel");
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","CENTER:DEFAULT:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      _nwwPanel.setLayout(formlayout1);

      addFillComponents(_nwwPanel,new int[]{ 1 },new int[]{ 1 });
      return _nwwPanel;
   }

   public JPanel createlayersPanel()
   {
      _layersPanel.setName("layersPanel");
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","CENTER:DEFAULT:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      _layersPanel.setLayout(formlayout1);

      addFillComponents(_layersPanel,new int[]{ 1 },new int[]{ 1 });
      return _layersPanel;
   }

   public JPanel createtoolsPanel()
   {
      _toolsPanel.setName("toolsPanel");
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","FILL:DEFAULT:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      _toolsPanel.setLayout(formlayout1);

      addFillComponents(_toolsPanel,new int[]{ 1 },new int[]{ 1 });
      return _toolsPanel;
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
