package org.hortonmachine.gps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.TitledBorder;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class GpsInfoView extends JPanel
{
   JTextField _gpsFixStatusText = new JTextField();
   JTextField _gpsFixQualityText = new JTextField();
   JTextField _dataStatusText = new JTextField();
   JTextField _faaModeText = new JTextField();
   JTextField _hdopText = new JTextField();
   JTextField _vdopText = new JTextField();
   JTextField _pdopText = new JTextField();
   JTextField _tsText = new JTextField();
   JTextField _speedText = new JTextField();
   JTextField _satCountText = new JTextField();
   JTextField _satIdsText = new JTextField();
   JTextField _satInfoCountText = new JTextField();
   JTextField _lonText = new JTextField();
   JTextField _latText = new JTextField();
   JTextField _pointCountText = new JTextField();
   JTextField _humidityText = new JTextField();
   JTextField _temperatureText = new JTextField();
   JPanel _mapPanel = new JPanel();
   JCheckBox _followGPSCheck = new JCheckBox();
   JCheckBox _useFakeGpsCheck = new JCheckBox();
   JPanel _layersPanel = new JPanel();
   JToggleButton _gpsConnectButton = new JToggleButton();
   JComboBox _portsCombo = new JComboBox();
   JCheckBox _recordNmeaCheck = new JCheckBox();

   /**
    * Default constructor
    */
   public GpsInfoView()
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
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(0.2),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(0.4),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:GROW(1.0),CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      jpanel1.add(createPanel1(),cc.xywh(18,10,4,18));
      _mapPanel.setName("mapPanel");
      jpanel1.add(_mapPanel,cc.xywh(4,2,13,26));

      _followGPSCheck.setActionCommand("follow GPS");
      _followGPSCheck.setName("followGPSCheck");
      _followGPSCheck.setText("follow GPS");
      jpanel1.add(_followGPSCheck,cc.xy(19,6));

      _useFakeGpsCheck.setActionCommand("follow GPS");
      _useFakeGpsCheck.setName("useFakeGpsCheck");
      _useFakeGpsCheck.setText("use fake GPS (triggers connection)");
      jpanel1.add(_useFakeGpsCheck,cc.xy(19,4));

      _layersPanel.setName("layersPanel");
      jpanel1.add(_layersPanel,cc.xywh(2,2,1,26));

      jpanel1.add(createPanel2(),cc.xywh(18,2,4,1));
      _recordNmeaCheck.setActionCommand("follow GPS");
      _recordNmeaCheck.setName("recordNmeaCheck");
      _recordNmeaCheck.setText("record NMEA log (saved in home)");
      jpanel1.add(_recordNmeaCheck,cc.xy(19,8));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28 });
      return jpanel1;
   }

   public JPanel createPanel1()
   {
      JPanel jpanel1 = new JPanel();
      TitledBorder titledborder1 = new TitledBorder(null,"GPS INFO",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,null,new Color(33,33,33));
      jpanel1.setBorder(titledborder1);
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:2DLU:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      JLabel jlabel1 = new JLabel();
      jlabel1.setText("GPS fix status");
      jpanel1.add(jlabel1,cc.xy(2,4));

      _gpsFixStatusText.setName("gpsFixStatusText");
      jpanel1.add(_gpsFixStatusText,cc.xy(4,4));

      JLabel jlabel2 = new JLabel();
      jlabel2.setText("GPS fix quality");
      jpanel1.add(jlabel2,cc.xy(2,6));

      JLabel jlabel3 = new JLabel();
      jlabel3.setText("Data status");
      jpanel1.add(jlabel3,cc.xy(2,8));

      JLabel jlabel4 = new JLabel();
      jlabel4.setText("Faa mode");
      jpanel1.add(jlabel4,cc.xy(2,10));

      JLabel jlabel5 = new JLabel();
      jlabel5.setText("Horizontal precision");
      jpanel1.add(jlabel5,cc.xy(2,16));

      JLabel jlabel6 = new JLabel();
      jlabel6.setText("Vertical precision");
      jpanel1.add(jlabel6,cc.xy(2,18));

      JLabel jlabel7 = new JLabel();
      jlabel7.setText("Position precision");
      jpanel1.add(jlabel7,cc.xy(2,20));

      JLabel jlabel8 = new JLabel();
      jlabel8.setText("Timestamp");
      jpanel1.add(jlabel8,cc.xy(2,22));

      JLabel jlabel9 = new JLabel();
      jlabel9.setText("Speed [Km/h]");
      jpanel1.add(jlabel9,cc.xy(2,24));

      JLabel jlabel10 = new JLabel();
      jlabel10.setText("Satellites count");
      jpanel1.add(jlabel10,cc.xy(2,26));

      JLabel jlabel11 = new JLabel();
      jlabel11.setText("Satellites ids");
      jpanel1.add(jlabel11,cc.xy(2,28));

      JLabel jlabel12 = new JLabel();
      jlabel12.setText("Satellites with info");
      jpanel1.add(jlabel12,cc.xy(2,30));

      _gpsFixQualityText.setName("gpsFixQualityText");
      jpanel1.add(_gpsFixQualityText,cc.xy(4,6));

      _dataStatusText.setName("dataStatusText");
      jpanel1.add(_dataStatusText,cc.xy(4,8));

      _faaModeText.setName("faaModeText");
      jpanel1.add(_faaModeText,cc.xy(4,10));

      _hdopText.setName("hdopText");
      jpanel1.add(_hdopText,cc.xy(4,16));

      _vdopText.setName("vdopText");
      jpanel1.add(_vdopText,cc.xy(4,18));

      _pdopText.setName("pdopText");
      jpanel1.add(_pdopText,cc.xy(4,20));

      _tsText.setName("tsText");
      jpanel1.add(_tsText,cc.xy(4,22));

      _speedText.setName("speedText");
      jpanel1.add(_speedText,cc.xy(4,24));

      _satCountText.setName("satCountText");
      jpanel1.add(_satCountText,cc.xy(4,26));

      _satIdsText.setName("satIdsText");
      jpanel1.add(_satIdsText,cc.xy(4,28));

      _satInfoCountText.setName("satInfoCountText");
      jpanel1.add(_satInfoCountText,cc.xy(4,30));

      JLabel jlabel13 = new JLabel();
      jlabel13.setText("Longitude");
      jpanel1.add(jlabel13,cc.xy(2,12));

      JLabel jlabel14 = new JLabel();
      jlabel14.setText("Latitude");
      jpanel1.add(jlabel14,cc.xy(2,14));

      _lonText.setName("lonText");
      jpanel1.add(_lonText,cc.xy(4,12));

      _latText.setName("latText");
      jpanel1.add(_latText,cc.xy(4,14));

      JLabel jlabel15 = new JLabel();
      jlabel15.setText("Event count");
      jpanel1.add(jlabel15,cc.xy(2,2));

      _pointCountText.setName("pointCountText");
      jpanel1.add(_pointCountText,cc.xy(4,2));

      JLabel jlabel16 = new JLabel();
      jlabel16.setText("Humidity [%]");
      jpanel1.add(jlabel16,cc.xy(2,32));

      _humidityText.setName("humidityText");
      jpanel1.add(_humidityText,cc.xy(4,32));

      JLabel jlabel17 = new JLabel();
      jlabel17.setText("Temperature [C]");
      jpanel1.add(jlabel17,cc.xy(2,34));

      _temperatureText.setName("temperatureText");
      jpanel1.add(_temperatureText,cc.xy(4,34));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35 });
      return jpanel1;
   }

   public JPanel createPanel2()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:DEFAULT:NONE,FILL:DEFAULT:NONE,FILL:4DLU:NONE","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _gpsConnectButton.setActionCommand("connect");
      _gpsConnectButton.setName("gpsConnectButton");
      _gpsConnectButton.setText("connect");
      jpanel1.add(_gpsConnectButton,cc.xy(4,1));

      _portsCombo.setName("portsCombo");
      jpanel1.add(_portsCombo,cc.xy(2,1));

      addFillComponents(jpanel1,new int[]{ 1,3,5 },new int[]{ 1 });
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
