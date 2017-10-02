/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.gears.io.dxfdwg.libs.dwg;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Vector;

import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgArc;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgAttdef;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgAttrib;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgBlock;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgBlockControl;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgBlockHeader;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgCircle;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgEllipse;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgEndblk;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgInsert;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgLayer;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgLayerControl;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgLine;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgLinearDimension;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgLwPolyline;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgMText;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgPoint;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgPolyline2D;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgPolyline3D;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgSeqend;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgSolid;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgSpline;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgText;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgVertex2D;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgVertex3D;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.utils.AcadExtrusionCalculator;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.utils.GisModelCurveCalculator;

/**
 * The DwgFile class provides a revision-neutral interface for reading and handling
 * DWG files
 * Reading methods are useful for reading DWG files, and handling methods like
 * calculateDwgPolylines() are useful for handling more complex
 * objects in the DWG file
 * 
 * @author jmorell
 */
public class DwgFile {

    private String fileName;
    private String dwgVersion;
    private Vector dwgSectionOffsets;
    private Vector dwgObjectOffsets;
    private Vector dwgObjects;
    private Vector dwgClasses;
    private DwgFileReader dwgReader;
    private Vector layerTable;
    private Vector layerNames;
    private boolean dwg3DFile;

    /**
     * Creates a new DwgFile object given the absolute path to
     * a DWG file
     * 
     * @param filePath an absolute path to the DWG file
     */
    public DwgFile( String filePath ) {
        this.fileName = filePath;
        dwgSectionOffsets = new Vector();
        dwgObjectOffsets = new Vector();
        dwgObjects = new Vector();
        dwgClasses = new Vector();
    }

    /**
     * Reads a DWG file and put its objects in the dwgObjects Vector
     * This method is version independent
     * 
     * @throws IOException If the file location is wrong
     */
    public void read() throws IOException {
        System.out.println("DwgFile.read() executed ...");
        setDwgVersion();
        if (dwgVersion.equals("R13")) {
            dwgReader = new DwgFileV14Reader();
            dwgReader.read(this);
        } else if (dwgVersion.equals("R14")) {
            dwgReader = new DwgFileV14Reader();
            dwgReader.read(this);
        } else if (dwgVersion.equals("R15")) {
            dwgReader = new DwgFileV15Reader();
            dwgReader.read(this);
        } else if (dwgVersion.equals("Unknown")) {
            throw new IOException("DWG version of the file is not supported.");
        }
    }

    /**
     * Modify the geometry of the objects applying the Extrusion vector where it
     * is necessary
     */
    public void applyExtrusions() {
        for( int i = 0; i < dwgObjects.size(); i++ ) {
            DwgObject dwgObject = (DwgObject) dwgObjects.get(i);
            if (dwgObject instanceof DwgArc) {
                double[] arcCenter = ((DwgArc) dwgObject).getCenter();
                double[] arcExt = ((DwgArc) dwgObject).getExtrusion();
                arcCenter = AcadExtrusionCalculator.CalculateAcadExtrusion(arcCenter, arcExt);
                ((DwgArc) dwgObject).setCenter(arcCenter);
            } else if (dwgObject instanceof DwgAttdef) {
                // Extrusion in DwgAttdef is not necessary
            } else if (dwgObject instanceof DwgAttrib) {
                Point2D attribInsertionPoint = ((DwgAttrib) dwgObject).getInsertionPoint();
                double attribElevation = ((DwgAttrib) dwgObject).getElevation();
                double[] attribInsertionPoint3D = new double[]{attribInsertionPoint.getX(),
                        attribInsertionPoint.getY(), attribElevation};
                double[] attribExt = ((DwgAttrib) dwgObject).getExtrusion();
                attribInsertionPoint3D = AcadExtrusionCalculator.CalculateAcadExtrusion(
                        attribInsertionPoint3D, attribExt);
                ((DwgAttrib) dwgObject).setInsertionPoint(new Point2D.Double(
                        attribInsertionPoint3D[0], attribInsertionPoint3D[1]));
                ((DwgAttrib) dwgObject).setElevation(attribInsertionPoint3D[2]);
            } else if (dwgObject instanceof DwgBlock) {
                // DwgBlock hasn't Extrusion
            } else if (dwgObject instanceof DwgBlockControl) {
                // DwgBlockControl hasn't Extrusion
            } else if (dwgObject instanceof DwgBlockHeader) {
                // DwgBlockHeader hasn't Extrusion
            } else if (dwgObject instanceof DwgCircle) {
                double[] circleCenter = ((DwgCircle) dwgObject).getCenter();
                double[] circleExt = ((DwgCircle) dwgObject).getExtrusion();
                circleCenter = AcadExtrusionCalculator.CalculateAcadExtrusion(circleCenter,
                        circleExt);
                ((DwgCircle) dwgObject).setCenter(circleCenter);
                // Seems that Autocad don't apply the extrusion to Ellipses
                /*} else if (dwgObject instanceof DwgEllipse) {
                 double[] ellipseCenter = ((DwgEllipse)dwgObject).getCenter();
                 double[] ellipseExt = ((DwgEllipse)dwgObject).getExtrusion();
                 ellipseCenter = AcadExtrusionCalculator.CalculateAcadExtrusion(ellipseCenter, ellipseExt);
                 ((DwgEllipse)dwgObject).setCenter(ellipseCenter);*/
            } else if (dwgObject instanceof DwgInsert) {
                double[] insertPoint = ((DwgInsert) dwgObject).getInsertionPoint();
                double[] insertExt = ((DwgInsert) dwgObject).getExtrusion();
                insertPoint = AcadExtrusionCalculator
                        .CalculateAcadExtrusion(insertPoint, insertExt);
                ((DwgInsert) dwgObject).setInsertionPoint(insertPoint);
            } else if (dwgObject instanceof DwgLayer) {
                // DwgLayer hasn't Extrusion
            } else if (dwgObject instanceof DwgLayerControl) {
                // DwgLayerControl hasn't Extrusion
            } else if (dwgObject instanceof DwgLine) {
                double[] lineP1 = ((DwgLine) dwgObject).getP1();
                double[] lineP2 = ((DwgLine) dwgObject).getP2();
                boolean zflag = ((DwgLine) dwgObject).isZflag();
                if (zflag) {
                    // elev = 0.0;
                    lineP1 = new double[]{lineP1[0], lineP1[1], 0.0};
                    lineP2 = new double[]{lineP2[0], lineP2[1], 0.0};
                }
                double[] lineExt = ((DwgLine) dwgObject).getExtrusion();
                lineP1 = AcadExtrusionCalculator.CalculateAcadExtrusion(lineP1, lineExt);
                lineP2 = AcadExtrusionCalculator.CalculateAcadExtrusion(lineP2, lineExt);
                ((DwgLine) dwgObject).setP1(lineP1);
                ((DwgLine) dwgObject).setP2(lineP2);
            } else if (dwgObject instanceof DwgLinearDimension) {
                // TODO: Extrusions in DwgLinearDimension elements
                // TODO: Void LwPolylines are a bug
            } else if (dwgObject instanceof DwgLwPolyline
                    && ((DwgLwPolyline) dwgObject).getVertices() != null) {
                Point2D[] vertices = ((DwgLwPolyline) dwgObject).getVertices();
                double[] lwPolylineExt = ((DwgLwPolyline) dwgObject).getNormal();
                // Normals and Extrusions aren`t the same
                if (lwPolylineExt[0] == 0 && lwPolylineExt[1] == 0 && lwPolylineExt[2] == 0)
                    lwPolylineExt[2] = 1.0;
                double elev = ((DwgLwPolyline) dwgObject).getElevation();
                double[][] lwPolylinePoints3D = new double[vertices.length][3];
                for( int j = 0; j < vertices.length; j++ ) {
                    lwPolylinePoints3D[j][0] = vertices[j].getX();
                    lwPolylinePoints3D[j][1] = vertices[j].getY();
                    lwPolylinePoints3D[j][2] = elev;
                    lwPolylinePoints3D[j] = AcadExtrusionCalculator.CalculateAcadExtrusion(
                            lwPolylinePoints3D[j], lwPolylineExt);
                }
                ((DwgLwPolyline) dwgObject).setElevation(elev);
                for( int j = 0; j < vertices.length; j++ ) {
                    vertices[j] = new Point2D.Double(lwPolylinePoints3D[j][0],
                            lwPolylinePoints3D[j][1]);
                }
                ((DwgLwPolyline) dwgObject).setVertices(vertices);
            } else if (dwgObject instanceof DwgMText) {
                double[] mtextPoint = ((DwgMText) dwgObject).getInsertionPoint();
                double[] mtextExt = ((DwgMText) dwgObject).getExtrusion();
                mtextPoint = AcadExtrusionCalculator.CalculateAcadExtrusion(mtextPoint, mtextExt);
                ((DwgMText) dwgObject).setInsertionPoint(mtextPoint);
            } else if (dwgObject instanceof DwgPoint) {
                double[] point = ((DwgPoint) dwgObject).getPoint();
                double[] pointExt = ((DwgPoint) dwgObject).getExtrusion();
                point = AcadExtrusionCalculator.CalculateAcadExtrusion(point, pointExt);
                ((DwgPoint) dwgObject).setPoint(point);
            } else if (dwgObject instanceof DwgSolid) {
                double[] corner1 = ((DwgSolid) dwgObject).getCorner1();
                double[] corner2 = ((DwgSolid) dwgObject).getCorner2();
                double[] corner3 = ((DwgSolid) dwgObject).getCorner3();
                double[] corner4 = ((DwgSolid) dwgObject).getCorner4();
                double[] solidExt = ((DwgSolid) dwgObject).getExtrusion();
                corner1 = AcadExtrusionCalculator.CalculateAcadExtrusion(corner1, solidExt);
                ((DwgSolid) dwgObject).setCorner1(corner1);
                ((DwgSolid) dwgObject).setCorner2(corner2);
                ((DwgSolid) dwgObject).setCorner3(corner3);
                ((DwgSolid) dwgObject).setCorner4(corner4);
            } else if (dwgObject instanceof DwgSpline) {
                // DwgSpline hasn't Extrusion
            } else if (dwgObject instanceof DwgText) {
                Point2D tpoint = ((DwgText) dwgObject).getInsertionPoint();
                double elev = ((DwgText) dwgObject).getElevation();
                double[] textPoint = new double[]{tpoint.getX(), tpoint.getY(), elev};
                double[] textExt = ((DwgText) dwgObject).getExtrusion();
                textPoint = AcadExtrusionCalculator.CalculateAcadExtrusion(textPoint, textExt);
                ((DwgText) dwgObject).setInsertionPoint(new Point2D.Double(textPoint[0],
                        textPoint[1]));
                ((DwgText) dwgObject).setElevation(elev);
            } else if (dwgObject instanceof DwgPolyline2D
                    && ((DwgPolyline2D) dwgObject).getPts() != null) {
                Point2D[] vertices = ((DwgPolyline2D) dwgObject).getPts();
                double[] polyline2DExt = ((DwgPolyline2D) dwgObject).getExtrusion();
                double elev = ((DwgPolyline2D) dwgObject).getElevation();
                double[][] polylinePoints3D = new double[vertices.length][3];
                for( int j = 0; j < vertices.length; j++ ) {
                    polylinePoints3D[j][0] = vertices[j].getX();
                    polylinePoints3D[j][1] = vertices[j].getY();
                    polylinePoints3D[j][2] = elev;
                    polylinePoints3D[j] = AcadExtrusionCalculator.CalculateAcadExtrusion(
                            polylinePoints3D[j], polyline2DExt);
                }
                ((DwgPolyline2D) dwgObject).setElevation(elev);
                for( int j = 0; j < vertices.length; j++ ) {
                    vertices[j] = new Point2D.Double(polylinePoints3D[j][0], polylinePoints3D[j][1]);
                }
                ((DwgPolyline2D) dwgObject).setPts(vertices);
            } else if (dwgObject instanceof DwgPolyline3D) {
                // DwgPolyline3D hasn't Extrusion
            } else if (dwgObject instanceof DwgVertex2D) {
                // DwgVertex2D hasn't Extrusion
            } else if (dwgObject instanceof DwgVertex3D) {
                // DwgVertex3D hasn't Extrusion
            } else {
                //
            }
        }
    }

    /**
     * Configure the geometry of the polylines in a DWG file from the vertex list in
     * this DWG file. This geometry is given by an array of Points.
     * Besides, manage closed polylines and polylines with bulges in a GIS Data model.
     * It means that the arcs of the polylines will be done through a set of points and
     * a distance between these points.
     */
    public void calculateGisModelDwgPolylines() {
        for( int i = 0; i < dwgObjects.size(); i++ ) {
            DwgObject pol = (DwgObject) dwgObjects.get(i);
            if (pol instanceof DwgPolyline2D) {
                int flags = ((DwgPolyline2D) pol).getFlags();
                int firstHandle = ((DwgPolyline2D) pol).getFirstVertexHandle();
                int lastHandle = ((DwgPolyline2D) pol).getLastVertexHandle();
                Vector pts = new Vector();
                Vector bulges = new Vector();
                double[] pt = new double[3];
                for( int j = 0; j < dwgObjects.size(); j++ ) {
                    DwgObject firstVertex = (DwgObject) dwgObjects.get(j);
                    if (firstVertex instanceof DwgVertex2D) {
                        int vertexHandle = firstVertex.getHandle();
                        if (vertexHandle == firstHandle) {
                            int k = 0;
                            while( true ) {
                                DwgObject vertex = (DwgObject) dwgObjects.get(j + k);
                                int vHandle = vertex.getHandle();
                                if (vertex instanceof DwgVertex2D) {
                                    pt = ((DwgVertex2D) vertex).getPoint();
                                    pts.add(new Point2D.Double(pt[0], pt[1]));
                                    double bulge = ((DwgVertex2D) vertex).getBulge();
                                    bulges.add(new Double(bulge));
                                    k++;
                                    if (vHandle == lastHandle && vertex instanceof DwgVertex2D) {
                                        break;
                                    }
                                } else if (vertex instanceof DwgSeqend) {
                                    break;
                                }
                            }
                        }
                    }
                }
                if (pts.size() > 0) {
                    Point2D[] newPts = new Point2D[pts.size()];
                    if ((flags & 0x1) == 0x1) {
                        newPts = new Point2D[pts.size() + 1];
                        for( int j = 0; j < pts.size(); j++ ) {
                            newPts[j] = (Point2D) pts.get(j);
                        }
                        newPts[pts.size()] = (Point2D) pts.get(0);
                        bulges.add(new Double(0));
                    } else {
                        for( int j = 0; j < pts.size(); j++ ) {
                            newPts[j] = (Point2D) pts.get(j);
                        }
                    }
                    double[] bs = new double[bulges.size()];
                    for( int j = 0; j < bulges.size(); j++ ) {
                        bs[j] = ((Double) bulges.get(j)).doubleValue();
                    }
                    ((DwgPolyline2D) pol).setBulges(bs);
                    Point2D[] points = GisModelCurveCalculator.calculateGisModelBulge(newPts, bs);
                    ((DwgPolyline2D) pol).setPts(points);
                } else {
                    // System.out.println("Encontrada polil�nea sin puntos ...");
                    // TODO: No se debe mandar nunca una polil�nea sin puntos, si esto
                    // ocurre es porque existe un error que hay que corregir ...
                }
            } else if (pol instanceof DwgPolyline3D) {
                int closedFlags = ((DwgPolyline3D) pol).getClosedFlags();
                int firstHandle = ((DwgPolyline3D) pol).getFirstVertexHandle();
                int lastHandle = ((DwgPolyline3D) pol).getLastVertexHandle();
                Vector pts = new Vector();
                double[] pt = new double[3];
                for( int j = 0; j < dwgObjects.size(); j++ ) {
                    DwgObject firstVertex = (DwgObject) dwgObjects.get(j);
                    if (firstVertex instanceof DwgVertex3D) {
                        int vertexHandle = firstVertex.getHandle();
                        if (vertexHandle == firstHandle) {
                            int k = 0;
                            while( true ) {
                                DwgObject vertex = (DwgObject) dwgObjects.get(j + k);
                                int vHandle = vertex.getHandle();
                                if (vertex instanceof DwgVertex3D) {
                                    pt = ((DwgVertex3D) vertex).getPoint();
                                    pts.add(new double[]{pt[0], pt[1], pt[2]});
                                    k++;
                                    if (vHandle == lastHandle && vertex instanceof DwgVertex3D) {
                                        break;
                                    }
                                } else if (vertex instanceof DwgSeqend) {
                                    break;
                                }
                            }
                        }
                    }
                }
                if (pts.size() > 0) {
                    double[][] newPts = new double[pts.size()][3];
                    if ((closedFlags & 0x1) == 0x1) {
                        newPts = new double[pts.size() + 1][3];
                        for( int j = 0; j < pts.size(); j++ ) {
                            newPts[j][0] = ((double[]) pts.get(j))[0];
                            newPts[j][1] = ((double[]) pts.get(j))[1];
                            newPts[j][2] = ((double[]) pts.get(j))[2];
                        }
                        newPts[pts.size()][0] = ((double[]) pts.get(0))[0];
                        newPts[pts.size()][1] = ((double[]) pts.get(0))[1];
                        newPts[pts.size()][2] = ((double[]) pts.get(0))[2];
                    } else {
                        for( int j = 0; j < pts.size(); j++ ) {
                            newPts[j][0] = ((double[]) pts.get(j))[0];
                            newPts[j][1] = ((double[]) pts.get(j))[1];
                            newPts[j][2] = ((double[]) pts.get(j))[2];
                        }
                    }
                    ((DwgPolyline3D) pol).setPts(newPts);
                } else {
                    // System.out.println("Encontrada polil�nea sin puntos ...");
                    // TODO: No se debe mandar nunca una polil�nea sin puntos, si esto
                    // ocurre es porque existe un error que hay que corregir ...
                }
            } else if (pol instanceof DwgLwPolyline && ((DwgLwPolyline) pol).getVertices() != null) {
                int flags = ((DwgLwPolyline) pol).getFlag();
                Point2D[] pts = ((DwgLwPolyline) pol).getVertices();
                double[] bulges = ((DwgLwPolyline) pol).getBulges();
                Point2D[] newPts = new Point2D[pts.length];
                double[] newBulges = new double[bulges.length];
                // TODO: Aqu� pueden existir casos no contemplados ...
                // System.out.println("flags = " + flags);
                if (flags == 512 || flags == 776 || flags == 768) {
                    newPts = new Point2D[pts.length + 1];
                    newBulges = new double[bulges.length + 1];
                    for( int j = 0; j < pts.length; j++ ) {
                        newPts[j] = (Point2D) pts[j];
                    }
                    newPts[pts.length] = (Point2D) pts[0];
                    newBulges[pts.length] = 0;
                } else {
                    for( int j = 0; j < pts.length; j++ ) {
                        newPts[j] = (Point2D) pts[j];
                    }
                }
                if (pts.length > 0) {
                    ((DwgLwPolyline) pol).setBulges(newBulges);
                    Point2D[] points = GisModelCurveCalculator.calculateGisModelBulge(newPts,
                            newBulges);
                    ((DwgLwPolyline) pol).setVertices(points);
                } else {
                    // System.out.println("Encontrada polil�nea sin puntos ...");
                    // TODO: No se debe mandar nunca una polil�nea sin puntos, si esto
                    // ocurre es porque existe un error que hay que corregir ...
                }
            }
        }
    }

    /**
     * Configure the geometry of the polylines in a DWG file from the vertex list in
     * this DWG file. This geometry is given by an array of Points
     * Besides, manage closed polylines and polylines with bulges in a GIS Data model.
     * It means that the arcs of the polylines will be done through a curvature
     * parameter called bulge associated with the points of the polyline.
     */
    public void calculateCadModelDwgPolylines() {
        for( int i = 0; i < dwgObjects.size(); i++ ) {
            DwgObject pol = (DwgObject) dwgObjects.get(i);
            if (pol instanceof DwgPolyline2D) {
                int flags = ((DwgPolyline2D) pol).getFlags();
                int firstHandle = ((DwgPolyline2D) pol).getFirstVertexHandle();
                int lastHandle = ((DwgPolyline2D) pol).getLastVertexHandle();
                Vector pts = new Vector();
                Vector bulges = new Vector();
                double[] pt = new double[3];
                for( int j = 0; j < dwgObjects.size(); j++ ) {
                    DwgObject firstVertex = (DwgObject) dwgObjects.get(j);
                    if (firstVertex instanceof DwgVertex2D) {
                        int vertexHandle = firstVertex.getHandle();
                        if (vertexHandle == firstHandle) {
                            int k = 0;
                            while( true ) {
                                DwgObject vertex = (DwgObject) dwgObjects.get(j + k);
                                int vHandle = vertex.getHandle();
                                if (vertex instanceof DwgVertex2D) {
                                    pt = ((DwgVertex2D) vertex).getPoint();
                                    pts.add(new Point2D.Double(pt[0], pt[1]));
                                    double bulge = ((DwgVertex2D) vertex).getBulge();
                                    bulges.add(new Double(bulge));
                                    k++;
                                    if (vHandle == lastHandle && vertex instanceof DwgVertex2D) {
                                        break;
                                    }
                                } else if (vertex instanceof DwgSeqend) {
                                    break;
                                }
                            }
                        }
                    }
                }
                if (pts.size() > 0) {
                    /*Point2D[] newPts = new Point2D[pts.size()];
                     if ((flags & 0x1)==0x1) {
                     newPts = new Point2D[pts.size()+1];
                     for (int j=0;j<pts.size();j++) {
                     newPts[j] = (Point2D)pts.get(j);
                     }
                     newPts[pts.size()] = (Point2D)pts.get(0);
                     bulges.add(new Double(0));
                     } else {
                     for (int j=0;j<pts.size();j++) {
                     newPts[j] = (Point2D)pts.get(j);
                     }
                     }*/
                    double[] bs = new double[bulges.size()];
                    for( int j = 0; j < bulges.size(); j++ ) {
                        bs[j] = ((Double) bulges.get(j)).doubleValue();
                    }
                    ((DwgPolyline2D) pol).setBulges(bs);
                    // Point2D[] points = GisModelCurveCalculator.calculateGisModelBulge(newPts,
                    // bs);
                    Point2D[] points = new Point2D[pts.size()];
                    for( int j = 0; j < pts.size(); j++ ) {
                        points[j] = (Point2D) pts.get(j);
                    }
                    ((DwgPolyline2D) pol).setPts(points);
                } else {
                    // System.out.println("Encontrada polil�nea sin puntos ...");
                    // TODO: No se debe mandar nunca una polil�nea sin puntos, si esto
                    // ocurre es porque existe un error que hay que corregir ...
                }
            } else if (pol instanceof DwgPolyline3D) {
            } else if (pol instanceof DwgLwPolyline && ((DwgLwPolyline) pol).getVertices() != null) {
            }
        }
    }

    /**
     * Modify the geometry of the objects contained in the blocks of a DWG file and
     * add these objects to the DWG object list.
     */
    public void blockManagement() {
        Vector dwgObjectsWithoutBlocks = new Vector();
        boolean addingToBlock = false;
        for( int i = 0; i < dwgObjects.size(); i++ ) {
            try {
                DwgObject entity = (DwgObject) dwgObjects.get(i);
                if (entity instanceof DwgArc && !addingToBlock) {
                    dwgObjectsWithoutBlocks.add(entity);
                } else if (entity instanceof DwgEllipse && !addingToBlock) {
                    dwgObjectsWithoutBlocks.add(entity);
                } else if (entity instanceof DwgCircle && !addingToBlock) {
                    dwgObjectsWithoutBlocks.add(entity);
                } else if (entity instanceof DwgPolyline2D && !addingToBlock) {
                    dwgObjectsWithoutBlocks.add(entity);
                } else if (entity instanceof DwgPolyline3D && !addingToBlock) {
                    dwgObjectsWithoutBlocks.add(entity);
                } else if (entity instanceof DwgLwPolyline && !addingToBlock) {
                    dwgObjectsWithoutBlocks.add(entity);
                } else if (entity instanceof DwgSolid && !addingToBlock) {
                    dwgObjectsWithoutBlocks.add(entity);
                } else if (entity instanceof DwgLine && !addingToBlock) {
                    dwgObjectsWithoutBlocks.add(entity);
                } else if (entity instanceof DwgPoint && !addingToBlock) {
                    dwgObjectsWithoutBlocks.add(entity);
                } else if (entity instanceof DwgMText && !addingToBlock) {
                    dwgObjectsWithoutBlocks.add(entity);
                } else if (entity instanceof DwgText && !addingToBlock) {
                    dwgObjectsWithoutBlocks.add(entity);
                } else if (entity instanceof DwgAttrib && !addingToBlock) {
                    dwgObjectsWithoutBlocks.add(entity);
                } else if (entity instanceof DwgAttdef && !addingToBlock) {
                    dwgObjectsWithoutBlocks.add(entity);
                } else if (entity instanceof DwgBlock) {
                    addingToBlock = true;
                } else if (entity instanceof DwgEndblk) {
                    addingToBlock = false;
                } else if (entity instanceof DwgBlockHeader) {
                    addingToBlock = true;
                } else if (entity instanceof DwgInsert && !addingToBlock) {
                    /* double[] p = ((DwgInsert) entity).getInsertionPoint();
                     Point2D point = new Point2D.Double(p[0], p[1]);
                     double[] scale = ((DwgInsert) entity).getScale();
                     double rot = ((DwgInsert) entity).getRotation();
                     int blockHandle = ((DwgInsert) entity).getBlockHeaderHandle();
                     manageInsert(point, scale, rot, blockHandle, i,
                     dwgObjectsWithoutBlocks);*/
                } else {
                    // System.out.println("Detectado dwgObject pendiente de implementar");
                }
            } catch (StackOverflowError e) {
                e.printStackTrace();
                System.out.println("Overflowerror at object: " + i);
            }
        }
        dwgObjects = dwgObjectsWithoutBlocks;
    }

    /**
     * Manages an INSERT of a DWG file. This object is the insertion point of a DWG
     * block. It has the next parameters:
     * @param insPoint, coordinates of the insertion point.
     * @param scale, scale of the elements of the block that will be inserted.
     * @param rot, rotation angle of the elements of the block.
     * @param bHandle, offset for the coordinates of the elements of the block.
     * @param id, count that serves as a id.
     * @param dwgObjectsWithoutBlocks, a object list with the elements extracted from
     * the blocks.
     */
    private void manageInsert( Point2D insPoint, double[] scale, double rot, int bHandle, int id,
            Vector dwgObjectsWithoutBlocks ) {
        for( int i = 0; i < dwgObjects.size(); i++ ) {
            DwgObject obj = (DwgObject) dwgObjects.get(i);
            if (obj instanceof DwgBlockHeader) {
                int objHandle = ((DwgBlockHeader) obj).getHandle();
                if (objHandle == bHandle) {
                    // System.out.println("Encontrado DwgBlockHeader con handle = " + bHandle);
                    double[] bPoint = ((DwgBlockHeader) obj).getBasePoint();
                    String bname = ((DwgBlockHeader) obj).getName();
                    // System.out.println("Nombre del bloque = " + bname);
                    if (!bname.startsWith("*")) {
                        int firstObjectHandle = ((DwgBlockHeader) obj).getFirstEntityHandle();
                        // System.out.println("firstObjectHandle = " + firstObjectHandle);
                        int lastObjectHandle = ((DwgBlockHeader) obj).getLastEntityHandle();
                        // System.out.println("lastObjectHandle = " + lastObjectHandle);
                        DwgBlock block = null;
                        for( int j = 0; j < dwgObjects.size(); j++ ) {
                            DwgObject ent = (DwgObject) dwgObjects.get(j);
                            if (ent instanceof DwgBlock) {
                                String name = ((DwgBlock) ent).getName();
                                if (bname.equals(name)) {
                                    block = (DwgBlock) ent;
                                    // System.out.println("Encontrado DwgBlock con bname = " +
                                    // bname);
                                    break;
                                }
                            }
                        }
                        for( int j = 0; j < dwgObjects.size(); j++ ) {
                            DwgObject fObj = (DwgObject) dwgObjects.get(j);
                            if (fObj != null) {
                                int fObjHandle = fObj.getHandle();
                                if (fObjHandle == firstObjectHandle) {
                                    int k = 0;
                                    while( true ) {
                                        // System.out.println("Encontrado elemento " + k +
                                        // " del bloque");
                                        DwgObject iObj = (DwgObject) dwgObjects.get(j + k);
                                        int iObjHandle = iObj.getHandle();
                                        // System.out.println("iObj.getType() = " + iObj.getType());
                                        // System.out.println("insPoint.getX() = " +
                                        // insPoint.getX());
                                        // System.out.println("insPoint.getY() = " +
                                        // insPoint.getY());
                                        // System.out.println("rot = " + rot);
                                        manageBlockEntity(iObj, bPoint, insPoint, scale, rot, id,
                                                dwgObjectsWithoutBlocks);
                                        k++;
                                        if (iObjHandle == lastObjectHandle)
                                            break;
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Changes the location of an object extracted from a block. This location will be
     * obtained through the insertion parameters from the block and the corresponding
     * insert.
     * @param entity, the entity extracted from the block.
     * @param bPoint, offset for the coordinates of the entity.
     * @param insPoint, coordinates of the insertion point for the entity.
     * @param scale, scale for the entity.
     * @param rot, rotation angle for the entity.
     * @param id, a count as a id.
     * @param dwgObjectsWithoutBlocks, a object list with the elements extracted from
     * the blocks.
     */
    private void manageBlockEntity( DwgObject entity, double[] bPoint, Point2D insPoint,
            double[] scale, double rot, int id, Vector dwgObjectsWithoutBlocks ) {
        if (entity instanceof DwgArc) {
            // System.out.println("Encuentra un arco dentro de un bloque ...");
            DwgArc transformedEntity = new DwgArc();
            double[] center = ((DwgArc) entity).getCenter();
            Point2D pointAux = new Point2D.Double(center[0] - bPoint[0], center[1] - bPoint[1]);
            double laX = insPoint.getX()
                    + ((pointAux.getX() * scale[0]) * Math.cos(rot) + (pointAux.getY() * scale[1])
                            * (-1) * Math.sin(rot));
            double laY = insPoint.getY()
                    + ((pointAux.getX() * scale[0]) * Math.sin(rot) + (pointAux.getY() * scale[1])
                            * Math.cos(rot));
            double laZ = center[2] * scale[2];
            double[] transformedCenter = new double[]{laX, laY, laZ};
            double radius = ((DwgArc) entity).getRadius();
            // System.out.println("radius = " + radius);
            // System.out.println("scale[0] = " + scale[0]);
            // System.out.println("scale[1] = " + scale[1]);
            // System.out.println("rot = " + rot);
            double transformedRadius = radius * scale[0];
            double initAngle = ((DwgArc) entity).getInitAngle();
            double endAngle = ((DwgArc) entity).getEndAngle();
            // System.out.println("initAngle = " + initAngle);
            // System.out.println("endAngle = " + endAngle);
            // System.out.println("rot = " + rot);
            double transformedInitAngle = initAngle + rot;
            if (transformedInitAngle < 0) {
                transformedInitAngle = transformedInitAngle + (2 * Math.PI);
            } else if (transformedInitAngle > (2 * Math.PI)) {
                transformedInitAngle = transformedInitAngle - (2 * Math.PI);
            }
            double transformedEndAngle = endAngle + rot;
            if (transformedEndAngle < 0) {
                transformedEndAngle = transformedEndAngle + (2 * Math.PI);
            } else if (transformedEndAngle > (2 * Math.PI)) {
                transformedEndAngle = transformedEndAngle - (2 * Math.PI);
            }
            transformedEntity = (DwgArc) ((DwgArc) entity).clone();
            transformedEntity.setCenter(transformedCenter);
            transformedEntity.setRadius(transformedRadius);
            transformedEntity.setInitAngle(transformedInitAngle);
            transformedEntity.setEndAngle(transformedEndAngle);
            dwgObjectsWithoutBlocks.add(transformedEntity);
        } else if (entity instanceof DwgCircle) {
            // System.out.println("Encuentra un c�rculo dentro de un bloque ...");
            DwgCircle transformedEntity = new DwgCircle();
            double[] center = ((DwgCircle) entity).getCenter();
            Point2D pointAux = new Point2D.Double(center[0] - bPoint[0], center[1] - bPoint[1]);
            double laX = insPoint.getX()
                    + ((pointAux.getX() * scale[0]) * Math.cos(rot) + (pointAux.getY() * scale[1])
                            * (-1) * Math.sin(rot));
            double laY = insPoint.getY()
                    + ((pointAux.getX() * scale[0]) * Math.sin(rot) + (pointAux.getY() * scale[1])
                            * Math.cos(rot));
            double laZ = center[2] * scale[2];
            double[] transformedCenter = new double[]{laX, laY, laZ};
            double radius = ((DwgCircle) entity).getRadius();
            double transformedRadius = radius * scale[0];
            transformedEntity = (DwgCircle) ((DwgCircle) entity).clone();
            transformedEntity.setCenter(transformedCenter);
            transformedEntity.setRadius(transformedRadius);
            dwgObjectsWithoutBlocks.add(transformedEntity);
        } else if (entity instanceof DwgEllipse) {
            DwgEllipse transformedEntity = new DwgEllipse();
            double[] center = ((DwgEllipse) entity).getCenter();
            Point2D pointAux = new Point2D.Double(center[0] - bPoint[0], center[1] - bPoint[1]);
            double laX = insPoint.getX()
                    + ((pointAux.getX() * scale[0]) * Math.cos(rot) + (pointAux.getY() * scale[1])
                            * (-1) * Math.sin(rot));
            double laY = insPoint.getY()
                    + ((pointAux.getX() * scale[0]) * Math.sin(rot) + (pointAux.getY() * scale[1])
                            * Math.cos(rot));
            double laZ = center[2] * scale[2];
            double[] transformedCenter = new double[]{laX, laY, laZ};
            double[] majorAxisVector = ((DwgEllipse) entity).getMajorAxisVector();
            double[] transformedMajorAxisVector = new double[]{majorAxisVector[0] * scale[0],
                    majorAxisVector[1] * scale[1], majorAxisVector[2] * scale[2]};
            // TODO: Rotar un �ngulo rot el vector majorAxisVector fijado en
            // center.
            double axisRatio = ((DwgEllipse) entity).getAxisRatio();
            double transformedAxisRatio = axisRatio;
            double initAngle = ((DwgEllipse) entity).getInitAngle();
            double endAngle = ((DwgEllipse) entity).getEndAngle();
            double transformedInitAngle = initAngle + rot;
            if (transformedInitAngle < 0) {
                transformedInitAngle = transformedInitAngle + (2 * Math.PI);
            } else if (transformedInitAngle > (2 * Math.PI)) {
                transformedInitAngle = transformedInitAngle - (2 * Math.PI);
            }
            double transformedEndAngle = endAngle + rot;
            if (transformedEndAngle < 0) {
                transformedEndAngle = transformedEndAngle + (2 * Math.PI);
            } else if (transformedEndAngle > (2 * Math.PI)) {
                transformedEndAngle = transformedEndAngle - (2 * Math.PI);
            }
            transformedEntity = (DwgEllipse) ((DwgEllipse) entity).clone();
            transformedEntity.setCenter(transformedCenter);
            transformedEntity.setMajorAxisVector(transformedMajorAxisVector);
            transformedEntity.setAxisRatio(transformedAxisRatio);
            transformedEntity.setInitAngle(transformedInitAngle);
            transformedEntity.setEndAngle(transformedEndAngle);
            dwgObjectsWithoutBlocks.add(transformedEntity);
        } else if (entity instanceof DwgLine) {
            // System.out.println("Encuentra una l�nea dentro de un bloque ...");
            DwgLine transformedEntity = new DwgLine();
            double[] p1 = ((DwgLine) entity).getP1();
            double[] p2 = ((DwgLine) entity).getP2();
            Point2D pointAux = new Point2D.Double(p1[0] - bPoint[0], p1[1] - bPoint[1]);
            double laX = insPoint.getX()
                    + ((pointAux.getX() * scale[0]) * Math.cos(rot) + (pointAux.getY() * scale[1])
                            * (-1) * Math.sin(rot));
            double laY = insPoint.getY()
                    + ((pointAux.getX() * scale[0]) * Math.sin(rot) + (pointAux.getY() * scale[1])
                            * Math.cos(rot));
            double[] transformedP1 = null;
            if (((DwgLine) entity).isZflag()) {
                double laZ = p1[2] * scale[2];
                transformedP1 = new double[]{laX, laY, laZ};
            } else {
                transformedP1 = new double[]{laX, laY};
            }
            // double[] transformedP1 = new double[]{laX, laY};
            pointAux = new Point2D.Double(p2[0] - bPoint[0], p2[1] - bPoint[1]);
            laX = insPoint.getX()
                    + ((pointAux.getX() * scale[0]) * Math.cos(rot) + (pointAux.getY() * scale[1])
                            * (-1) * Math.sin(rot));
            laY = insPoint.getY()
                    + ((pointAux.getX() * scale[0]) * Math.sin(rot) + (pointAux.getY() * scale[1])
                            * Math.cos(rot));
            double[] transformedP2 = null;
            if (((DwgLine) entity).isZflag()) {
                double laZ = p2[2] * scale[2];
                transformedP2 = new double[]{laX, laY, laZ};
            } else {
                transformedP2 = new double[]{laX, laY};
            }
            // double[] transformedP2 = new double[]{laX, laY};
            transformedEntity = (DwgLine) ((DwgLine) entity).clone();
            transformedEntity.setP1(transformedP1);
            transformedEntity.setP2(transformedP2);
            dwgObjectsWithoutBlocks.add(transformedEntity);
        } else if (entity instanceof DwgLwPolyline) {
            // System.out.println("Encuentra una DwgLwPolyline dentro de un bloque ...");
            DwgLwPolyline transformedEntity = new DwgLwPolyline();
            Point2D[] vertices = ((DwgLwPolyline) entity).getVertices();
            if (vertices != null) {
                Point2D[] transformedVertices = new Point2D[vertices.length];
                for( int i = 0; i < vertices.length; i++ ) {
                    Point2D pointAux = new Point2D.Double(vertices[i].getX() - bPoint[0],
                            vertices[i].getY() - bPoint[1]);
                    double laX = insPoint.getX()
                            + ((pointAux.getX() * scale[0]) * Math.cos(rot) + (pointAux.getY() * scale[1])
                                    * (-1) * Math.sin(rot));
                    double laY = insPoint.getY()
                            + ((pointAux.getX() * scale[0]) * Math.sin(rot) + (pointAux.getY() * scale[1])
                                    * Math.cos(rot));
                    transformedVertices[i] = new Point2D.Double(laX, laY);
                }
                transformedEntity = (DwgLwPolyline) ((DwgLwPolyline) entity).clone();
                transformedEntity.setVertices(transformedVertices);
                transformedEntity.setElevation(((DwgLwPolyline) entity).getElevation() * scale[2]);
                dwgObjectsWithoutBlocks.add(transformedEntity);
            }
        } else if (entity instanceof DwgMText) {

        } else if (entity instanceof DwgPoint) {

        } else if (entity instanceof DwgPolyline2D) {
            // System.out.println("Encuentra una polil�nea dentro de un bloque ...");
            DwgPolyline2D transformedEntity = new DwgPolyline2D();
            Point2D[] vertices = ((DwgPolyline2D) entity).getPts();
            if (vertices != null) {
                Point2D[] transformedVertices = new Point2D[vertices.length];
                for( int i = 0; i < vertices.length; i++ ) {
                    Point2D pointAux = new Point2D.Double(vertices[i].getX() - bPoint[0],
                            vertices[i].getY() - bPoint[1]);
                    double laX = insPoint.getX()
                            + ((pointAux.getX() * scale[0]) * Math.cos(rot) + (pointAux.getY() * scale[1])
                                    * (-1) * Math.sin(rot));
                    double laY = insPoint.getY()
                            + ((pointAux.getX() * scale[0]) * Math.sin(rot) + (pointAux.getY() * scale[1])
                                    * Math.cos(rot));
                    transformedVertices[i] = new Point2D.Double(laX, laY);
                }
                transformedEntity = (DwgPolyline2D) ((DwgPolyline2D) entity).clone();
                transformedEntity.setPts(transformedVertices);
                transformedEntity.setElevation(((DwgPolyline2D) entity).getElevation() * scale[2]);
                dwgObjectsWithoutBlocks.add(transformedEntity);
            }
        } else if (entity instanceof DwgPolyline3D) {

        } else if (entity instanceof DwgSolid) {
            DwgSolid transformedEntity = new DwgSolid();
            double[] corner1 = ((DwgSolid) entity).getCorner1();
            double[] corner2 = ((DwgSolid) entity).getCorner2();
            double[] corner3 = ((DwgSolid) entity).getCorner3();
            double[] corner4 = ((DwgSolid) entity).getCorner4();
            Point2D pointAux = new Point2D.Double(corner1[0] - bPoint[0], corner1[1] - bPoint[1]);
            double laX = insPoint.getX()
                    + ((pointAux.getX() * scale[0]) * Math.cos(rot) + (pointAux.getY() * scale[1])
                            * (-1) * Math.sin(rot));
            double laY = insPoint.getY()
                    + ((pointAux.getX() * scale[0]) * Math.sin(rot) + (pointAux.getY() * scale[1])
                            * Math.cos(rot));
            double[] transformedP1 = new double[]{laX, laY};
            pointAux = new Point2D.Double(corner2[0] - bPoint[0], corner2[1] - bPoint[1]);
            laX = insPoint.getX()
                    + ((pointAux.getX() * scale[0]) * Math.cos(rot) + (pointAux.getY() * scale[1])
                            * (-1) * Math.sin(rot));
            laY = insPoint.getY()
                    + ((pointAux.getX() * scale[0]) * Math.sin(rot) + (pointAux.getY() * scale[1])
                            * Math.cos(rot));
            double[] transformedP2 = new double[]{laX, laY};
            pointAux = new Point2D.Double(corner3[0] - bPoint[0], corner3[1] - bPoint[1]);
            laX = insPoint.getX()
                    + ((pointAux.getX() * scale[0]) * Math.cos(rot) + (pointAux.getY() * scale[1])
                            * (-1) * Math.sin(rot));
            laY = insPoint.getY()
                    + ((pointAux.getX() * scale[0]) * Math.sin(rot) + (pointAux.getY() * scale[1])
                            * Math.cos(rot));
            double[] transformedP3 = new double[]{laX, laY};
            pointAux = new Point2D.Double(corner4[0] - bPoint[0], corner4[1] - bPoint[1]);
            laX = insPoint.getX()
                    + ((pointAux.getX() * scale[0]) * Math.cos(rot) + (pointAux.getY() * scale[1])
                            * (-1) * Math.sin(rot));
            laY = insPoint.getY()
                    + ((pointAux.getX() * scale[0]) * Math.sin(rot) + (pointAux.getY() * scale[1])
                            * Math.cos(rot));
            double[] transformedP4 = new double[]{laX, laY};
            transformedEntity = (DwgSolid) ((DwgSolid) entity).clone();
            transformedEntity.setCorner1(transformedP1);
            transformedEntity.setCorner2(transformedP2);
            transformedEntity.setCorner3(transformedP3);
            transformedEntity.setCorner4(transformedP4);
            transformedEntity.setElevation(((DwgSolid) entity).getElevation() * scale[2]);
            dwgObjectsWithoutBlocks.add(transformedEntity);
        } else if (entity instanceof DwgSpline) {

        } else if (entity instanceof DwgText) {

        } else if (entity instanceof DwgInsert) {
            // System.out.println("Encuentra un insert dentro de un bloque ...");
            DwgInsert transformedEntity = new DwgInsert();
            double[] p = ((DwgInsert) entity).getInsertionPoint();
            Point2D point = new Point2D.Double(p[0], p[1]);
            double[] newScale = ((DwgInsert) entity).getScale();
            double newRot = ((DwgInsert) entity).getRotation();
            int newBlockHandle = ((DwgInsert) entity).getBlockHeaderHandle();
            Point2D pointAux = new Point2D.Double(point.getX() - bPoint[0], point.getY()
                    - bPoint[1]);
            double laX = insPoint.getX()
                    + ((pointAux.getX() * scale[0]) * Math.cos(rot) + (pointAux.getY() * scale[1])
                            * (-1) * Math.sin(rot));
            double laY = insPoint.getY()
                    + ((pointAux.getX() * scale[0]) * Math.sin(rot) + (pointAux.getY() * scale[1])
                            * Math.cos(rot));
            double laZ = p[2] * scale[2];
            Point2D newInsPoint = new Point2D.Double(laX, laY);
            newScale = new double[]{scale[0] * newScale[0], scale[1] * newScale[1],
                    scale[2] * newScale[2]};
            newRot = newRot + rot;
            if (newRot < 0) {
                newRot = newRot + (2 * Math.PI);
            } else if (newRot > (2 * Math.PI)) {
                newRot = newRot - (2 * Math.PI);
            }
            manageInsert(newInsPoint, newScale, newRot, newBlockHandle, id, dwgObjectsWithoutBlocks);
        }
    }

    /**
     * Initialize a new Vector that contains the DWG file layers. Each layer have three
     * parameters. These parameters are handle, name and color
     */
    public void initializeLayerTable() {
        layerTable = new Vector();
        layerNames = new Vector();
        for( int i = 0; i < dwgObjects.size(); i++ ) {
            DwgObject obj = (DwgObject) dwgObjects.get(i);
            if (obj instanceof DwgLayer) {
                Vector layerTableRecord = new Vector();
                layerTableRecord.add(new Integer(obj.getHandle()));
                layerTableRecord.add(((DwgLayer) obj).getName());
                layerTableRecord.add(new Integer(((DwgLayer) obj).getColor()));
                layerTable.add(layerTableRecord);
                layerNames.add(((DwgLayer) obj).getName());
            }
        }
        System.out.println("");
    }

    /**
     * Returns the name of the layer of a DWG object 
     * 
     * @param entity DWG object which we want to know its layer name
     * @return String Layer name of the DWG object
     */
    // TODO: Gesti�n de capas pendiente ...
    public String getLayerName( DwgObject entity ) {
        String layerName = "";
        int layer = entity.getLayerHandle();
        for( int j = 0; j < layerTable.size(); j++ ) {
            Vector layerTableRecord = (Vector) layerTable.get(j);
            int lHandle = ((Integer) layerTableRecord.get(0)).intValue();
            if (lHandle == layer) {
                layerName = (String) layerTableRecord.get(1);
            }
        }
        /*
         * workaround for the cases in which the entity 
         * can't define it's own layer name: assign all the
         * objects to the layer 0
         */
        if (layerName.equals(""))
            return "0";
        return layerName;
    }

    /**
     * Returns the color of the layer of a DWG object 
     * 
     * @param entity DWG object which we want to know its layer color
     * @return int Layer color of the DWG object in the Autocad color code
     */
    public int getColorByLayer( DwgObject entity ) {
        int colorByLayer = 0;
        int layer = entity.getLayerHandle();
        for( int j = 0; j < layerTable.size(); j++ ) {
            Vector layerTableRecord = (Vector) layerTable.get(j);
            int lHandle = ((Integer) layerTableRecord.get(0)).intValue();
            if (lHandle == layer) {
                colorByLayer = ((Integer) layerTableRecord.get(2)).intValue();
            }
        }
        return colorByLayer;
    }

    private void setDwgVersion() throws IOException {
        System.out.println("DwgFile.setDwgVersion() executed ...");
        File file = new File(fileName);
        FileInputStream fileInputStream = new FileInputStream(file);
        FileChannel fileChannel = fileInputStream.getChannel();
        long channelSize = fileChannel.size();
        ByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, channelSize);
        byte[] versionBytes = {byteBuffer.get(0), byteBuffer.get(1), byteBuffer.get(2),
                byteBuffer.get(3), byteBuffer.get(4), byteBuffer.get(5)};
        ByteBuffer versionByteBuffer = ByteBuffer.wrap(versionBytes);
        String versionString = readDwgVersion(versionByteBuffer);
        String version;
        if (versionString.equals("AC1009")) {
            version = new String("R12");
        } else if (versionString.equals("AC1010")) {
            version = new String("R12+");
        } else if (versionString.equals("AC1012")) {
            version = new String("R13");
        } else if (versionString.equals("AC1014")) {
            version = new String("R14");
        } else if (versionString.equals("AC1015")) {
            version = new String("R15");
        } else {
            version = new String("Unknown");
        }
        this.dwgVersion = version;
    }

    private String readDwgVersion( ByteBuffer versionBuffer ) {
        String[] bs = new String[versionBuffer.capacity()];
        String sv = "";
        for( int i = 0; i < versionBuffer.capacity(); i++ ) {
            bs[i] = new String(new byte[]{(byte) (versionBuffer.get(i))});
            sv = sv + bs[i];
        }
        return sv;
    }

    /**
     * Test if the DWG file is 2D or 3D. If there is any object with a non cero
     * elevation value, the file is considered 3D.
     */
    public void testDwg3D() {
        for( int i = 0; i < dwgObjects.size(); i++ ) {
            DwgObject obj = (DwgObject) dwgObjects.get(i);
            double z = 0.0;
            if (obj instanceof DwgArc) {
                z = ((DwgArc) obj).getCenter()[2];
                if (z != 0.0)
                    dwg3DFile = true;
            } else if (obj instanceof DwgAttrib) {
                z = ((DwgAttrib) obj).getElevation();
                if (z != 0.0)
                    dwg3DFile = true;
            } else if (obj instanceof DwgBlockHeader) {
                z = ((DwgBlockHeader) obj).getBasePoint()[2];
                if (z != 0.0)
                    dwg3DFile = true;
            } else if (obj instanceof DwgCircle) {
                z = ((DwgCircle) obj).getCenter()[2];
                if (z != 0.0)
                    dwg3DFile = true;
            } else if (obj instanceof DwgEllipse) {
                z = ((DwgEllipse) obj).getCenter()[2];
                if (z != 0.0)
                    dwg3DFile = true;
            } else if (obj instanceof DwgInsert) {
                z = ((DwgInsert) obj).getInsertionPoint()[2];
                if (z != 0.0)
                    dwg3DFile = true;
            } else if (obj instanceof DwgLine) {
                if (!((DwgLine) obj).isZflag()) {
                    double z1 = ((DwgLine) obj).getP1()[2];
                    double z2 = ((DwgLine) obj).getP2()[2];
                    if (z1 != 0.0 || z2 != 0.0)
                        dwg3DFile = true;
                }
                // } else if (obj instanceof DwgLinearDimension) {
                // z = ((DwgLinearDimension)obj).getElevation();
                // if (z!=0.0) dwg3DFile = true;
            } else if (obj instanceof DwgLwPolyline) {
                z = ((DwgLwPolyline) obj).getElevation();
                if (z != 0.0)
                    dwg3DFile = true;
            } else if (obj instanceof DwgMText) {
                z = ((DwgMText) obj).getInsertionPoint()[2];
                if (z != 0.0)
                    dwg3DFile = true;
            } else if (obj instanceof DwgPoint) {
                z = ((DwgPoint) obj).getPoint()[2];
                if (z != 0.0)
                    dwg3DFile = true;
            } else if (obj instanceof DwgPolyline2D) {
                z = ((DwgPolyline2D) obj).getElevation();
                if (z != 0.0)
                    dwg3DFile = true;
            } else if (obj instanceof DwgPolyline3D) {
                if (((DwgPolyline3D) obj).getPts() != null) {
                    double[][] pts = ((DwgPolyline3D) obj).getPts();
                    for( int j = 0; j < pts.length; j++ ) {
                        z = pts[j][2];
                        if (z != 0.0)
                            dwg3DFile = true;
                    }
                }
            } else if (obj instanceof DwgSolid) {
                z = ((DwgSolid) obj).getElevation();
                if (z != 0.0)
                    dwg3DFile = true;
            } else if (obj instanceof DwgSpline) {
                double[][] pts = ((DwgSpline) obj).getControlPoints();
                for( int j = 0; j < pts.length; j++ ) {
                    z = pts[j][2];
                    if (z != 0.0)
                        dwg3DFile = true;
                }
            } else if (obj instanceof DwgText) {
                z = ((DwgText) obj).getElevation();
                if (z != 0.0)
                    dwg3DFile = true;
            }
        }
    }

    /**
     * Add a DWG section offset to the dwgSectionOffsets vector
     * 
     * @param key Define the DWG section
     * @param seek Offset of the section
     * @param size Size of the section
     */
    public void addDwgSectionOffset( String key, int seek, int size ) {
        DwgSectionOffset dso = new DwgSectionOffset(key, seek, size);
        dwgSectionOffsets.add(dso);
    }

    /**
     * Returns the offset of DWG section given by its key 
     * 
     * @param key Define the DWG section
     * @return int Offset of the section in the DWG file
     */
    public int getDwgSectionOffset( String key ) {
        int offset = 0;
        for( int i = 0; i < dwgSectionOffsets.size(); i++ ) {
            DwgSectionOffset dso = (DwgSectionOffset) dwgSectionOffsets.get(i);
            String ikey = dso.getKey();
            if (key.equals(ikey)) {
                offset = dso.getSeek();
                break;
            }
        }
        return offset;
    }

    /**
     * Add a DWG object offset to the dwgObjectOffsets vector
     * 
     * @param handle Object handle
     * @param offset Offset of the object data in the DWG file
     */
    public void addDwgObjectOffset( int handle, int offset ) {
        DwgObjectOffset doo = new DwgObjectOffset(handle, offset);
        dwgObjectOffsets.add(doo);
    }

    /**
     * 
     * Add a DWG object to the dwgObject vector
     * 
     * @param dwgObject DWG object
     */
    public void addDwgObject( DwgObject dwgObject ) {
        dwgObjects.add(dwgObject);
    }

    /**
     * Add a DWG class to the dwgClasses vector
     * 
     * @param dwgClass DWG class
     */
    public void addDwgClass( DwgClass dwgClass ) {
        System.out.println("DwgFile.addDwgClass() executed ...");
        dwgClasses.add(dwgClass);
    }

    /**
     * @return Returns the dwgObjectOffsets.
     */
    public Vector getDwgObjectOffsets() {
        return dwgObjectOffsets;
    }

    /**
     * @return Returns the dwgObjects.
     */
    public Vector getDwgObjects() {
        return dwgObjects;
    }

    /**
     * @return Returns the fileName.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return Returns the dwg3DFile.
     */
    public boolean isDwg3DFile() {
        return dwg3DFile;
    }

    /**
     * @param dwg3DFile The dwg3DFile to set.
     */
    public void setDwg3DFile( boolean dwg3DFile ) {
        this.dwg3DFile = dwg3DFile;
    }

    public Vector<String> getLayerNames() {
        return layerNames;
    }
}
