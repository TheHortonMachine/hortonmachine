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
package org.hortonmachine.gears.io.dxfdwg.libs.dwg.utils;

import java.awt.Color;

/**
 * This class allows to convert Autocad colors in Java colors
 * 
 * @author jmorell
 */
public class AcadColor {
    /**
     * When Autocad color is 256, then the color of the object is the color of it
     * layer
     */
	public final static int BYLAYER = 256;
    /**
     * When Autocad color is 0, then the color of the object is the color of the
     * block where it is placed
     */
	public final static int BYBLOCK = 0;
	private static AcadColor[] colors = initTable();
	private int code;
	private float r0, g0, b0;
	private int r, g, b;
	private Color color = null;
	
	/**
	 * This method is used to build the Autocad color table
	 * 
	 * @param code Code is the Autocad color number
	 * @param r0 r0 is the red component (0-1)
	 * @param g0 g0 is the green component (0-1)
	 * @param b0 b0 is the blue component (0-1)
	 * @param r r0 is the red component (0-255)
	 * @param g g0 is the green component (0-255)
	 * @param b b0 is the blue component (0-255)
	 */
	public AcadColor(int code, double r0, double g0, double b0, double r, double g, double b) {
		this.code = code;
		this.r0 = (float) r0; this.g0 = (float) g0; this.b0 = (float) b0;
		this.r = (int) r; this.g = (int) g; this.b = (int) b;
		this.color = new Color(this.r, this.g, this.b);
		colors[code] = this;
	}
	
	/**
	 * This method uses Autocad color table for convert a color in the Autocad color
	 * code in a java Color
	 * 
	 * @param code This int is the Autocad color number
	 * @return Color Java Color corresponding to the Autocad color number argument
	 */
	public static Color getColor(int code) {
		return colors[code].color;
	}
	
	/**
	 * Initialize an Autocad color table
	 * 
	 * @return AcadColor[] Object of this class that represents Acad color to Java color
	 * 		   conversion
	 */
	public static AcadColor[] initTable() {
		colors = new AcadColor[256];

		new AcadColor(0, 0, 0, 0, 0, 0, 0);
		
		new AcadColor(1, 1, 0, 0, 255, 0, 0);
		new AcadColor(2, 1, 1, 0, 255, 255, 0);
		new AcadColor(3, 0, 1, 0, 0, 255, 0);
		new AcadColor(4, 0, 1, 1, 0, 255, 255);
		new AcadColor(5, 0, 0, 1, 0, 0, 255);
		new AcadColor(6, 1, 0, 1, 255, 0, 255);
		new AcadColor(7, 0, 0, 0, 0, 0, 0);
		new AcadColor(8, 0, 0, 0, 0, 0, 0);
		new AcadColor(9, 0, 0, 0, 0, 0, 0);
		new AcadColor(10, 1, 0, 0, 255, 0, 0);
		new AcadColor(11, 1.0, 0.5, 0.5, 255.0, 127.5, 127.5);
		new AcadColor(12, 0.65, 0, 0, 165.75, 0, 0);
		new AcadColor(13, 0.65, 0.325, 0.325, 165.75, 82.875, 82.875);
		new AcadColor(14, 0.5, 0, 0, 127.5, 0, 0);
		new AcadColor(15, 0.5, 0.25, 0.25, 127.5, 63.75, 63.75);
		new AcadColor(16, 0.3, 0, 0, 76.5, 0, 0);
		new AcadColor(17, 0.3, 0.15, 0.15, 76.5, 38.25, 38.25);
		new AcadColor(18, 0.15, 0, 0, 38.25, 0, 0);
		new AcadColor(19, 0.15, 0.075, 0.075, 38.25, 19.125, 19.125);
		new AcadColor(20, 1, 0.25, 0, 255, 63.75, 0);
		new AcadColor(21, 1, 0.625, 0.5, 255, 159.375, 127.5);
		new AcadColor(22, 0.65, 0.1625, 0, 165.75, 41.4375, 0);
		new AcadColor(23, 0.65, 0.4063, 0.325, 165.75, 103.6065, 82.875);
		new AcadColor(24, 0.5, 0.125, 0, 127.5, 31.875, 0);
		new AcadColor(25, 0.5, 0.3125, 0.25, 127.5, 79.6875, 63.75);
		new AcadColor(26, 0.3, 0.075, 0, 76.5, 19.125, 0);
		new AcadColor(27, 0.3, 0.1875, 0.15, 76.5, 47.8125, 38.25);
		new AcadColor(28, 0.15, 0.0375, 0, 38.25, 9.5625, 0);
		new AcadColor(29, 0.15, 0.0938, 0.075, 38.25, 23.919, 19.125);
		new AcadColor(30, 1, 0.5, 0, 255, 127.5, 0);
		new AcadColor(31, 1, 0.75, 0.5, 255, 191.25, 127.5);
		new AcadColor(32, 0.65, 0.325, 0, 165.75, 82.875, 0);
		new AcadColor(33, 0.65, 0.4875, 0.325, 165.75, 124.3125, 82.875);
		new AcadColor(34, 0.5, 0.25, 0, 127.5, 63.75, 0);
		new AcadColor(35, 0.5, 0.375, 0.25, 127.5, 95.625, 63.75);
		new AcadColor(36, 0.3, 0.15, 0, 76.5, 38.25, 0);
		new AcadColor(37, 0.3, 0.225, 0.15, 76.5, 57.375, 38.25);
		new AcadColor(38, 0.15, 0.075, 0, 38.25, 19.125, 0);
		new AcadColor(39, 0.15, 0.1125, 0.075, 38.25, 28.6875, 19.125);
		new AcadColor(40, 1, 0.75, 0, 255, 191.25, 0);
		new AcadColor(41, 1, 0.875, 0.5, 255, 223.125, 127.5);
		new AcadColor(42, 0.65, 0.4875, 0, 165.75, 124.3125, 0);
		new AcadColor(43, 0.65, 0.5688, 0.325, 165.75, 145.044, 82.875);
		new AcadColor(44, 0.5, 0.375, 0, 127.5, 95.625, 0);
		new AcadColor(45, 0.5, 0.4375, 0.25, 127.5, 111.5625, 63.75);
		new AcadColor(46, 0.3, 0.225, 0, 76.5, 57.375, 0);
		new AcadColor(47, 0.3, 0.2625, 0.15, 76.5, 66.9375, 38.25);
		new AcadColor(48, 0.15, 0.1125, 0, 38.25, 28.6875, 0);
		new AcadColor(49, 0.15, 0.1313, 0.075, 38.25, 33.4815, 19.125);
		new AcadColor(50, 1, 1, 0, 255, 255, 0);
		new AcadColor(51, 1, 1, 0.5, 255, 255, 127.5);
		new AcadColor(52, 0.65, 0.65, 0, 165.75, 165.75, 0);
		new AcadColor(53, 0.65, 0.65, 0.325, 165.75, 165.75, 82.875);
		new AcadColor(54, 0.5, 0.5, 0, 127.5, 127.5, 0);
		new AcadColor(55, 0.5, 0.5, 0.25, 127.5, 127.5, 63.75);
		new AcadColor(56, 0.3, 0.3, 0, 76.5, 76.5, 0);
		new AcadColor(57, 0.3, 0.3, 0.15, 76.5, 76.5, 38.25);
		new AcadColor(58, 0.15, 0.15, 0, 38.25, 38.25, 0);
		new AcadColor(59, 0.15, 0.15, 0.075, 38.25, 38.25, 19.125);
		new AcadColor(60, 0.75, 1, 0, 191.25, 255, 0);
		new AcadColor(61, 0.875, 1, 0.5, 223.125, 255, 127.5);
		new AcadColor(62, 0.4875, 0.65, 0, 124.3125, 165.75, 0);
		new AcadColor(63, 0.5688, 0.65, 0.325, 145.044, 165.75, 82.875);
		new AcadColor(64, 0.375, 0.5, 0, 95.625, 127.5, 0);
		new AcadColor(65, 0.4375, 0.5, 0.25, 111.5625, 127.5, 63.75);
		new AcadColor(66, 0.225, 0.3, 0, 57.375, 76.5, 0);
		new AcadColor(67, 0.2625, 0.3, 0.15, 66.9375, 76.5, 38.25);
		new AcadColor(68, 0.1125, 0.15, 0, 28.6875, 38.25, 0);
		new AcadColor(69, 0.1313, 0.15, 0.075, 33.4815, 38.25, 19.125);
		new AcadColor(70, 0.5, 1, 0, 127.5, 255, 0);
		new AcadColor(71, 0.75, 1, 0.5, 191.25, 255, 127.5);
		new AcadColor(72, 0.325, 0.65, 0, 82.875, 165.75, 0);
		new AcadColor(73, 0.4875, 0.65, 0.325, 124.3125, 165.75, 82.875);
		new AcadColor(74, 0.25, 0.5, 0, 63.75, 127.5, 0);
		new AcadColor(75, 0.375, 0.5, 0.25, 95.625, 127.5, 63.75);
		new AcadColor(76, 0.15, 0.3, 0, 38.25, 76.5, 0);
		new AcadColor(77, 0.225, 0.3, 0.15, 57.375, 76.5, 38.25);
		new AcadColor(78, 0.075, 0.15, 0, 19.125, 38.25, 0);
		new AcadColor(79, 0.1125, 0.15, 0.075, 28.6875, 38.25, 19.125);
		new AcadColor(80, 0.25, 1, 0, 63.75, 255, 0);
		new AcadColor(81, 0.625, 1, 0.5, 159.375, 255, 127.5);
		new AcadColor(82, 0.1625, 0.65, 0, 41.4375, 165.75, 0);
		new AcadColor(83, 0.4063, 0.65, 0.325, 103.6065, 165.75, 82.875);
		new AcadColor(84, 0.125, 0.5, 0, 31.875, 127.5, 0);
		new AcadColor(85, 0.3125, 0.5, 0.25, 79.6875, 127.5, 63.75);
		new AcadColor(86, 0.075, 0.3, 0, 19.125, 76.5, 0);
		new AcadColor(87, 0.1875, 0.3, 0.15, 47.8125, 76.5, 38.25);
		new AcadColor(88, 0.0375, 0.15, 0, 9.5625, 38.25, 0);
		new AcadColor(89, 0.0938, 0.15, 0.075, 23.919, 38.25, 19.125);
		new AcadColor(90, 0, 1, 0, 0, 255, 0);
		new AcadColor(91, 0.5, 1, 0.5, 127.5, 255, 127.5);
		new AcadColor(92, 0, 0.65, 0, 0, 165.75, 0);
		new AcadColor(93, 0.325, 0.65, 0.325, 82.875, 165.75, 82.875);
		new AcadColor(94, 0, 0.5, 0, 0, 127.5, 0);
		new AcadColor(95, 0.25, 0.5, 0.25, 63.75, 127.5, 63.75);
		new AcadColor(96, 0, 0.3, 0, 0, 76.5, 0);
		new AcadColor(97, 0.15, 0.3, 0.15, 38.25, 76.5, 38.25);
		new AcadColor(98, 0, 0.15, 0, 0, 38.25, 0);
		new AcadColor(99, 0.075, 0.15, 0.075, 19.125, 38.25, 19.125);
		new AcadColor(100, 0, 1, 0.25, 0, 255, 63.75);
		new AcadColor(101, 0.5, 1, 0.625, 127.5, 255, 159.375);
		new AcadColor(102, 0, 0.65, 0.1625, 0, 165.75, 41.4375);
		new AcadColor(103, 0.325, 0.65, 0.4063, 82.875, 165.75, 103.6065);
		new AcadColor(104, 0, 0.5, 0.125, 0, 127.5, 31.875);
		new AcadColor(105, 0.25, 0.5, 0.3125, 63.75, 127.5, 79.6875);
		new AcadColor(106, 0, 0.3, 0.075, 0, 76.5, 19.125);
		new AcadColor(107, 0.15, 0.3, 0.1875, 38.25, 76.5, 47.8125);
		new AcadColor(108, 0, 0.15, 0.0375, 0, 38.25, 9.5625);
		new AcadColor(109, 0.075, 0.15, 0.0938, 19.125, 38.25, 23.919);
		new AcadColor(110, 0, 1, 0.5, 0, 255, 127.5);
		new AcadColor(111, 0.5, 1, 0.75, 127.5, 255, 191.25);
		new AcadColor(112, 0, 0.65, 0.325, 0, 165.75, 82.875);
		new AcadColor(113, 0.325, 0.65, 0.4875, 82.875, 165.75, 124.3125);
		new AcadColor(114, 0, 0.5, 0.25, 0, 127.5, 63.75);
		new AcadColor(115, 0.25, 0.5, 0.375, 63.75, 127.5, 95.625);
		new AcadColor(116, 0, 0.3, 0.15, 0, 76.5, 38.25);
		new AcadColor(117, 0.15, 0.3, 0.225, 38.25, 76.5, 57.375);
		new AcadColor(118, 0, 0.15, 0.075, 0, 38.25, 19.125);
		new AcadColor(119, 0.075, 0.15, 0.1125, 19.125, 38.25, 28.6875);
		new AcadColor(120, 0, 1, 0.75, 0, 255, 191.25);
		new AcadColor(121, 0.5, 1, 0.875, 127.5, 255, 223.125);
		new AcadColor(122, 0, 0.65, 0.4875, 0, 165.75, 124.3125);
		new AcadColor(123, 0.325, 0.65, 0.5688, 82.875, 165.75, 145.044);
		new AcadColor(124, 0, 0.5, 0.375, 0, 127.5, 95.625);
		new AcadColor(125, 0.25, 0.5, 0.4375, 63.75, 127.5, 111.5625);
		new AcadColor(126, 0, 0.3, 0.225, 0, 76.5, 57.375);
		new AcadColor(127, 0.15, 0.3, 0.2625, 38.25, 76.5, 66.9375);
		new AcadColor(128, 0, 0.15, 0.1125, 0, 38.25, 28.6875);
		new AcadColor(129, 0.075, 0.15, 0.1313, 19.125, 38.25, 33.4815);
		new AcadColor(130, 0, 1, 1, 0, 255, 255);
		new AcadColor(131, 0.5, 1, 1, 127.5, 255, 255);
		new AcadColor(132, 0, 0.65, 0.65, 0, 165.75, 165.75);
		new AcadColor(133, 0.325, 0.65, 0.65, 82.875, 165.75, 165.75);
		new AcadColor(134, 0, 0.5, 0.5, 0, 127.5, 127.5);
		new AcadColor(135, 0.25, 0.5, 0.5, 63.75, 127.5, 127.5);
		new AcadColor(136, 0, 0.3, 0.3, 0, 76.5, 76.5);
		new AcadColor(137, 0.15, 0.3, 0.3, 38.25, 76.5, 76.5);
		new AcadColor(138, 0, 0.15, 0.15, 0, 38.25, 38.25);
		new AcadColor(139, 0.075, 0.15, 0.15, 19.125, 38.25, 38.25);
		new AcadColor(140, 0, 0.75, 1, 0, 191.25, 255);
		new AcadColor(141, 0.5, 0.875, 1, 127.5, 223.125, 255);
		new AcadColor(142, 0, 0.4875, 0.65, 0, 124.3125, 165.75);
		new AcadColor(143, 0.325, 0.5688, 0.65, 82.875, 145.044, 165.75);
		new AcadColor(144, 0, 0.375, 0.5, 0, 95.625, 127.5);
		new AcadColor(145, 0.25, 0.4375, 0.5, 63.75, 111.5625, 127.5);
		new AcadColor(146, 0, 0.225, 0.3, 0, 57.375, 76.5);
		new AcadColor(147, 0.15, 0.2625, 0.3, 38.25, 66.9375, 76.5);
		new AcadColor(148, 0, 0.1125, 0.15, 0, 28.6875, 38.25);
		new AcadColor(149, 0.075, 0.1313, 0.15, 19.125, 33.4815, 38.25);
		new AcadColor(150, 0, 0.5, 1, 0, 127.5, 255);
		new AcadColor(151, 0.5, 0.75, 1, 127.5, 191.25, 255);
		new AcadColor(152, 0, 0.325, 0.65, 0, 82.875, 165.75);
		new AcadColor(153, 0.325, 0.4875, 0.65, 82.875, 124.3125, 165.75);
		new AcadColor(154, 0, 0.25, 0.5, 0, 63.75, 127.5);
		new AcadColor(155, 0.25, 0.375, 0.5, 63.75, 95.625, 127.5);
		new AcadColor(156, 0, 0.15, 0.3, 0, 38.25, 76.5);
		new AcadColor(157, 0.15, 0.225, 0.3, 38.25, 57.375, 76.5);
		new AcadColor(158, 0, 0.075, 0.15, 0, 19.125, 38.25);
		new AcadColor(159, 0.075, 0.1125, 0.15, 19.125, 28.6875, 38.25);
		new AcadColor(160, 0, 0.25, 1, 0, 63.75, 255);
		new AcadColor(161, 0.5, 0.625, 1, 127.5, 159.375, 255);
		new AcadColor(162, 0, 0.1625, 0.65, 0, 41.4375, 165.75);
		new AcadColor(163, 0.325, 0.4063, 0.65, 82.875, 103.6065, 165.75);
		new AcadColor(164, 0, 0.125, 0.5, 0, 31.875, 127.5);
		new AcadColor(165, 0.25, 0.3125, 0.5, 63.75, 79.6875, 127.5);
		new AcadColor(166, 0, 0.075, 0.3, 0, 19.125, 76.5);
		new AcadColor(167, 0.15, 0.1875, 0.3, 38.25, 47.8125, 76.5);
		new AcadColor(168, 0, 0.0375, 0.15, 0, 9.5625, 38.25);
		new AcadColor(169, 0.075, 0.0938, 0.15, 19.125, 23.919, 38.25);
		new AcadColor(170, 0, 0, 1, 0, 0, 255);
		new AcadColor(171, 0.5, 0.5, 1, 127.5, 127.5, 255);
		new AcadColor(172, 0, 0, 0.65, 0, 0, 165.75);
		new AcadColor(173, 0.325, 0.325, 0.65, 82.875, 82.875, 165.75);
		new AcadColor(174, 0, 0, 0.5, 0, 0, 127.5);
		new AcadColor(175, 0.25, 0.25, 0.5, 63.75, 63.75, 127.5);
		new AcadColor(176, 0, 0, 0.3, 0, 0, 76.5);
		new AcadColor(177, 0.15, 0.15, 0.3, 38.25, 38.25, 76.5);
		new AcadColor(178, 0, 0, 0.15, 0, 0, 38.25);
		new AcadColor(179, 0.075, 0.075, 0.15, 19.125, 19.125, 38.25);
		new AcadColor(180, 0.25, 0, 1, 63.75, 0, 255);
		new AcadColor(181, 0.625, 0.5, 1, 159.375, 127.5, 255);
		new AcadColor(182, 0.1625, 0, 0.65, 41.4375, 0, 165.75);
		new AcadColor(183, 0.4063, 0.325, 0.65, 103.6065, 82.875, 165.75);
		new AcadColor(184, 0.125, 0, 0.5, 31.875, 0, 127.5);
		new AcadColor(185, 0.3125, 0.25, 0.5, 79.6875, 63.75, 127.5);
		new AcadColor(186, 0.075, 0, 0.3, 19.125, 0, 76.5);
		new AcadColor(187, 0.1875, 0.15, 0.3, 47.8125, 38.25, 76.5);
		new AcadColor(188, 0.0375, 0, 0.15, 9.5625, 0, 38.25);
		new AcadColor(189, 0.0938, 0.075, 0.15, 23.919, 19.125, 38.25);
		new AcadColor(190, 0.5, 0, 1, 127.5, 0, 255);
		new AcadColor(191, 0.75, 0.5, 1, 191.25, 127.5, 255);
		new AcadColor(192, 0.325, 0, 0.65, 82.875, 0, 165.75);
		new AcadColor(193, 0.4875, 0.325, 0.65, 124.3125, 82.875, 165.75);
		new AcadColor(194, 0.25, 0, 0.5, 63.75, 0, 127.5);
		new AcadColor(195, 0.375, 0.25, 0.5, 95.625, 63.75, 127.5);
		new AcadColor(196, 0.15, 0, 0.3, 38.25, 0, 76.5);
		new AcadColor(197, 0.225, 0.15, 0.3, 57.375, 38.25, 76.5);
		new AcadColor(198, 0.075, 0, 0.15, 19.125, 0, 38.25);
		new AcadColor(199, 0.1125, 0.075, 0.15, 28.6875, 19.125, 38.25);
		new AcadColor(200, 0.75, 0, 1, 191.25, 0, 255);
		new AcadColor(201, 0.875, 0.5, 1, 223.125, 127.5, 255);
		new AcadColor(202, 0.4875, 0, 0.65, 124.3125, 0, 165.75);
		new AcadColor(203, 0.5688, 0.325, 0.65, 145.044, 82.875, 165.75);
		new AcadColor(204, 0.375, 0, 0.5, 95.625, 0, 127.5);
		new AcadColor(205, 0.4375, 0.25, 0.5, 111.5625, 63.75, 127.5);
		new AcadColor(206, 0.225, 0, 0.3, 57.375, 0, 76.5);
		new AcadColor(207, 0.2625, 0.15, 0.3, 66.9375, 38.25, 76.5);
		new AcadColor(208, 0.1125, 0, 0.15, 28.6875, 0, 38.25);
		new AcadColor(209, 0.1313, 0.075, 0.15, 33.4815, 19.125, 38.25);
		new AcadColor(210, 1, 0, 1, 255, 0, 255);
		new AcadColor(211, 1, 0.5, 1, 255, 127.5, 255);
		new AcadColor(212, 0.65, 0, 0.65, 165.75, 0, 165.75);
		new AcadColor(213, 0.65, 0.325, 0.65, 165.75, 82.875, 165.75);
		new AcadColor(214, 0.5, 0, 0.5, 127.5, 0, 127.5);
		new AcadColor(215, 0.5, 0.25, 0.5, 127.5, 63.75, 127.5);
		new AcadColor(216, 0.3, 0, 0.3, 76.5, 0, 76.5);
		new AcadColor(217, 0.3, 0.15, 0.3, 76.5, 38.25, 76.5);
		new AcadColor(218, 0.15, 0, 0.15, 38.25, 0, 38.25);
		new AcadColor(219, 0.15, 0.075, 0.15, 38.25, 19.125, 38.25);
		new AcadColor(220, 1, 0, 0.75, 255, 0, 191.25);
		new AcadColor(221, 1, 0.5, 0.875, 255, 127.5, 223.125);
		new AcadColor(222, 0.65, 0, 0.4875, 165.75, 0, 124.3125);
		new AcadColor(223, 0.65, 0.325, 0.5688, 165.75, 82.875, 145.044);
		new AcadColor(224, 0.5, 0, 0.375, 127.5, 0, 95.625);
		new AcadColor(225, 0.5, 0.25, 0.4375, 127.5, 63.75, 111.5625);
		new AcadColor(226, 0.3, 0, 0.225, 76.5, 0, 57.375);
		new AcadColor(227, 0.3, 0.15, 0.2625, 76.5, 38.25, 66.9375);
		new AcadColor(228, 0.15, 0, 0.1125, 38.25, 0, 28.6875);
		new AcadColor(229, 0.15, 0.075, 0.1313, 38.25, 19.125, 33.4815);
		new AcadColor(230, 1, 0, 0.5, 255, 0, 127.5);
		new AcadColor(231, 1, 0.5, 0.75, 255, 127.5, 191.25);
		new AcadColor(232, 0.65, 0, 0.325, 165.75, 0, 82.875);
		new AcadColor(233, 0.65, 0.325, 0.4875, 165.75, 82.875, 124.3125);
		new AcadColor(234, 0.5, 0, 0.25, 127.5, 0, 63.75);
		new AcadColor(235, 0.5, 0.25, 0.375, 127.5, 63.75, 95.625);
		new AcadColor(236, 0.3, 0, 0.15, 76.5, 0, 38.25);
		new AcadColor(237, 0.3, 0.15, 0.225, 76.5, 38.25, 57.375);
		new AcadColor(238, 0.15, 0, 0.075, 38.25, 0, 19.125);
		new AcadColor(239, 0.15, 0.075, 0.1125, 38.25, 19.125, 28.6875);
		new AcadColor(240, 1, 0, 0.25, 255, 0, 63.75);
		new AcadColor(241, 1, 0.5, 0.625, 255, 127.5, 159.375);
		new AcadColor(242, 0.65, 0, 0.1625, 165.75, 0, 41.4375);
		new AcadColor(243, 0.65, 0.325, 0.4063, 165.75, 82.875, 103.6065);
		new AcadColor(244, 0.5, 0, 0.125, 127.5, 0, 31.875);
		new AcadColor(245, 0.5, 0.25, 0.3125, 127.5, 63.75, 79.6875);
		new AcadColor(246, 0.3, 0, 0.075, 76.5, 0, 19.125);
		new AcadColor(247, 0.3, 0.15, 0.1875, 76.5, 38.25, 47.8125);
		new AcadColor(248, 0.15, 0, 0.0375, 38.25, 0, 9.5625);
		new AcadColor(249, 0.15, 0.075, 0.0938, 38.25, 19.125, 23.919);
		new AcadColor(250, 0.33, 0.33, 0.33, 84.15, 84.15, 84.15);
		new AcadColor(251, 0.464, 0.464, 0.464, 118.32, 118.32, 118.32);
		new AcadColor(252, 0.598, 0.598, 0.598, 152.49, 152.49, 152.49);
		new AcadColor(253, 0.732, 0.732, 0.732, 186.66, 186.66, 186.66);
		new AcadColor(254, 0.866, 0.866, 0.866, 220.83, 220.83, 220.83);
		new AcadColor(255, 1.0, 1.0, 1.0, 255.0, 255.0, 255.0);
		return AcadColor.colors;
	}
}
