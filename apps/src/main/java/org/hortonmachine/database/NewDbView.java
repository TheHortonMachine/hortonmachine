package org.hortonmachine.database;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;


public class NewDbView extends JPanel
{
   JLabel _dbLabel = new JLabel();
   JLabel _userLabel = new JLabel();
   JLabel _pwdLabel = new JLabel();
   JTextField _dbTextField = new JTextField();
   JButton _browseButton = new JButton();
   JTextField _userTextField = new JTextField();
   JPasswordField _pwdTextField = new JPasswordField();
   JButton _connectButton = new JButton();
   JButton _cancelButton = new JButton();
   JLabel _dbTypeLabel = new JLabel();
   JComboBox _dbTypeCombo = new JComboBox();
   JLabel _extLabel = new JLabel();
   JTextField _extTextField = new JTextField();
   JCheckBox _connectRemoteCheck = new JCheckBox();

   /**
    * Default constructor
    */
   public NewDbView()
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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:GROW(0.5),CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:GROW(0.5)");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _dbLabel.setName("dbLabel");
      _dbLabel.setText("Database Path");
      _dbLabel.setHorizontalAlignment(JLabel.TRAILING);
      jpanel1.add(_dbLabel,cc.xy(2,2));

      _userLabel.setName("userLabel");
      _userLabel.setText("Username");
      _userLabel.setHorizontalAlignment(JLabel.TRAILING);
      jpanel1.add(_userLabel,cc.xy(2,6));

      _pwdLabel.setName("pwdLabel");
      _pwdLabel.setText("Password");
      _pwdLabel.setHorizontalAlignment(JLabel.TRAILING);
      jpanel1.add(_pwdLabel,cc.xy(2,8));

      _dbTextField.setName("dbTextField");
      jpanel1.add(_dbTextField,cc.xywh(4,2,13,1));

      _browseButton.setActionCommand("...");
      _browseButton.setName("browseButton");
      _browseButton.setText("...");
      jpanel1.add(_browseButton,cc.xy(18,2));

      _userTextField.setName("userTextField");
      jpanel1.add(_userTextField,cc.xywh(4,6,13,1));

      _pwdTextField.setName("pwdTextField");
      jpanel1.add(_pwdTextField,cc.xywh(4,8,13,1));

      jpanel1.add(createPanel1(),cc.xywh(2,12,18,1));
      _dbTypeLabel.setName("dbTypeLabel");
      _dbTypeLabel.setText("Database Type");
      _dbTypeLabel.setHorizontalAlignment(JLabel.TRAILING);
      jpanel1.add(_dbTypeLabel,cc.xy(2,4));

      _dbTypeCombo.setName("dbTypeCombo");
      jpanel1.add(_dbTypeCombo,cc.xywh(4,4,5,1));

      _extLabel.setName("extLabel");
      _extLabel.setText("Extension (usually)");
      jpanel1.add(_extLabel,cc.xy(11,4));

      _extTextField.setName("extTextField");
      jpanel1.add(_extTextField,cc.xywh(13,4,4,1));

      _connectRemoteCheck.setActionCommand("connect in network mode? (port 9092)");
      _connectRemoteCheck.setName("connectRemoteCheck");
      _connectRemoteCheck.setText("connect in network mode? (port 9092)");
      jpanel1.add(_connectRemoteCheck,cc.xywh(4,10,5,1));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13 });
      return jpanel1;
   }

   public JPanel createPanel1()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("RIGHT:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,LEFT:DEFAULT:GROW(1.0)","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _connectButton.setActionCommand("connect");
      _connectButton.setName("connectButton");
      _connectButton.setText("connect");
      jpanel1.add(_connectButton,cc.xy(1,1));

      _cancelButton.setActionCommand("cancel");
      _cancelButton.setName("cancelButton");
      _cancelButton.setText("cancel");
      jpanel1.add(_cancelButton,cc.xy(3,1));

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
