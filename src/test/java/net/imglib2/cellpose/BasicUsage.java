package net.imglib2.cellpose;

import java.io.IOException;

import org.apposed.appose.BuildException;
import org.apposed.appose.TaskException;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class BasicUsage
{

	public static void main( final String[] args ) throws BuildException, IOException, InterruptedException, TaskException
	{
		basicUsage( args );
//		outputType( args );
	}

	public static < T extends RealType< T > & NativeType< T > > void outputType( final String[] args ) throws BuildException, IOException, InterruptedException, TaskException
	{
		final ImagePlus imp = IJ.openImage( "http://imagej.net/images/blobs.gif" );
		final Img< T > img = ImageJFunctions.wrap( imp );

		final RandomAccessibleInterval< T > input = img;
		final AxisInfo inputAxes = AxisInfo.XY;
		final ApposeTaskListener listener = ApposeTaskListener.STD;
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.channels( 1, 0 )
				.computeFlows( true )
				.build();

		final CellposeOutput< UnsignedIntType > output = Cellpose.cellpose3(
				input,
				inputAxes,
				new UnsignedIntType(),
				params,
				listener );

		@SuppressWarnings( "unused" )
		final RandomAccessibleInterval< UnsignedIntType > labels = output.labels;
		@SuppressWarnings( "unused" )
		final RandomAccessibleInterval< UnsignedByteType > flows = output.flows;
	}

	public static < T extends RealType< T > & NativeType< T > > void basicUsage( final String[] args ) throws BuildException, IOException, InterruptedException, TaskException
	{
		// Demo preparation. We use IJ for this one.
		ImageJ.main( args );
		final ImagePlus imp = IJ.openImage( "http://imagej.net/images/blobs.gif" );
		imp.show();
		final Img< T > img = ImageJFunctions.wrap( imp );

		// Input
		final RandomAccessibleInterval< T > input = img;
		// You need to specify the dimensionality of your input
		final AxisInfo inputAxes = AxisInfo.XY;

		// Get messages about installing and processing
		final ApposeTaskListener listener = ApposeTaskListener.STD;

		// Specify the parameters for Cellpose 3
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
		    .model( Cellpose3BuiltinModels.CYTO2 )
		    .channels( 1, 0 )
		    .computeFlows( true )
		    .build();

		final CellposeOutput< UnsignedShortType > output = Cellpose.cellpose3( input, inputAxes, params, listener );

		final RandomAccessibleInterval< UnsignedShortType > labels = output.labels;
		final RandomAccessibleInterval< UnsignedByteType > flows = output.flows;

		ImageJFunctions.show( labels ).setTitle( "Cellpose output" );
		ImageJFunctions.show( flows ).setTitle( "Cellpose flows" );
	}
}
