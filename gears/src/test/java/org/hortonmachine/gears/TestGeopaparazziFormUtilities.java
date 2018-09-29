/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
 */
package org.hortonmachine.gears;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.hortonmachine.gears.io.geopaparazzi.forms.Form;
import org.hortonmachine.gears.io.geopaparazzi.forms.MainFrame;
import org.hortonmachine.gears.io.geopaparazzi.forms.Section;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemBoolean;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemCombo;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemConnectedCombo;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemDate;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemDouble;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemInteger;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemText;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemTime;
import org.hortonmachine.gears.utils.HMTestCase;
/**
 * Test Geopaparazzi Form Utilities.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestGeopaparazziFormUtilities extends HMTestCase {

    public void testGeopaparazziFormUtilities() throws Exception {
        MainFrame mainFrame = new MainFrame();
        Section section = new Section("examples", "examples of supported form widgets");
        mainFrame.addSection(section);

        Form textForm = new Form("text");
        section.addForms(textForm);
        ItemText someText = new ItemText(null, "some text", "", false, false);
        textForm.addItem(someText);

        Form numericForm = new Form("numeric text");
        section.addForms(numericForm);
        ItemDouble doubleText = new ItemDouble(null, "a number", null, false, false, null, null);
        numericForm.addItem(doubleText);
        ItemInteger intText = new ItemInteger(null, "an integer number", null, false, false, null, null);
        numericForm.addItem(intText);

        Form dateForm = new Form("date");
        section.addForms(dateForm);
        ItemDate dateText = new ItemDate(null, "a date", null, false);
        dateForm.addItem(dateText);

        Form timeForm = new Form("time");
        section.addForms(timeForm);
        ItemTime timeText = new ItemTime(null, "a time", null, false);
        timeForm.addItem(timeText);

        Form booleanForm = new Form("boolean");
        section.addForms(booleanForm);
        ItemBoolean booleanText = new ItemBoolean(null, "a boolean choice", null, false);
        booleanForm.addItem(booleanText);

        Form combosForm = new Form("combos");
        section.addForms(combosForm);
        String[] comboItems = {"choice 1", "choice 2", "choice 3", "choice 4", "choice 5"};
        ItemCombo simpleCombo = new ItemCombo(null, "a single choice combo", comboItems, null, false, false);
        combosForm.addItem(simpleCombo);

        String[] comboItems1 = {"choice 1 of 1", "choice 2 of 1", "choice 3 of 1", "choice 4 of 1", "choice 5 of 1"};
        String[] comboItems2 = {"choice 1 of 2", "choice 2 of 2", "choice 3 of 2", "choice 4 of 2", "choice 5 of 2"};
        LinkedHashMap<String, List<String>> dataMap = new LinkedHashMap<String, List<String>>();
        dataMap.put("items 1", Arrays.asList(comboItems1));
        dataMap.put("items 2", Arrays.asList(comboItems2));
        ItemConnectedCombo connectedCombo = new ItemConnectedCombo(null, "two connected combos", dataMap, null, false);
        combosForm.addItem(connectedCombo);

        String replaced = mainFrame.toString().replaceAll("\\s+", "").replaceAll("\"", "");
        String expectedWithoutSpacesAndQuotes = "[{sectionname:examples,sectiondescription:examplesofsupportedformwidgets,forms:[{formname:text,formitems:[{key:sometext,value:,type:string,mandatory:no}]},{formname:numerictext,formitems:[{key:anumber,value:,type:double,mandatory:no},{key:anintegernumber,value:,type:integer,mandatory:no}]},{formname:date,formitems:[{key:adate,value:,type:date,mandatory:no}]},{formname:time,formitems:[{key:atime,value:,type:time,mandatory:no}]},{formname:boolean,formitems:[{key:abooleanchoice,value:false,type:boolean,mandatory:no}]},{formname:combos,formitems:[{key:asinglechoicecombo,values:{items:[{item:},{item:choice1},{item:choice2},{item:choice3},{item:choice4},{item:choice5}]},value:,type:stringcombo,mandatory:no},{key:twoconnectedcombos,values:{items1:[{item:choice1of1},{item:choice2of1},{item:choice3of1},{item:choice4of1},{item:choice5of1}],items2:[{item:choice1of2},{item:choice2of2},{item:choice3of2},{item:choice4of2},{item:choice5of2}]},value:,type:connectedstringcombo,mandatory:no}]}]}]";
        assertEquals(expectedWithoutSpacesAndQuotes, replaced);
    }

}
