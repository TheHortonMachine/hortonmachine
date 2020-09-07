package org.hortonmachine.gui.settings;

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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;


public class SettingsView extends JPanel
{
   JTabbedPane _jtabbedpane1 = new JTabbedPane();
   JTextField _proxyHostField = new JTextField();
   JTextField _proxyPortField = new JTextField();
   JTextField _proxyUserField = new JTextField();
   JPasswordField _proxyPasswordField = new JPasswordField();
   JCheckBox _proxyCheckbox = new JCheckBox();
   JTextField _charsetTextField = new JTextField();
   JComboBox _orientationCombo = new JComboBox();
   JTextField _preferencesDbPAth = new JTextField();
   JButton _preferencesDbButton = new JButton();
   JTextField _sshKeyPathField = new JTextField();
   JButton _sshKeyButton = new JButton();
   JPasswordField _sshKeyPassphraseField = new JPasswordField();
   JTextField _spatialiteModPathField = new JTextField();
   JButton _spatialiteModButton = new JButton();

   /**
    * Default constructor
    */
   public SettingsView()
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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _jtabbedpane1.addTab("Proxy",null,createPanel1());
      _jtabbedpane1.addTab("Internationalization",null,createPanel3());
      _jtabbedpane1.addTab("Preferences",null,createPanel6());
      _jtabbedpane1.addTab("SSH",null,createPanel7());
      _jtabbedpane1.addTab("Native libs",null,createPanel8());
      jpanel1.add(_jtabbedpane1,cc.xy(2,2));

      addFillComponents(jpanel1,new int[]{ 1,2,3 },new int[]{ 1,2,3 });
      return jpanel1;
   }

   public JPanel createPanel1()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:GROW(1.0),CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      jpanel1.add(createPanel2(),cc.xy(2,2));
      addFillComponents(jpanel1,new int[]{ 1,2,3 },new int[]{ 1,2,3 });
      return jpanel1;
   }

   public JPanel createPanel2()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Proxy",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(0,0,0));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      JLabel jlabel1 = new JLabel();
      jlabel1.setText("host");
      jpanel1.add(jlabel1,cc.xy(2,4));

      _proxyHostField.setName("proxyHostField");
      jpanel1.add(_proxyHostField,cc.xy(4,4));

      JLabel jlabel2 = new JLabel();
      jlabel2.setText("port");
      jpanel1.add(jlabel2,cc.xy(2,6));

      _proxyPortField.setName("proxyPortField");
      jpanel1.add(_proxyPortField,cc.xy(4,6));

      _proxyUserField.setName("proxyUserField");
      jpanel1.add(_proxyUserField,cc.xy(4,8));

      JLabel jlabel3 = new JLabel();
      jlabel3.setText("user");
      jpanel1.add(jlabel3,cc.xy(2,8));

      JLabel jlabel4 = new JLabel();
      jlabel4.setText("password");
      jpanel1.add(jlabel4,cc.xy(2,10));

      _proxyPasswordField.setName("proxyPasswordField");
      jpanel1.add(_proxyPasswordField,cc.xy(4,10));

      _proxyCheckbox.setActionCommand("enable ssh tunnel");
      _proxyCheckbox.setName("proxyCheckbox");
      _proxyCheckbox.setText("enable proxy");
      jpanel1.add(_proxyCheckbox,cc.xywh(2,2,3,1));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11 });
      return jpanel1;
   }

   public JPanel createPanel3()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:GROW(1.0),CENTER:DEFAULT:NONE,CENTER:DEFAULT:GROW(1.0),CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      jpanel1.add(createPanel4(),cc.xy(2,2));
      jpanel1.add(createPanel5(),cc.xy(2,4));
      addFillComponents(jpanel1,new int[]{ 1,2,3 },new int[]{ 1,2,3,4,5 });
      return jpanel1;
   }

   public JPanel createPanel4()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Charset",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(0,0,0));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      JLabel jlabel1 = new JLabel();
      jlabel1.setText("Charset to use for some read/write (gform/shp)");
      jpanel1.add(jlabel1,cc.xy(2,1));

      _charsetTextField.setName("charsetTextField");
      jpanel1.add(_charsetTextField,cc.xy(4,1));

      addFillComponents(jpanel1,new int[]{ 1,3,5 },new int[]{ 1 });
      return jpanel1;
   }

   public JPanel createPanel5()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Component Orientation",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(0,0,0));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _orientationCombo.setName("orientationCombo");
      jpanel1.add(_orientationCombo,cc.xy(2,1));

      addFillComponents(jpanel1,new int[]{ 1,3 },new int[]{ 1 });
      return jpanel1;
   }

   public JPanel createPanel6()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:8DLU:GROW(1.0),FILL:DEFAULT:NONE,FILL:20DLU:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      JLabel jlabel1 = new JLabel();
      jlabel1.setText("Folder to store the preferences database");
      jpanel1.add(jlabel1,cc.xy(2,2));

      _preferencesDbPAth.setName("preferencesDbPAth");
      jpanel1.add(_preferencesDbPAth,cc.xy(4,2));

      _preferencesDbButton.setActionCommand("...");
      _preferencesDbButton.setName("preferencesDbButton");
      _preferencesDbButton.setText("...");
      jpanel1.add(_preferencesDbButton,cc.xy(6,2));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7 },new int[]{ 1,2,3 });
      return jpanel1;
   }

   public JPanel createPanel7()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:20DLU:NONE,FILL:DEFAULT:NONE,FILL:8DLU:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      JLabel jlabel1 = new JLabel();
      jlabel1.setText("Path to the private key (id_dsa)");
      jpanel1.add(jlabel1,cc.xy(2,2));

      _sshKeyPathField.setName("sshKeyPathField");
      jpanel1.add(_sshKeyPathField,cc.xy(4,2));

      _sshKeyButton.setActionCommand("...");
      _sshKeyButton.setName("sshKeyButton");
      _sshKeyButton.setText("...");
      jpanel1.add(_sshKeyButton,cc.xy(6,2));

      JLabel jlabel2 = new JLabel();
      jlabel2.setText("Passphrase");
      jpanel1.add(jlabel2,cc.xy(2,4));

      _sshKeyPassphraseField.setName("sshKeyPassphraseField");
      jpanel1.add(_sshKeyPassphraseField,cc.xy(4,4));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8 },new int[]{ 1,2,3,4,5 });
      return jpanel1;
   }

   public JPanel createPanel8()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:20DLU:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      JLabel jlabel1 = new JLabel();
      jlabel1.setText("Path to spatialite loadable modules");
      jpanel1.add(jlabel1,cc.xy(2,2));

      _spatialiteModPathField.setName("spatialiteModPathField");
      jpanel1.add(_spatialiteModPathField,cc.xy(4,2));

      _spatialiteModButton.setActionCommand("...");
      _spatialiteModButton.setName("spatialiteModButton");
      _spatialiteModButton.setText("...");
      jpanel1.add(_spatialiteModButton,cc.xy(6,2));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8 },new int[]{ 1,2,3 });
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
