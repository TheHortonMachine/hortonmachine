package org.hortonmachine.lidar;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.TitledBorder;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class LasInfoView extends JPanel
{
   JTextField _inputPathField = new JTextField();
   JButton _loadButton = new JButton();
   JTable _headerTable = new JTable();
   JTable _firstPointTable = new JTable();
   JTextField _dtmInputPathField = new JTextField();
   JButton _loadDtmButton = new JButton();
   JTextField _samplingField = new JTextField();
   JTextField _classesField = new JTextField();
   JTextField _impulsesField = new JTextField();
   JTextField _intensityRangeField = new JTextField();
   JTextField _lowerThresField = new JTextField();
   JTextField _upperThresField = new JTextField();
   JTextField _westField = new JTextField();
   JTextField _eastField = new JTextField();
   JTextField _southField = new JTextField();
   JTextField _northField = new JTextField();
   JTextField _minZField = new JTextField();
   JTextField _maxZField = new JTextField();
   JButton _boundsLoadButton = new JButton();
   JToggleButton _boundsSelectButton = new JToggleButton();
   JButton _boundsClearButton = new JButton();
   JButton _loadDataButton = new JButton();
   JRadioButton _elevationRadio = new JRadioButton();
   ButtonGroup _buttongroup1 = new ButtonGroup();
   JRadioButton _intensityRadio = new JRadioButton();
   JRadioButton _classRadio = new JRadioButton();
   JRadioButton _impulseRadio = new JRadioButton();
   JRadioButton _ownColorRadio = new JRadioButton();
   JButton _loadPreviewButton = new JButton();
   /** The container of the 3D viewer, filled by the controller. */
   JPanel _viewerPanel = new JPanel(new BorderLayout());

   /**
    * Default constructor
    */
   public LasInfoView()
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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:190DLU:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      jpanel1.add(createPanel1(),cc.xy(2,2));
      jpanel1.add(createPanel9(),cc.xy(5,2));
      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6 },new int[]{ 1,2,3 });
      return jpanel1;
   }

   public JPanel createPanel1()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:60DLU:GROW(0.4),CENTER:2DLU:NONE,FILL:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      jpanel1.add(createPanel2(),cc.xywh(2,1,3,1));
      // header and first point in tabs, taking all the free height of the column
      jpanel1.add(createInfoTabs(),cc.xywh(2,5,3,2));
      jpanel1.add(createPanel5(),cc.xywh(2,3,3,1));
      jpanel1.add(createPanel6(),cc.xywh(2,8,3,1));
      // reading the file and filtering again what was read, side by side
      JPanel loadButtons = new JPanel(new java.awt.GridLayout(1, 2, 4, 0));
      _loadDataButton.setActionCommand("Load Preview");
      _loadDataButton.setName("loadDataButton");
      _loadDataButton.setText("Load data");
      _loadDataButton.setToolTipText("Read the file, with sampling and filters");
      loadButtons.add(_loadDataButton);
      _loadPreviewButton.setActionCommand("Apply filters");
      _loadPreviewButton.setName("loadPreviewButton");
      _loadPreviewButton.setText("Refilter loaded");
      _loadPreviewButton.setToolTipText("<html>Apply the filters again to the points already read, without reading the file.<br>"
            + "It can only keep points that were read, and computes the heights over a DTM set later.</html>");
      loadButtons.add(_loadPreviewButton);
      jpanel1.add(loadButtons,cc.xywh(2,10,3,1));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11 });
      return jpanel1;
   }

   public JPanel createPanel2()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Las/Laz input file",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,null);
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:32DLU:GROW(1.0),FILL:DEFAULT:NONE,FILL:32DLU:NONE","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _inputPathField.setName("inputPathField");
      jpanel1.add(_inputPathField,cc.xy(1,1));

      _loadButton.setActionCommand("load");
      _loadButton.setName("loadButton");
      _loadButton.setText("...");
      jpanel1.add(_loadButton,cc.xy(3,1));

      addFillComponents(jpanel1,new int[]{ 2 },new int[0]);
      return jpanel1;
   }

   /**
    * The header and first point tables, as tabs (no longer in createPanel3/createPanel4,
    * where they got too little height).
    */
   public JTabbedPane createInfoTabs()
   {
      JTabbedPane tabs = new JTabbedPane();
      // the default viewport of the tables asks 450 pixels of width, more than the fixed width
      // of the column, which pushed all the fields of the column out of their panels
      _headerTable.setPreferredScrollableViewportSize(new Dimension(100, 100));
      _firstPointTable.setPreferredScrollableViewportSize(new Dimension(100, 100));
      _headerTable.setName("headerTable");
      tabs.addTab("Header", new JScrollPane(_headerTable));
      _firstPointTable.setName("firstPointTable");
      tabs.addTab("First point", new JScrollPane(_firstPointTable));
      return tabs;
   }

   public JPanel createPanel3()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Header Information",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,null);
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","FILL:DEFAULT:GROW(1.0)");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _headerTable.setName("headerTable");
      JScrollPane jscrollpane1 = new JScrollPane();
      jscrollpane1.setViewportView(_headerTable);
      jscrollpane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      jscrollpane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      jpanel1.add(jscrollpane1,cc.xy(1,1));

      addFillComponents(jpanel1,new int[0],new int[0]);
      return jpanel1;
   }

   public JPanel createPanel4()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"First Point Information",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,null);
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","FILL:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _firstPointTable.setName("firstPointTable");
      JScrollPane jscrollpane1 = new JScrollPane();
      jscrollpane1.setViewportView(_firstPointTable);
      jscrollpane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      jscrollpane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      jpanel1.add(jscrollpane1,cc.xy(1,1));

      addFillComponents(jpanel1,new int[0],new int[0]);
      return jpanel1;
   }

   public JPanel createPanel5()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Optional DTM for delta",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,null);
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:32DLU:GROW(1.0),FILL:DEFAULT:NONE,FILL:32DLU:NONE","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _dtmInputPathField.setName("dtmInputPathField");
      jpanel1.add(_dtmInputPathField,cc.xy(1,1));

      _loadDtmButton.setActionCommand("load");
      _loadDtmButton.setName("loadDtmButton");
      _loadDtmButton.setText("...");
      jpanel1.add(_loadDtmButton,cc.xy(3,1));

      addFillComponents(jpanel1,new int[]{ 2 },new int[0]);
      return jpanel1;
   }

   public JPanel createPanel6()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Filters",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,null);
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,FILL:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      jpanel1.add(createPanel7(),cc.xy(1,1));
      jpanel1.add(createPanel8(),cc.xy(1,4));
      addFillComponents(jpanel1,new int[]{ 1 },new int[]{ 1,2,3,4 });
      return jpanel1;
   }

   public JPanel createPanel7()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,LEFT:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0)","FILL:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      JLabel jlabel1 = new JLabel();
      jlabel1.setText("sampling");
      jpanel1.add(jlabel1,cc.xy(2,1));

      _samplingField.setName("samplingField");
      jpanel1.add(_samplingField,cc.xy(4,1));

      JLabel jlabel2 = new JLabel();
      jlabel2.setText("classes");
      jpanel1.add(jlabel2,cc.xy(2,3));

      JLabel jlabel3 = new JLabel();
      jlabel3.setText("impulses");
      jpanel1.add(jlabel3,cc.xy(2,5));

      _classesField.setName("classesField");
      jpanel1.add(_classesField,cc.xy(4,3));

      _impulsesField.setName("impulsesField");
      jpanel1.add(_impulsesField,cc.xy(4,5));

      JLabel jlabel4 = new JLabel();
      jlabel4.setText("min/max intensity");
      jpanel1.add(jlabel4,cc.xy(2,7));

      _intensityRangeField.setName("intensityRangeField");
      jpanel1.add(_intensityRangeField,cc.xy(4,7));

      JLabel jlabel5 = new JLabel();
      jlabel5.setText("DTM lower thres");
      jpanel1.add(jlabel5,cc.xy(2,9));

      _lowerThresField.setName("lowerThresField");
      jpanel1.add(_lowerThresField,cc.xy(4,9));

      JLabel jlabel6 = new JLabel();
      jlabel6.setText("DTM upper thres");
      jpanel1.add(jlabel6,cc.xy(2,11));

      _upperThresField.setName("upperThresField");
      jpanel1.add(_upperThresField,cc.xy(4,11));

      addFillComponents(jpanel1,new int[]{ 1,3 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12 });
      return jpanel1;
   }

   public JPanel createPanel8()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Bounds",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,null);
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:4DLU:NONE,FILL:24DLU:NONE","CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      JLabel jlabel1 = new JLabel();
      jlabel1.setText("West");
      jpanel1.add(jlabel1,cc.xy(2,1));

      JLabel jlabel2 = new JLabel();
      jlabel2.setText("East");
      jpanel1.add(jlabel2,cc.xy(2,3));

      _westField.setName("westField");
      jpanel1.add(_westField,cc.xywh(4,1,3,1));

      _eastField.setName("eastField");
      jpanel1.add(_eastField,cc.xywh(4,3,3,1));

      JLabel jlabel3 = new JLabel();
      jlabel3.setText("South");
      jpanel1.add(jlabel3,cc.xy(2,5));

      JLabel jlabel4 = new JLabel();
      jlabel4.setText("North");
      jpanel1.add(jlabel4,cc.xy(2,7));

      _southField.setName("southField");
      jpanel1.add(_southField,cc.xywh(4,5,3,1));

      _northField.setName("northField");
      jpanel1.add(_northField,cc.xywh(4,7,3,1));

      JLabel jlabel5 = new JLabel();
      jlabel5.setText("Min Z");
      jpanel1.add(jlabel5,cc.xy(2,9));

      JLabel jlabel6 = new JLabel();
      jlabel6.setText("Max Z");
      jpanel1.add(jlabel6,cc.xy(2,11));

      _minZField.setName("minZField");
      jpanel1.add(_minZField,cc.xywh(4,9,3,1));

      _maxZField.setName("maxZField");
      jpanel1.add(_maxZField,cc.xywh(4,11,3,1));

      // the bounds from a file, drawn on the map, or cleared, in one row
      // without a width of its own: three buttons as wide as the widest are more than the fixed
      // width of the column, and pushed all the fields of the column out of their panels
      JPanel boundsButtons = new JPanel(new java.awt.GridLayout(1, 3, 4, 0)){
         @Override
         public Dimension getPreferredSize() {
            return new Dimension(10, super.getPreferredSize().height);
         }

         @Override
         public Dimension getMinimumSize() {
            return getPreferredSize();
         }
      };
      _boundsLoadButton.setActionCommand("load");
      _boundsLoadButton.setName("boundsLoadButton");
      _boundsLoadButton.setText("From file");
      _boundsLoadButton.setToolTipText("Take the bounds from the extent of a vector file");
      boundsButtons.add(_boundsLoadButton);
      _boundsSelectButton.setName("boundsSelectButton");
      _boundsSelectButton.setText("Draw on map");
      _boundsSelectButton.setToolTipText("<html>When active, drag a box on the 3D view to set the bounds,<br>"
            + "then drag its sides or inside to adjust it.</html>");
      boundsButtons.add(_boundsSelectButton);
      _boundsClearButton.setName("boundsClearButton");
      _boundsClearButton.setText("Clear");
      _boundsClearButton.setToolTipText("Remove the bounds, to load the whole file");
      boundsButtons.add(_boundsClearButton);
      for( java.awt.Component button : boundsButtons.getComponents() ) {
         ((javax.swing.AbstractButton) button).setMargin(new java.awt.Insets(2, 4, 2, 4));
      }
      jpanel1.add(boundsButtons,cc.xywh(2,13,5,1));

      addFillComponents(jpanel1,new int[]{ 1,3,5,6 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14 });
      return jpanel1;
   }

   /**
    * The right side: the 3D viewer, taking all the space, and under it the coloring of the points.
    */
   public JPanel createPanel9()
   {
      JPanel jpanel1 = new JPanel(new BorderLayout(0, 4));
      _viewerPanel.setName("viewerPanel");
      jpanel1.add(_viewerPanel, BorderLayout.CENTER);
      jpanel1.add(createPanel10(), BorderLayout.SOUTH);
      return jpanel1;
   }

   /**
    * The coloring of the points, in one row. The value used for the coloring is also the one
    * filtered by the range slider of the viewer.
    */
   public JPanel createPanel10()
   {
      JPanel jpanel1 = new JPanel(new BorderLayout());
      TitledBorder titledborder1 = new TitledBorder(null,"Color and slice by",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,null);
      jpanel1.setBorder(titledborder1);

      JPanel radios = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
      _elevationRadio.setActionCommand("elevation");
      _elevationRadio.setName("elevationRadio");
      _elevationRadio.setText("elevation");
      _elevationRadio.setToolTipText("The elevation, or the height over the ground if a DTM is set");
      _buttongroup1.add(_elevationRadio);
      radios.add(_elevationRadio);

      _intensityRadio.setActionCommand("intensity");
      _intensityRadio.setName("intensityRadio");
      _intensityRadio.setText("intensity");
      _buttongroup1.add(_intensityRadio);
      radios.add(_intensityRadio);

      _classRadio.setActionCommand("classification");
      _classRadio.setName("classRadio");
      _classRadio.setText("classification");
      _buttongroup1.add(_classRadio);
      radios.add(_classRadio);

      _impulseRadio.setActionCommand("impulse");
      _impulseRadio.setName("impulseRadio");
      _impulseRadio.setText("impulse");
      _buttongroup1.add(_impulseRadio);
      radios.add(_impulseRadio);

      _ownColorRadio.setActionCommand("own color");
      _ownColorRadio.setName("ownColorRadio");
      _ownColorRadio.setText("own color");
      _ownColorRadio.setToolTipText("The colors of the points, sliced by elevation");
      _buttongroup1.add(_ownColorRadio);
      radios.add(_ownColorRadio);
      jpanel1.add(radios, BorderLayout.CENTER);
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
