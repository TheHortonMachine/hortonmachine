package org.hortonmachine.jython_tmp;
//package org.hortonmachine.jython;
//
//import com.jgoodies.forms.layout.CellConstraints;
//import com.jgoodies.forms.layout.FormLayout;
//import java.awt.BorderLayout;
//import java.awt.Color;
//import java.awt.ComponentOrientation;
//import java.awt.Container;
//import java.awt.Dimension;
//import javax.swing.Box;
//import javax.swing.ImageIcon;
//import javax.swing.JButton;
//import javax.swing.JFrame;
//import javax.swing.JLabel;
//import javax.swing.JPanel;
//import javax.swing.JTextField;
//import javax.swing.JTextPane;
//import javax.swing.border.EtchedBorder;
//
//
//public class SimpleRunnerGui extends JPanel
//{
//   JLabel _runFileLabel = new JLabel();
//   JTextField _filePathField = new JTextField();
//   JButton _filePathButton = new JButton();
//   JLabel _runScriptLabel = new JLabel();
//   JButton _runButton = new JButton();
//   JButton _closeButton = new JButton();
//   JTextPane _scriptArea = new JTextPane();
//
//   /**
//    * Default constructor
//    */
//   public SimpleRunnerGui()
//   {
//      initializePanel();
//   }
//
//   /**
//    * Adds fill components to empty cells in the first row and first column of the grid.
//    * This ensures that the grid spacing will be the same as shown in the designer.
//    * @param cols an array of column indices in the first row where fill components should be added.
//    * @param rows an array of row indices in the first column where fill components should be added.
//    */
//   void addFillComponents( Container panel, int[] cols, int[] rows )
//   {
//      Dimension filler = new Dimension(10,10);
//
//      boolean filled_cell_11 = false;
//      CellConstraints cc = new CellConstraints();
//      if ( cols.length > 0 && rows.length > 0 )
//      {
//         if ( cols[0] == 1 && rows[0] == 1 )
//         {
//            /** add a rigid area  */
//            panel.add( Box.createRigidArea( filler ), cc.xy(1,1) );
//            filled_cell_11 = true;
//         }
//      }
//
//      for( int index = 0; index < cols.length; index++ )
//      {
//         if ( cols[index] == 1 && filled_cell_11 )
//         {
//            continue;
//         }
//         panel.add( Box.createRigidArea( filler ), cc.xy(cols[index],1) );
//      }
//
//      for( int index = 0; index < rows.length; index++ )
//      {
//         if ( rows[index] == 1 && filled_cell_11 )
//         {
//            continue;
//         }
//         panel.add( Box.createRigidArea( filler ), cc.xy(1,rows[index]) );
//      }
//
//   }
//
//   /**
//    * Helper method to load an image file from the CLASSPATH
//    * @param imageName the package and name of the file to load relative to the CLASSPATH
//    * @return an ImageIcon instance with the specified image file
//    * @throws IllegalArgumentException if the image resource cannot be loaded.
//    */
//   public ImageIcon loadImage( String imageName )
//   {
//      try
//      {
//         ClassLoader classloader = getClass().getClassLoader();
//         java.net.URL url = classloader.getResource( imageName );
//         if ( url != null )
//         {
//            ImageIcon icon = new ImageIcon( url );
//            return icon;
//         }
//      }
//      catch( Exception e )
//      {
//         e.printStackTrace();
//      }
//      throw new IllegalArgumentException( "Unable to load image: " + imageName );
//   }
//
//   /**
//    * Method for recalculating the component orientation for 
//    * right-to-left Locales.
//    * @param orientation the component orientation to be applied
//    */
//   public void applyComponentOrientation( ComponentOrientation orientation )
//   {
//      // Not yet implemented...
//      // I18NUtils.applyComponentOrientation(this, orientation);
//      super.applyComponentOrientation(orientation);
//   }
//
//   public JPanel createPanel()
//   {
//      JPanel jpanel1 = new JPanel();
//      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,CENTER:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:GROW(1.0),CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
//      CellConstraints cc = new CellConstraints();
//      jpanel1.setLayout(formlayout1);
//
//      _runFileLabel.setName("runFileLabel");
//      _runFileLabel.setText("Run script from file");
//      jpanel1.add(_runFileLabel,cc.xywh(2,2,18,1));
//
//      _filePathField.setName("filePathField");
//      jpanel1.add(_filePathField,cc.xywh(3,4,15,1));
//
//      _filePathButton.setActionCommand("...");
//      _filePathButton.setName("filePathButton");
//      _filePathButton.setText("...");
//      jpanel1.add(_filePathButton,cc.xy(19,4));
//
//      _runScriptLabel.setName("runScriptLabel");
//      _runScriptLabel.setText("Run script");
//      jpanel1.add(_runScriptLabel,cc.xywh(2,7,18,1));
//
//      jpanel1.add(createPanel1(),cc.xywh(2,19,18,1));
//      jpanel1.add(createPanel2(),cc.xywh(3,9,17,9));
//      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20 });
//      return jpanel1;
//   }
//
//   public JPanel createPanel1()
//   {
//      JPanel jpanel1 = new JPanel();
//      FormLayout formlayout1 = new FormLayout("RIGHT:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,LEFT:DEFAULT:GROW(1.0)","CENTER:DEFAULT:NONE");
//      CellConstraints cc = new CellConstraints();
//      jpanel1.setLayout(formlayout1);
//
//      _runButton.setActionCommand("Run");
//      _runButton.setName("runButton");
//      _runButton.setText("Run");
//      jpanel1.add(_runButton,cc.xy(1,1));
//
//      _closeButton.setActionCommand("Close");
//      _closeButton.setName("closeButton");
//      _closeButton.setText("Close");
//      jpanel1.add(_closeButton,cc.xy(3,1));
//
//      addFillComponents(jpanel1,new int[]{ 2 },new int[0]);
//      return jpanel1;
//   }
//
//   public JPanel createPanel2()
//   {
//      JPanel jpanel1 = new JPanel();
//      EtchedBorder etchedborder1 = new EtchedBorder(EtchedBorder.LOWERED,null,null);
//      jpanel1.setBorder(etchedborder1);
//      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0)","CENTER:DEFAULT:GROW(1.0)");
//      CellConstraints cc = new CellConstraints();
//      jpanel1.setLayout(formlayout1);
//
//      _scriptArea.setName("scriptArea");
//      jpanel1.add(_scriptArea,new CellConstraints(1,1,1,1,CellConstraints.FILL,CellConstraints.FILL));
//
//      addFillComponents(jpanel1,new int[0],new int[0]);
//      return jpanel1;
//   }
//
//   /**
//    * Initializer
//    */
//   protected void initializePanel()
//   {
//      setLayout(new BorderLayout());
//      add(createPanel(), BorderLayout.CENTER);
//   }
//
//
//}
