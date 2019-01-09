/*******************************************************************************
 * Copyright (C) 2018 HydroloGIS S.r.l. (www.hydrologis.com)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Author: Antonello Andrea (http://www.hydrologis.com)
 ******************************************************************************/
package org.hortonmachine.gps.nmea;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gps.utils.CurrentGpsInfo;

import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.sentence.SentenceId;
import net.sf.marineapi.nmea.sentence.SentenceValidator;

/**
 * A fake NMEA GPS that reads data from file.
 *
 */
public class FakeNmeaGps extends ANmeaGps {

    private static final int GPS_INTERVAL = 300;

    private File dataFile;
    private List<String> dataLines;

    /**
     * Constructor.
     * 
     * @param dataFile the file to read NMEA strings from.
     */
    public FakeNmeaGps( File dataFile ) {
        this.dataFile = dataFile;
    }

    public void start() throws Exception {
        if (dataFile == null || !dataFile.exists()) {
            InputStream stream = FakeNmeaGps.class.getResourceAsStream("/defaultlog.nmea");
            try (java.util.Scanner s = new java.util.Scanner(stream)) {
                s.useDelimiter("\n");
                dataLines = new ArrayList<>();
                while( s.hasNext() ) {
                    dataLines.add(s.next());
                }
            }
        } else {
            dataLines = readFileToLinesList(dataFile);
        }

        int linesCount = dataLines.size();

        new Thread(new Runnable(){
            public void run() {
                for( int i = 0; i < linesCount; i++ ) {
                    if (i == linesCount - 1) {
                        i = 0;
                    }
                    if (doCancel) {
                        break;
                    }

                    String line = dataLines.get(i);
                    line = line.trim();
                    if (SentenceValidator.isValid(line)) {
                        if (line.contains("MTK001")) {
                            continue;
                        }
                        ByteArrayInputStream source = new ByteArrayInputStream(line.getBytes());
                        SentenceReader reader = new SentenceReader(source);
                        reader.addSentenceListener(FakeNmeaGps.this, SentenceId.GSA);
                        reader.addSentenceListener(FakeNmeaGps.this, SentenceId.GGA);
                        reader.addSentenceListener(FakeNmeaGps.this, SentenceId.GLL);
                        reader.addSentenceListener(FakeNmeaGps.this, SentenceId.GSV);
                        reader.addSentenceListener(FakeNmeaGps.this, SentenceId.RMC);
                        reader.start();
                    }

                    try {
                        Thread.sleep(GPS_INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    private List<String> readFileToLinesList( File file ) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while( (line = br.readLine()) != null ) {
                lines.add(line);
            }
            return lines;
        }
    }

    public static void main( String[] args ) throws Exception {
        FakeNmeaGps fg = new FakeNmeaGps(null);
        fg.start();

        fg.addListener(new NmeaGpsListener(){

            @Override
            public void onGpsEvent( CurrentGpsInfo gpsInfo ) {
                System.out.println(gpsInfo);
            }
        });

    }
}
