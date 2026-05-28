package net.imglib2.cellpose;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apposed.appose.BuildException;
import org.apposed.appose.TaskException;

import bdv.util.BdvFunctions;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.LUT;
import net.imglib2.Cursor;
import net.imglib2.FinalDimensions;
import net.imglib2.Interval;
import net.imglib2.algorithm.util.Grids;
import net.imglib2.appose.ShmImg;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.ImgUtil;
import net.imglib2.util.Util;
import net.imglib2.view.fluent.RandomAccessibleIntervalView;

public class DemoTileProcessing
{
	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args ) throws BuildException, IOException, InterruptedException, TaskException
	{
		try
		{

//			ImageJ.main( args );

			// Input image.
			final String filePath = "samples/IDR-1.tif";
			final ImagePlus imp = IJ.openImage( filePath );
//		imp.show();
			final Img< T > img = ImageJFunctions.wrap( imp );
			BdvFunctions.show( img, filePath );
			final AxisInfo axisInfo = AxisInfo.XY;


			// Output image.
			final Img< UnsignedShortType > output = Util.getArrayOrCellImgFactory( img, new UnsignedShortType() ).create( img );

			// Cellpose config.
			final Cellpose3Parameters params = Cellpose3Parameters.defaultNucleiParameters();

			// Tiles.
			final int[] blockSize = new int[] { 512, 512 };
			final FinalDimensions blockDims = new FinalDimensions( blockSize );
			final List< Interval > chunks = Grids.collectAllContainedIntervals( img.dimensionsAsLongArray(), blockSize );

			// Placeholders for tile processing.
			final ShmImg< T > inputShmImg = Cellpose.createInputShmImg( blockDims, img.getType() );
			final ShmImg< UnsignedShortType > outputLabelsShmImg = Cellpose.createOutputLabelsShmImg( blockDims, axisInfo, new UnsignedShortType() );

			// Runner.
//		final ApposeTaskListener listener = ApposeTaskListener.VOID;
			final ApposeTaskListener listener = ApposeTaskListener.STD;

			
			final ImagePlus outputImp = NewImage.createShortImage( "Cellpose output", img.dimension( 0 ), img.dimension( 1 ), 1, NewImage.FILL_BLACK );
			useGlasbeyDarkLUT( outputImp );

			try (final CellposeRunner< T, UnsignedShortType > runner = Cellpose.cellpose3Runner(
					params,
					listener,
					inputShmImg,
					axisInfo,
					outputLabelsShmImg,
					null ))
			{
				// Init runner.
				runner.init();

				// Process tiles.
				int n = 1;
				for ( final Interval tileInterval : chunks )
				{
					if ( n++ > 10 )
						break; // DEBUG

					System.out.println( "\nProcessing tile: " + tileInterval );

					System.out.println( "Copying input tile to shared memory..." );
					final RandomAccessibleIntervalView< T > viewInput = img.view()
							.interval( tileInterval )
							.zeroMin();
					ImgUtil.copy( viewInput, inputShmImg );

					System.out.println( "Running Cellpose..." );
					runner.run();

					System.out.println( "Copying output labels from shared memory..." );
					final RandomAccessibleIntervalView< UnsignedShortType > viewOutput = output.view()
							.interval( tileInterval )
							.zeroMin();

					// Pedestrian copy to the output ImagePlus.
					final Cursor< UnsignedShortType > c = outputLabelsShmImg.localizingCursor();
					while ( c.hasNext() )
					{
						c.fwd();
						final int x = ( int ) ( c.getIntPosition( 0 ) + tileInterval.min( 0 ) );
						final int y = ( int ) ( c.getIntPosition( 1 ) + tileInterval.min( 1 ) );
						final int val = c.get().get();
						outputImp.getProcessor().set( x, y, val );
					}
					System.out.println( "Tile done." );

					outputImp.resetDisplayRange();
					outputImp.show();
				}
				System.out.println( "Done." );
			}
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}

	}

	private static LUT loadLutFromResource( final String resourcePath )
	{
		try (InputStream is = DemoTileProcessing.class.getResourceAsStream( resourcePath );
				BufferedReader reader = new BufferedReader( new InputStreamReader( is ) ))
		{

			if ( is == null )
			{
				IJ.error( "LUT resource not found: " + resourcePath );
				return null;
			}

			final byte[] reds = new byte[ 256 ];
			final byte[] greens = new byte[ 256 ];
			final byte[] blues = new byte[ 256 ];
			String line;
			int index = 0;

			while ( ( line = reader.readLine() ) != null && index < 256 )
			{
				line = line.trim();
				if ( line.isEmpty() )
					continue; // Skip empty lines

				// Split by whitespace
				final String[] parts = line.split( "\\s+" );
				if ( parts.length >= 3 )
				{
					reds[ index ] = ( byte ) Integer.parseInt( parts[ 0 ] );
					greens[ index ] = ( byte ) Integer.parseInt( parts[ 1 ] );
					blues[ index ] = ( byte ) Integer.parseInt( parts[ 2 ] );
					index++;
				}
			}

			if ( index != 256 )
			{
				IJ.error( "Invalid LUT file: expected 256 entries, found " + index );
				return null;
			}

			return new LUT( reds, greens, blues );
		}
		catch ( final IOException e )
		{
			IJ.error( "Failed to load LUT: " + e.getMessage() );
			return null;
		}
	}

	public static final void useGlasbeyDarkLUT( final ImagePlus imp )
	{
		final LUT lut = loadLutFromResource( "/glasbey_on_dark.lut" );
		useLUT( imp, lut );
	}

	public static final void useLUT( final ImagePlus imp, final LUT lut )
	{
		imp.setLut( lut );
		imp.updateAndDraw();
	}

}
