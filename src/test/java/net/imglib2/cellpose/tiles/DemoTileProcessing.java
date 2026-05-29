package net.imglib2.cellpose.tiles;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apposed.appose.BuildException;
import org.apposed.appose.TaskException;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.RGBStackMerge;
import ij.process.ImageProcessor;
import ij.process.LUT;
import net.imglib2.Cursor;
import net.imglib2.FinalDimensions;
import net.imglib2.Interval;
import net.imglib2.appose.ShmImg;
import net.imglib2.cellpose.ApposeTaskListener;
import net.imglib2.cellpose.AxisInfo;
import net.imglib2.cellpose.Cellpose;
import net.imglib2.cellpose.Cellpose3BuiltinModels;
import net.imglib2.cellpose.Cellpose3Parameters;
import net.imglib2.cellpose.CellposeRunner;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.ImgUtil;
import net.imglib2.view.fluent.RandomAccessibleIntervalView;

public class DemoTileProcessing
{
	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args ) throws BuildException, IOException, InterruptedException, TaskException
	{
		try
		{
			ImageJ.main( args );

			// Input image.
			final String filePath = "samples/IDR-1.tif";
			final ImagePlus imp = IJ.openImage( filePath );
			final Img< T > img = ImageJFunctions.wrap( imp );

			// Output ImagePlus.
			final ImagePlus outputImp = NewImage.createShortImage( "Cellpose output", ( int ) img.dimension( 0 ), ( int ) img.dimension( 1 ), 1, NewImage.FILL_BLACK );

			// Merge channels and set LUT.
			final ImagePlus merged = RGBStackMerge.mergeChannels( new ImagePlus[] { imp, outputImp }, false );
			merged.setTitle( "Cellpose Tile Processing Demo" );
			merged.show();
			merged.setC( 1 );
			useLUT( merged.getChannelProcessor(), LUT.createLutFromColor( Color.WHITE ) );
			merged.setC( 2 );
			useGlasbeyDarkLUT( merged.getChannelProcessor() );

			// Cellpose config.
			final Cellpose3Parameters params = Cellpose3Parameters.builder()
					.model( Cellpose3BuiltinModels.NUCLEI )
					.diameter( 60.0 )
					.resample( true )
					.build();

			// Tiles.
			final int[] blockSize = new int[] { 512, 512 };
			final FinalDimensions blockDims = new FinalDimensions( blockSize );
			final List< Interval > chunks = Grids2.collectAllContainedIntervals( img.dimensionsAsLongArray(), blockSize );

			// Process.
			final long start = System.currentTimeMillis();
			final AxisInfo axisInfo = AxisInfo.XY;
			// Try-with-resources with auto-closeable ShmImgs and CellposeRunner.
			try (
					// Placeholders for tile processing.
					final ShmImg< T > cellposeInputData = Cellpose.createInputShmImg( blockDims, img.getType() );
					final ShmImg< UnsignedShortType > cellposeOutputData = Cellpose.createOutputLabelsShmImg( blockDims, axisInfo, new UnsignedShortType() );
					// The runner.
					final CellposeRunner< T, UnsignedShortType > runner = Cellpose.cellpose3Runner(
							params,
							ApposeTaskListener.VOID,
							cellposeInputData,
							axisInfo,
							cellposeOutputData,
							null );)
			{
				// Init runner.
				runner.init();

				// Process tiles.
				for ( final Interval tileInterval : chunks )
				{
					// Input tile -> Cellpose input data location.
					copyInput( img, cellposeInputData, tileInterval );

					// Run Cellpose.
					runner.run();

					// Cellpose output tile -> output ImagePlus.
					copyOutput( cellposeOutputData, merged, tileInterval );
					merged.resetDisplayRange();
					merged.updateAndDraw();
				}
			}
			finally
			{
				final long end = System.currentTimeMillis();
				System.out.println( String.format( "Done in: %.2f seconds", ( end - start ) / 1000. ) );
			}
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}

	/**
	 * Copy the Cellpose output labels contained in the tile interval to the
	 * output ImagePlus. The Cellpose output ShmImg is supposed to be at origin
	 * (0, 0) and of size equal to the tile size.
	 * 
	 * @param output
	 *            the Cellpose output ShmImg containing the labels for the tile
	 * @param target
	 *            the output ImagePlus where to copy the labels
	 * @param interval
	 *            the tile interval defining the location of the tile in the
	 *            output ImagePlus
	 */
	private static void copyOutput( final ShmImg< UnsignedShortType > output, final ImagePlus target, final Interval interval )
	{
		// Basic copy to the output ImagePlus.
		final Cursor< UnsignedShortType > c = output.localizingCursor();
		while ( c.hasNext() )
		{
			c.fwd();
			final int x = ( int ) ( c.getIntPosition( 0 ) + interval.min( 0 ) );
			if ( x >= target.getWidth() )
				continue;
			final int y = ( int ) ( c.getIntPosition( 1 ) + interval.min( 1 ) );
			if ( y >= target.getHeight() )
				continue;
			final int val = c.get().get();
			target.getProcessor().set( x, y, val );
		}
	}

	/**
	 * Copy the data of the input image contained in the tile interval to the
	 * Cellpose input ShmImg. The Cellpose image is supposed to be at origin (0,
	 * 0) and of size equal to the tile size. The input image must be defined
	 * over all the tile interval.
	 * 
	 * @param <T>
	 *            the pixel type
	 * @param input
	 *            the input image
	 * @param target
	 *            the Cellpose input ShmImg
	 * @param interval
	 *            the tile interval
	 */
	private static < T extends RealType< T > & NativeType< T > > void copyInput( final Img< T > input, final ShmImg< T > target, final Interval interval )
	{
		final RandomAccessibleIntervalView< T > viewInput = input.view()
				.interval( interval )
				.zeroMin();
		final RandomAccessibleIntervalView< T > viewInputShmImg = target.view()
				.translate( interval.minAsLongArray() )
				.interval( interval )
				.zeroMin();
		ImgUtil.copy( viewInput, viewInputShmImg );
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

	private static final void useGlasbeyDarkLUT( final ImageProcessor ip )
	{
		final LUT lut = loadLutFromResource( "/glasbey_on_dark.lut" );
		useLUT( ip, lut );
	}

	private static final void useLUT( final ImageProcessor ip, final LUT lut )
	{
		ip.setLut( lut );
	}

}
