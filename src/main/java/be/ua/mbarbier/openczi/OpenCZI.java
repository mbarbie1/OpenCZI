/*
 * Copyright 2018 mbarbier.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.ua.mbarbier.openczi;

import be.ua.mbarbier.openczi.lib.LibIO;
import static be.ua.mbarbier.openczi.lib.LibIO.byteToBufferedImage;
import static be.ua.mbarbier.openczi.lib.LibIO.byteToShort;
import static be.ua.mbarbier.openczi.lib.LibIO.showBufferedImage;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author mbarbier
 */
public class OpenCZI {

	public static void main( String[] args ) {

		File folder_B21 = new File( "D:/d_data/astrid/Beerse21-AT8-PT25-NeuN-DAPI" );
		File folder_B31 = new File( "D:/d_data/astrid/Beerse31-AT8-PT25-NeuN-DAPI" );
		File folder_B38 = new File( "G:/data/data_astrid_B38/CZI-Beerse38-AT8_PT25_NeuN_DAPI" );

		String contains = "66";
		String doesNotContain = "ryrh";
		ArrayList<String> listOfPossibleLowerCaseExtensions = new ArrayList<>();
		listOfPossibleLowerCaseExtensions.add("czi");
		ArrayList<File> fileList = LibIO.findFiles( folder_B38, contains, doesNotContain, listOfPossibleLowerCaseExtensions );
		
		for ( File file : fileList ) {
			IJ.log( "START opening: " + file.getAbsolutePath() );
			//ImagePlus imp;
			//byte[] b = LibIO.readImageByte( file );
			LibIO.simpleReadImage( file );
			
			short[] sb = byteToShort( LibIO.readImageByte( file ) );
			//System.out.print( Byte.toString(b[46346]) );
			int width = 36928;
			int height = 27776;
			//BufferedImage bi = byteToBufferedImage( b );
			ImageProcessor ip = new ShortProcessor( width, height, sb, null );
			ImagePlus imp = new ImagePlus( file.getName(), ip );
			imp.show();
			//showBufferedImage( bi );
			//File outputfile = new File("G:/data/image.jpg");
			//ImageIO.write(bi, "jpg", outputfile);
			//Graphics bg = bi.getGraphics();
			//bg.drawImage(bi, 0, 0, null);
			//bg.dispose();
			//return bi;
			//imp.show();
			IJ.log( "END opening image");
		}
	}
}
