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
//import org.locationtech.jump.workbench.plugin.Extension;
//import org.locationtech.jump.workbench.plugin.PlugInContext;
//
///**
// * This is the entry class to declare the dxf driver to JUMP.
// * You can put the &lt;extension&gt;drivers.dxf.DXFDriverConfiguration&lt;/extension&gt;
// * element in the workbench-properties.xml file or put the .jar file containing
// * the driver in the ext directory of your installation.
// * @author Micha�l Michaud
// * @version 0.5
// */
//// History
//// 0.4 (2006-10-15) : makes it possible to export any JUMP layer (in 0.3,
////                    layers which were not issued from a dxf file could not be
////                    exported because it misses some attributes).
//// 0.3 (2003-12-10)
//// 0.2
//public class DXFDriverConfiguration extends Extension {
//    public void configure(PlugInContext context) throws Exception {
//        new InstallDXFDataSourceQueryChooserPlugIn().initialize(context);
//    }
//    public String getName() {return "DXF driver";}
//    public String getVersion() {return "0.6";}
// }
