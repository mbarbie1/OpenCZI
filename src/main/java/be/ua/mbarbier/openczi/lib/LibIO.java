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
package be.ua.mbarbier.openczi.lib;

import ij.IJ;
import ij.ImagePlus;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;
import loci.formats.FormatException;
import loci.common.DateTools;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatReader;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.gui.BufferedImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.BF;

import ome.units.quantity.Length;
import ome.units.quantity.Time;
import ome.units.UNITS;

/**
 *
 * @author mbarbier
 */
public class LibIO {
	
	/**
	 *
	 * @param folder
	 * @param contains
	 * @param doesNotContain
	 * @param listOfPossibleLowerCaseExtensions
	 * @return
	 */
	public static ArrayList<File> findFiles(File folder, String contains, String doesNotContain, ArrayList<String> listOfPossibleLowerCaseExtensions ) {

		ArrayList<File> fileList = new ArrayList<File>();
		File[] files = folder.listFiles();
		for (File file : files ) {
			if ( file.isFile() ) {
				String fileName = file.getName();
				if ( fileName.contains(contains) & !fileName.contains(doesNotContain) ) {
					for ( String ext : listOfPossibleLowerCaseExtensions ) {
						if ( fileName.toLowerCase().endsWith(ext) ) {
							fileList.add(file);
						}
					}
				}
			}
		}

		return fileList;
	}

	/**
	 * Outputs dimensional information.
	 */
	public static void printPixelDimensions( IFormatReader reader) {
// output dimensional information
		int sizeX = reader.getSizeX();
		int sizeY = reader.getSizeY();
		int sizeZ = reader.getSizeZ();
		int sizeC = reader.getSizeC();
		int sizeT = reader.getSizeT();
		int imageCount = reader.getImageCount();
		System.out.println();
		System.out.println("Pixel dimensions:");
		System.out.println("\tWidth = " + sizeX);
		System.out.println("\tHeight = " + sizeY);
		System.out.println("\tFocal planes = " + sizeZ);
		System.out.println("\tChannels = " + sizeC);
		System.out.println("\tTimepoints = " + sizeT);
		System.out.println("\tTotal planes = " + imageCount);
	}

	/**
	 * Outputs global timing details.
	 */
	public static void printPhysicalDimensions(IMetadata meta, int series) {
		Length physicalSizeX = meta.getPixelsPhysicalSizeX(series);
		Length physicalSizeY = meta.getPixelsPhysicalSizeY(series);
		Length physicalSizeZ = meta.getPixelsPhysicalSizeZ(series);
		Time timeIncrement = meta.getPixelsTimeIncrement(series);
		System.out.println();
		System.out.println("Physical dimensions:");
		System.out.println("\tX spacing = "
				+ physicalSizeX.value() + " " + physicalSizeX.unit().getSymbol());
		System.out.println("\tY spacing = "
				+ physicalSizeY.value() + " " + physicalSizeY.unit().getSymbol());
		try {
			System.out.println("\tZ spacing = "
					+ physicalSizeZ.value() + " " + physicalSizeZ.unit().getSymbol());
		} catch(NullPointerException e0 ) {
			System.out.println("\tZ spacing = NA");
		}
		try {
			System.out.println("\tTime increment = " + timeIncrement.value(UNITS.SECOND).doubleValue() + " seconds");
		} catch(NullPointerException e0 ) {
			System.out.println("\tTime increment = NA");
		}
	}

	//import loci.plugins.BF;
	//public static ImagePlus readImages(File file) {
	//	
	//	file = "/Users/curtis/data/tubhiswt4D.ome.tif"
	//	ImagePlus[] imps = BF.openImagePlus(file);
	//	for ( ImagePlus imp : imps ) {
	//		imp.show();
	//	}
	//}
	
	public static void showBufferedImage( BufferedImage bi ) {
		JFrame editorFrame = new JFrame("Image Demo");
        editorFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		ImageIcon imageIcon = new ImageIcon( bi );
        JLabel jLabel = new JLabel();
        jLabel.setIcon(imageIcon);
        editorFrame.getContentPane().add(jLabel, BorderLayout.CENTER);

        editorFrame.pack();
        editorFrame.setLocationRelativeTo(null);
        editorFrame.setVisible(true);
	}

	public static ImagePlus byteToImp( String title, byte[] imageBytes ) throws IOException {

		BufferedImage bi = byteToBufferedImage( imageBytes );
		final ImagePlus imp = new ImagePlus( title, bi);

		return imp;
	}

	public static BufferedImage byteToBufferedImage( byte[] imageBytes ) throws IOException {

		// convert byte array back to BufferedImage
		InputStream in = new ByteArrayInputStream( imageBytes );
		BufferedImage bi = ImageIO.read( in );

		return bi;
	}

	public static short[] byteToShort( byte[] b ) {

		short[] sb = new short[b.length/2];
		// to turn bytes to shorts as either big endian or little endian. 
		ByteBuffer.wrap( b ).order( ByteOrder.LITTLE_ENDIAN ).asShortBuffer().get( sb );
		
		return sb;
	}
	
	public static ImagePlus readImage( String title, File file ) throws IOException {
		byte[] imageBytes = readImageByte( file );
		ImagePlus imp = byteToImp( title, imageBytes );
		
		return imp;
	}

	public static byte[] readImageByte( File file ) {

		try {
			// Image file path
			String id = file.getAbsolutePath();

			// parse command line arguments
			int series = 0;

			// create OME-XML metadata store
			ServiceFactory factory = new ServiceFactory();
			OMEXMLService service = factory.getInstance(OMEXMLService.class);
			IMetadata meta = service.createOMEXMLMetadata();

			// create format reader
			IFormatReader reader = new ImageReader();
			reader.setMetadataStore(meta);

			// initialize file
			System.out.println("Initializing " + id);
			reader.setId(id);

			int seriesCount = reader.getSeriesCount();
			if (series < seriesCount) {
				reader.setSeries(series);
			}
			series = reader.getSeries();
			System.out.println("\tImage series = " + series + " of " + seriesCount);

			printPixelDimensions(reader);
			printPhysicalDimensions(meta, series);

			// read in the first plane
//			ImageReader reader = new ImageReader();
			//read plane #0 from file #0
			//reader.setId(file.getAbsolutePath());
			int plane = 0;
			byte[] imageStream = reader.openBytes(plane);

			System.out.println("\tLength of the byte array = " + Integer.toString( imageStream.length ) );
			System.out.println("\tPrint of the byte array = " + imageStream.toString());
//			final BufferedImageReader br = new BufferedImageReader(reader);
//			br.setId(id);

			// convert byte array back to BufferedImage
			//InputStream in = new ByteArrayInputStream( imageStream );
			//BufferedImage bi = ImageIO.read(in);
			//showBufferedImage( bi );

//			// For now, let's just open the first image.
//			final BufferedImage bi = br.openImage(0);

			// Show it using ImageJ.
			//new ImageJ();
			//final ImagePlus imp = new ImagePlus(id, bi);
			//imp.show();
			return imageStream;

		} catch (DependencyException ex) {
			Logger.getLogger(LibIO.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ServiceException ex) {
			Logger.getLogger(LibIO.class.getName()).log(Level.SEVERE, null, ex);
		} catch (FormatException ex) {
			Logger.getLogger(LibIO.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(LibIO.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		
		return null;
	}

	
	public static void simpleReadImage( File file ) {

		// Image file path
		String id = file.getAbsolutePath();
			
		ImagePlus[] imps = BF.openImagePlus(id);
		for (ImagePlus imp : imps) imp.show();
  }

	
	//try {
	//	ImageReader reader = new ImageReader();
	//	//read plane #0 from file #0
	//	reader.setId(file.getAbsolutePath());
	//	int plane = 0;
	//	byte[] imageStream = reader.openBytes(plane);
	//	ImagePlus img = new ImagePlus();
	//	return img;
	//}
	//catch (FormatException ex

	
	//	) {
	//		Logger.getLogger(LibIO.class.getName()).log(Level.SEVERE, null, ex);
	//}
	//catch (IOException ex
//
	
//		) {
//			Logger.getLogger(LibIO.class.getName()).log(Level.SEVERE, null, ex);
//	}

//return null;
//	}
}
