package org.hortonmachine.gui.utils;

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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;


public class SshTesterView extends JPanel
{
   JTextField _hostField = new JTextField();
   JTextArea _outputArea = new JTextArea();
   JTextField _commandField = new JTextField();
   JButton _commandButton = new JButton();
   JTextField _userField = new JTextField();
   JPasswordField _passwordField = new JPasswordField();
   JLabel _proxyLabel = new JLabel();
   JTextField _portField = new JTextField();
   JTextField _remoteTunnelHostField = new JTextField();
   JTextField _remoteTunnelPortField = new JTextField();
   JTextField _localTunnelPortField = new JTextField();
   JTextField _tunnelUserField = new JTextField();
   JPasswordField _tunnelPasswordField = new JPasswordField();
   JToggleButton _toggleTunnelButton = new JToggleButton();
   JLabel _sshKeyLabel = new JLabel();

   /**
    * Default constructor
    */
   public SshTesterView()
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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:GROW(0.5),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:GROW(1.0),CENTER:DEFAULT:NONE,FILL:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _hostField.setName("hostField");
      jpanel1.add(_hostField,cc.xy(4,2));

      JLabel jlabel1 = new JLabel();
      jlabel1.setText("command");
      jpanel1.add(jlabel1,cc.xy(7,2));

      _outputArea.setName("outputArea");
      EtchedBorder etchedborder1 = new EtchedBorder(EtchedBorder.RAISED,null,null);
      _outputArea.setBorder(etchedborder1);
      JScrollPane jscrollpane1 = new JScrollPane();
      jscrollpane1.setViewportView(_outputArea);
      jscrollpane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      jscrollpane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      jpanel1.add(jscrollpane1,cc.xywh(7,6,5,35));

      _commandField.setName("commandField");
      jpanel1.add(_commandField,cc.xy(9,2));

      _commandButton.setActionCommand(" run ");
      _commandButton.setName("commandButton");
      _commandButton.setText(" run ");
      jpanel1.add(_commandButton,cc.xy(11,2));

      JLabel jlabel2 = new JLabel();
      jlabel2.setText("user");
      jpanel1.add(jlabel2,cc.xy(2,6));

      _userField.setName("userField");
      jpanel1.add(_userField,cc.xy(4,6));

      JLabel jlabel3 = new JLabel();
      jlabel3.setText("password");
      jpanel1.add(jlabel3,cc.xy(2,8));

      _passwordField.setName("passwordField");
      jpanel1.add(_passwordField,cc.xy(4,8));

      _proxyLabel.setName("proxyLabel");
      _proxyLabel.setText("   ");
      jpanel1.add(_proxyLabel,cc.xywh(2,10,3,1));

      JLabel jlabel4 = new JLabel();
      jlabel4.setText("host");
      jpanel1.add(jlabel4,cc.xy(2,2));

      JLabel jlabel5 = new JLabel();
      jlabel5.setText("port");
      jpanel1.add(jlabel5,cc.xy(2,4));

      _portField.setName("portField");
      jpanel1.add(_portField,cc.xy(4,4));

      jpanel1.add(createPanel1(),cc.xywh(2,15,3,26));
      _sshKeyLabel.setName("sshKeyLabel");
      jpanel1.add(_sshKeyLabel,cc.xywh(2,12,3,1));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41 });
      return jpanel1;
   }

   public JPanel createPanel1()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"Tunneling",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(0,0,0));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      JLabel jlabel1 = new JLabel();
      jlabel1.setText("remote host");
      jpanel1.add(jlabel1,cc.xy(2,2));

      _remoteTunnelHostField.setName("remoteTunnelHostField");
      jpanel1.add(_remoteTunnelHostField,cc.xy(4,2));

      JLabel jlabel2 = new JLabel();
      jlabel2.setText("remote port");
      jpanel1.add(jlabel2,cc.xy(2,4));

      _remoteTunnelPortField.setName("remoteTunnelPortField");
      jpanel1.add(_remoteTunnelPortField,cc.xy(4,4));

      JLabel jlabel3 = new JLabel();
      jlabel3.setText("local port");
      jpanel1.add(jlabel3,cc.xy(2,6));

      _localTunnelPortField.setName("localTunnelPortField");
      jpanel1.add(_localTunnelPortField,cc.xy(4,6));

      JLabel jlabel4 = new JLabel();
      jlabel4.setText("remote user");
      jpanel1.add(jlabel4,cc.xy(2,8));

      _tunnelUserField.setName("tunnelUserField");
      jpanel1.add(_tunnelUserField,cc.xy(4,8));

      _tunnelPasswordField.setName("tunnelPasswordField");
      jpanel1.add(_tunnelPasswordField,cc.xy(4,10));

      JLabel jlabel5 = new JLabel();
      jlabel5.setText("remote password");
      jpanel1.add(jlabel5,cc.xy(2,10));

      jpanel1.add(createPanel2(),cc.xywh(2,12,3,1));
      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12 });
      return jpanel1;
   }

   public JPanel createPanel2()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:GROW(1.0)","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _toggleTunnelButton.setActionCommand("create tunnel");
      _toggleTunnelButton.setName("toggleTunnelButton");
      _toggleTunnelButton.setText("create tunnel");
      jpanel1.add(_toggleTunnelButton,cc.xy(2,1));

      addFillComponents(jpanel1,new int[]{ 1,3 },new int[]{ 1 });
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
