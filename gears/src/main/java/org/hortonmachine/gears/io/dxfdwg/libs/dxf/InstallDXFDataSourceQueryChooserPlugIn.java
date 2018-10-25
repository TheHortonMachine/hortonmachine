package org.hortonmachine.gears.io.dxfdwg.libs.dxf;
///*
// * Library name : dxf
// * (C) 2006 Micha�l Michaud
// * 
// * This program is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// * 
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// * 
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// * 
// * For more information, contact:
// *
// * michael.michaud@free.fr
// *
// */
//
//package fr.michaelm.jump.drivers.dxf;
//
//import javax.swing.JFileChooser;
//
//import org.locationtech.jts.util.Assert;
//import org.locationtech.jump.workbench.datasource.InstallStandardDataSourceQueryChoosersPlugIn;
//import org.locationtech.jump.util.Blackboard;
//import org.locationtech.jump.workbench.plugin.AbstractPlugIn;
//import org.locationtech.jump.workbench.plugin.PlugInContext;
//import org.locationtech.jump.workbench.ui.GUIUtil;
//import org.locationtech.jump.io.JUMPWriter;
//import org.locationtech.jump.io.JUMPReader;
//import org.locationtech.jump.workbench.datasource.LoadFileDataSourceQueryChooser;
//import org.locationtech.jump.workbench.datasource.SaveFileDataSourceQueryChooser;
//import org.locationtech.jump.workbench.datasource.DataSourceQueryChooserManager;
//import org.locationtech.jump.workbench.WorkbenchContext;
//
//
///**
// * Install the DXF driver.
// * Extends the InstallStandardDataSourceQueryChoosersPlugIn class, overloading
// * initialize() to initialize DxfReader, DxfWriter.
// * @author Micha�l Michaud
// * @version 0.5.0
// */
//// History
//// 2006-10-18 : replace standard SaveFileDataSourceQueryChooser by a
//// SaveDxfFileDataSourceQueryChooser with options for header for entity
//// handles and for layer name.
//public class InstallDXFDataSourceQueryChooserPlugIn extends InstallStandardDataSourceQueryChoosersPlugIn {
//
//    private void addFileDataSourceQueryChoosers(
//        JUMPReader reader,
//        JUMPWriter writer,
//        final String description,
//        WorkbenchContext workbenchContext,
//        Class readerWriterDataSourceClass) {
//        DataSourceQueryChooserManager.get(
//            workbenchContext.getBlackboard())
//                .addLoadDataSourceQueryChooser(new LoadFileDataSourceQueryChooser(
//                    readerWriterDataSourceClass,
//                    description,
//                    extensions(readerWriterDataSourceClass),
//                    workbenchContext) {
//            protected void addFileFilters(JFileChooser chooser) {
//                super.addFileFilters(chooser);
//                InstallStandardDataSourceQueryChoosersPlugIn.addCompressedFileFilter(
//                    description,
//                    chooser);
//            }
//        }).addSaveDataSourceQueryChooser(
//            new SaveDxfFileDataSourceQueryChooser(
//                readerWriterDataSourceClass,
//                description,
//                extensions(readerWriterDataSourceClass),
//                workbenchContext));
//    }
//
//    public void initialize(final PlugInContext context) throws Exception {
//        Blackboard blackboard =
//            context.getWorkbenchContext().getWorkbench().getBlackboard();
//        addFileDataSourceQueryChoosers(
//            new DxfReader(),
//            new DxfWriter(),
//            "DXF",
//            context.getWorkbenchContext(),
//            DXFFileReaderWriter.class);
//    }
//
//}
