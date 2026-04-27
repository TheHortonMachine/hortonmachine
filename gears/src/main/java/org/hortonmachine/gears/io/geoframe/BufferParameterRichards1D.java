
/*
 * GNU GPL v3 License
 *
 * Copyright 2020 Niccolo` Tubini
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
 */

package org.hortonmachine.gears.io.geoframe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import oms3.annotations.*;

@Description("Prepare parameter vector for calibration.")
@Documentation("")
@Author(name = "Niccolo' Tubini, Riccardo Rigon", contact = "tubini.niccolo@gmail.com")
@Keywords("Hydrology, Richards, WHETGEO-1D")
//@Label(JGTConstants.HYDROGEOMORPHOLOGY)
//@Name("shortradbal")
//@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")

public class BufferParameterRichards1D {
	
	@Description("Adimensional water content at saturation.")
	@In 
	@Unit ("-")
	public double thetaS1;
	
	@Description("Adimensional water content at saturation.")
	@In 
	@Unit ("-")
	public double thetaS2;
	
	@Description("Adimensional water content at saturation.")
	@In 
	@Unit ("-")
	public double thetaS3;
	
	@Description("Adimensional water content at saturation.")
	@In 
	@Unit ("-")
	public double thetaS4;
	
	@Description("Adimensional water content at saturation.")
	@In 
	@Unit ("-")
	public double thetaS5;
	
	@Description("Adimensional water content at saturation.")
	@In 
	@Unit ("-")
	public double thetaS6;
	
	@Description("Adimensional water content at saturation.")
	@In 
	@Unit ("-")
	public double thetaS7;
	
	@Description("Adimensional water content at saturation.")
	@In 
	@Unit ("-")
	public double thetaS8;
	
	@Description("Adimensional residual water content.")
	@In 
	@Unit ("-")
	public double thetaR1;
	
	@Description("Adimensional residual water content.")
	@In 
	@Unit ("-")
	public double thetaR2;
	
	@Description("Adimensional residual water content.")
	@In 
	@Unit ("-")
	public double thetaR3;
	
	@Description("Adimensional residual water content.")
	@In 
	@Unit ("-")
	public double thetaR4;
	
	@Description("Adimensional residual water content.")
	@In 
	@Unit ("-")
	public double thetaR5;
	
	@Description("Adimensional residual water content.")
	@In 
	@Unit ("-")
	public double thetaR6;
	
	@Description("Adimensional residual water content.")
	@In 
	@Unit ("-")
	public double thetaR7;
	
	@Description("Adimensional residual water content.")
	@In 
	@Unit ("-")
	public double thetaR8;
	
	@Description("Soil water riterntion curva parameter 1.")
	@In 
	@Unit ("-")
	public double par1SWRC1;
	
	@Description("Soil water riterntion curva parameter 1.")
	@In 
	@Unit ("-")
	public double par1SWRC2;
	
	@Description("Soil water riterntion curva parameter 1.")
	@In 
	@Unit ("-")
	public double par1SWRC3;
	
	@Description("Soil water riterntion curva parameter 1.")
	@In 
	@Unit ("-")
	public double par1SWRC4;
	
	@Description("Soil water riterntion curva parameter 1.")
	@In 
	@Unit ("-")
	public double par1SWRC5;
	
	
	@Description("Soil water riterntion curva parameter 1.")
	@In 
	@Unit ("-")
	public double par1SWRC6;
	
	
	@Description("Soil water riterntion curva parameter 1.")
	@In 
	@Unit ("-")
	public double par1SWRC7;
	
	@Description("Soil water riterntion curva parameter 1.")
	@In 
	@Unit ("-")
	public double par1SWRC8;
	
	@Description("Soil water riterntion curva parameter 2.")
	@In 
	@Unit ("-")
	public double par2SWRC1;
	
	@Description("Soil water riterntion curva parameter 2.")
	@In 
	@Unit ("-")
	public double par2SWRC2;
	
	@Description("Soil water riterntion curva parameter 2.")
	@In 
	@Unit ("-")
	public double par2SWRC3;	
	
	@Description("Soil water riterntion curva parameter 2.")
	@In 
	@Unit ("-")
	public double par2SWRC4;
	
	@Description("Soil water riterntion curva parameter 2.")
	@In 
	@Unit ("-")
	public double par2SWRC5;
	
	@Description("Soil water riterntion curva parameter 2.")
	@In 
	@Unit ("-")
	public double par2SWRC6;
	
	@Description("Soil water riterntion curva parameter 2.")
	@In 
	@Unit ("-")
	public double par2SWRC7;
	
	@Description("Soil water riterntion curva parameter 2.")
	@In 
	@Unit ("-")
	public double par2SWRC8;	
	
	@Description("Soil water riterntion curva parameter 3.")
	@In 
	@Unit ("-")
	public double par3SWRC1;
	
	@Description("Soil water riterntion curva parameter 3.")
	@In 
	@Unit ("-")
	public double par3SWRC2;
	
	@Description("Soil water riterntion curva parameter 3.")
	@In 
	@Unit ("-")
	public double par3SWRC3;
	
	@Description("Soil water riterntion curva parameter 3.")
	@In 
	@Unit ("-")
	public double par3SWRC4;
	
	@Description("Soil water riterntion curva parameter 3.")
	@In 
	@Unit ("-")
	public double par3SWRC5;
	
	@Description("Soil water riterntion curva parameter 3.")
	@In 
	@Unit ("-")
	public double par3SWRC6;
	
	@Description("Soil water riterntion curva parameter 3.")
	@In 
	@Unit ("-")
	public double par3SWRC7;
	
	@Description("Soil water riterntion curva parameter 3.")
	@In 
	@Unit ("-")
	public double par3SWRC8;
	
	@Description("Soil water riterntion curva parameter 4.")
	@In 
	@Unit ("-")
	public double par4SWRC1;
	
	@Description("Soil water riterntion curva parameter 4.")
	@In 
	@Unit ("-")
	public double par4SWRC2;
	
	@Description("Soil water riterntion curva parameter 4.")
	@In 
	@Unit ("-")
	public double par4SWRC3;
	
	@Description("Soil water riterntion curva parameter 4.")
	@In 
	@Unit ("-")
	public double par4SWRC4;
	
	@Description("Soil water riterntion curva parameter 4.")
	@In 
	@Unit ("-")
	public double par4SWRC5;
	
	@Description("Soil water riterntion curva parameter 4.")
	@In 
	@Unit ("-")
	public double par4SWRC6;
	
	@Description("Soil water riterntion curva parameter 4.")
	@In 
	@Unit ("-")
	public double par4SWRC7;
	
	@Description("Soil water riterntion curva parameter 4.")
	@In 
	@Unit ("-")
	public double par4SWRC8;
	
	@Description("Soil water riterntion curva parameter 5.")
	@In 
	@Unit ("-")
	public double par5SWRC1;
	
	@Description("Soil water riterntion curva parameter 5.")
	@In 
	@Unit ("-")
	public double par5SWRC2;
	
	@Description("Soil water riterntion curva parameter 5.")
	@In 
	@Unit ("-")
	public double par5SWRC3;	
	
	@Description("Soil water riterntion curva parameter 5.")
	@In 
	@Unit ("-")
	public double par5SWRC4;
	
	@Description("Soil water riterntion curva parameter 5.")
	@In 
	@Unit ("-")
	public double par5SWRC5;
	
	@Description("Soil water riterntion curva parameter 5.")
	@In 
	@Unit ("-")
	public double par5SWRC6;
	
	@Description("Soil water riterntion curva parameter 5.")
	@In 
	@Unit ("-")
	public double par5SWRC7;
	
	@Description("Soil water riterntion curva parameter 5.")
	@In 
	@Unit ("-")
	public double par5SWRC8;	
	
	@Description("Soil compressibility.")
	@In 
	@Unit ("-")
	public double alphaSpecificStorage1;	
	
	@Description("Soil compressibility.")
	@In 
	@Unit ("-")
	public double alphaSpecificStorage2;	
	
	@Description("Soil compressibility.")
	@In 
	@Unit ("-")
	public double alphaSpecificStorage3;
	
	@Description("Soil compressibility.")
	@In 
	@Unit ("-")
	public double alphaSpecificStorage4;
	
	@Description("Soil compressibility.")
	@In 
	@Unit ("-")
	public double alphaSpecificStorage5;
	
	@Description("Soil compressibility.")
	@In 
	@Unit ("-")
	public double alphaSpecificStorage6;
	
	@Description("Soil compressibility.")
	@In 
	@Unit ("-")
	public double alphaSpecificStorage7;
	
	@Description("Soil compressibility.")
	@In 
	@Unit ("-")
	public double alphaSpecificStorage8;
	
	@Description("Water compressibility.")
	@In 
	@Unit ("-")
	public double betaSpecificStorage1;	
	
	@Description("Water compressibility.")
	@In 
	@Unit ("-")
	public double betaSpecificStorage2;	
	
	@Description("Water compressibility.")
	@In 
	@Unit ("-")
	public double betaSpecificStorage3;	
	
	@Description("Water compressibility.")
	@In 
	@Unit ("-")
	public double betaSpecificStorage4;
	
	@Description("Water compressibility.")
	@In 
	@Unit ("-")
	public double betaSpecificStorage5;
	
	@Description("Water compressibility.")
	@In 
	@Unit ("-")
	public double betaSpecificStorage6;
	
	@Description("Water compressibility.")
	@In 
	@Unit ("-")
	public double betaSpecificStorage7;	
	
	@Description("Water compressibility.")
	@In 
	@Unit ("-")
	public double betaSpecificStorage8;	

	@Description("Saturated hydraulic conductivity.")
	@In 
	@Unit ("-")
	public double ks1;	
	
	@Description("Saturated hydraulic conductivity.")
	@In 
	@Unit ("-")
	public double ks2;	
	
	@Description("Saturated hydraulic conductivity.")
	@In 
	@Unit ("-")
	public double ks3;	
	
	@Description("Saturated hydraulic conductivity.")
	@In 
	@Unit ("-")
	public double ks4;
	
	@Description("Saturated hydraulic conductivity.")
	@In 
	@Unit ("-")
	public double ks5;
	
	@Description("Saturated hydraulic conductivity.")
	@In 
	@Unit ("-")
	public double ks6;
	
	@Description("Saturated hydraulic conductivity.")
	@In 
	@Unit ("-")
	public double ks7;
	
	@Description("Saturated hydraulic conductivity.")
	@In 
	@Unit ("-")
	public double ks8;	
	
	
//	@Description("Variable to store")
//	@In 
//	@Unit ("-")
//	public int numberOfLayers;
	
	@Description(" ")
	@Out 
	@Unit ("-")
	public double[] thetaS;
	
	@Description(" ")
	@Out 
	@Unit ("-")
	public double[] thetaR;
	
	@Description(" ")
	@Out 
	@Unit ("-")
	public double[] par1SWRC;
	
	@Description(" ")
	@Out 
	@Unit ("-")
	public double[] par2SWRC;
	
	@Description(" ")
	@Out 
	@Unit ("-")
	public double[] par3SWRC;
	
	@Description(" ")
	@Out 
	@Unit ("-")
	public double[] par4SWRC;
	
	@Description(" ")
	@Out 
	@Unit ("-")
	public double[] par5SWRC;
	
	@Description(" ")
	@Out 
	@Unit ("-")
	public double[] ks;
	
	@Description(" ")
	@Out 
	@Unit ("-")
	public double[] alphaSpecificStorage;
		
	@Description(" ")
	@Out 
	@Unit ("-")
	public double[] betaSpecificStorage;
	
//	private List<Double> tmpThetaS;
//	private List<Double> tmpThetaR;
//	private List<Double> tmpPar1SWRC;
//	private List<Double> tmpPar2SWRC;
//	private List<Double> tmpPar3SWRC;
//	private List<Double> tmpPar4SWRC;
//	private List<Double> tmpPar5SWRC;
//	private List<Double> tmpAlphaSpecificStorage;
//	private List<Double> tmpBetaSpecificStorage;
//	private List<Double> tmpKs;
	
	@Execute
	public void solve() {

		thetaS = new double[9];
		thetaR = new double[9];
		par1SWRC = new double[9];
		par2SWRC = new double[9];
		par3SWRC = new double[9];
		par4SWRC = new double[9];
		par5SWRC = new double[9];
		alphaSpecificStorage = new double[9];
		betaSpecificStorage = new double[9];
		ks = new double[9];

		thetaS[0] = -9999.0;
		thetaS[1] = thetaS1;
		thetaS[2] = thetaS2;
		thetaS[3] = thetaS3;
		thetaS[4] = thetaS4;
		thetaS[5] = thetaS5;
		thetaS[6] = thetaS6;
		thetaS[7] = thetaS7;
		thetaS[8] = thetaS8;
		
		thetaR[0] = -9999.0;
		thetaR[1] = thetaR1;
		thetaR[2] = thetaR2;
		thetaR[3] = thetaR3;
		thetaR[4] = thetaR4;
		thetaR[5] = thetaR5;
		thetaR[6] = thetaR6;
		thetaR[7] = thetaR7;
		thetaR[8] = thetaR8;
		
		par1SWRC[0] = -9999.0;
		par1SWRC[1] = par1SWRC1;
		par1SWRC[2] = par1SWRC2;
		par1SWRC[3] = par1SWRC3;
		par1SWRC[4] = par1SWRC4;
		par1SWRC[5] = par1SWRC5;
		par1SWRC[6] = par1SWRC6;
		par1SWRC[7] = par1SWRC7;
		par1SWRC[8] = par1SWRC8;
		
		par2SWRC[0] = -9999.0;
		par2SWRC[1] = par2SWRC1;
		par2SWRC[2] = par2SWRC2;
		par2SWRC[3] = par2SWRC3;
		par2SWRC[4] = par2SWRC4;
		par2SWRC[5] = par2SWRC5;
		par2SWRC[6] = par2SWRC6;
		par2SWRC[7] = par2SWRC7;
		par2SWRC[8] = par2SWRC8;		
		
		par3SWRC[0] = -9999.0;
		par3SWRC[1] = par3SWRC1;
		par3SWRC[2] = par3SWRC2;
		par3SWRC[3] = par3SWRC3;
		par3SWRC[4] = par3SWRC4;
		par3SWRC[5] = par3SWRC5;
		par3SWRC[6] = par3SWRC6;
		par3SWRC[7] = par3SWRC7;
		par3SWRC[8] = par3SWRC8;		
		
		par4SWRC[0] = -9999.0;
		par4SWRC[1] = par4SWRC1;
		par4SWRC[2] = par4SWRC2;
		par4SWRC[3] = par4SWRC3;
		par4SWRC[4] = par4SWRC4;
		par4SWRC[5] = par4SWRC5;
		par4SWRC[6] = par4SWRC6;
		par4SWRC[7] = par4SWRC7;
		par4SWRC[8] = par4SWRC8;
		
		par5SWRC[0] = -9999.0;
		par5SWRC[1] = par5SWRC1;
		par5SWRC[2] = par5SWRC2;
		par5SWRC[3] = par5SWRC3;
		par5SWRC[4] = par5SWRC4;
		par5SWRC[5] = par5SWRC5;
		par5SWRC[6] = par5SWRC6;
		par5SWRC[7] = par5SWRC7;
		par5SWRC[8] = par5SWRC8;
		
		alphaSpecificStorage[0] = -9999.0;
		alphaSpecificStorage[1] = alphaSpecificStorage1;
		alphaSpecificStorage[2] = alphaSpecificStorage2;
		alphaSpecificStorage[3] = alphaSpecificStorage3;
		alphaSpecificStorage[4] = alphaSpecificStorage4;
		alphaSpecificStorage[5] = alphaSpecificStorage5;
		alphaSpecificStorage[6] = alphaSpecificStorage6;
		alphaSpecificStorage[7] = alphaSpecificStorage7;
		alphaSpecificStorage[8] = alphaSpecificStorage8;
		
		betaSpecificStorage[0] = -9999.0;
		betaSpecificStorage[1] = betaSpecificStorage1;
		betaSpecificStorage[2] = betaSpecificStorage2;
		betaSpecificStorage[3] = betaSpecificStorage3;
		betaSpecificStorage[4] = betaSpecificStorage4;
		betaSpecificStorage[5] = betaSpecificStorage5;
		betaSpecificStorage[6] = betaSpecificStorage6;
		betaSpecificStorage[7] = betaSpecificStorage7;
		betaSpecificStorage[8] = betaSpecificStorage8;
		
		ks[0] = -9999.0;
		ks[1] = ks1;
		ks[2] = ks2;
		ks[3] = ks3;
		ks[4] = ks4;
		ks[5] = ks5;
		ks[6] = ks6;
		ks[7] = ks7;
		ks[8] = ks8;
		

	}
	
	

}