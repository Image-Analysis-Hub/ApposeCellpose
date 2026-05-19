package org.apposed.appose.cellpose;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;

import org.apposed.appose.BuildException;
import org.apposed.appose.TaskException;
import org.junit.Test;

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/**
 * JUnit tests that check that the Cellpose functions can properly harness all
 * dimensionality cases.
 */
public class CellposeDimensionalitiesTest
{

	@Test
	void testCellpose3_XY()
	{
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.channels( 1, 0 )
				.computeFlows( true )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XY );
	}

	@Test
	void testCellpose3_XYC()
	{
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYC );
	}

	@Test
	void testCellpose3_XYT()
	{
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYT );
	}

	@Test
	void testCellpose3_XYCT()
	{
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYCT );
	}

	@Test
	void testCellpose3_XYZ()
	{
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.stitchThreshold( 0.4 )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYZ );
	}

	@Test
	void testCellpose3_XYZT()
	{
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.stitchThreshold( 0.4 )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYZT );
	}

	void testCellpose3_XYCZ()
	{
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.stitchThreshold( 0.4 )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYCZ );
	}

	void testCellpose3_XYCZT()
	{
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.stitchThreshold( 0.4 )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYCZT );
	}

	@Test
	void testCellpose3_XYZ_NoStich()
	{
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.stitchThreshold( 0. )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYZ );
	}

	@Test
	void testCellpose3_XYZT_NoStich()
	{
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.stitchThreshold( 0. )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYZT );
	}

	@Test
	void testCellpose3_XYZT_NoStich_Mode3D()
	{
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.stitchThreshold( 0. )
				.do3D( true )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYZT );
	}

	@Test
	void testCellpose3_XYCZ_NoStich()
	{
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.stitchThreshold( 0. )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYCZ );
	}

	@Test
	void testCellpose3_XYCZT_NoStich()
	{
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.stitchThreshold( 0. )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYCZT );
	}

	@Test
	void testCellpose4_XY()
	{
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XY );
	}

	@Test
	void testCellpose4_XYC()
	{
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYC );
	}

	@Test
	void testCellpose4_XYT()
	{
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYT );
	}

	@Test
	void testCellpose4_XYCT()
	{
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYCT );
	}

	@Test
	void testCellpose4_XYZ()
	{
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0.4 )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYZ );
	}

	@Test
	void testCellpose4_XYZT()
	{
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0.4 )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYZT );
	}

	@Test
	void testCellpose4_XYCZ()
	{
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0.4 )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYCZ );
	}

	@Test
	void testCellpose4_XYCZT()
	{
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0.4 )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYCZT );
	}

	@Test
	void testCellpose4_XYZ_NoStich()
	{
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0. )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYZ );
	}

	@Test
	void testCellpose4_XYZT_NoStich_Mode3D()
	{
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0. )
				.do3D( true )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYZT );
	}

	@Test
	void testCellpose4_XYZT_NoStich()
	{
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0. )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYZT );
	}

	@Test
	void testCellpose4_XYCZ_NoStich()
	{
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0. )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYCZ );
	}
	
	@Test
	void testCellpose3_XYCZ_NoStich_Mode3D()
	{
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.stitchThreshold( 0. )
				.do3D( true )
				.computeFlows( true )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYCZ );
	}
	
	@Test
	void testCellpose3_XYCZT_NoStich_Mode3D()
	{
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.stitchThreshold( 0. )
				.do3D( true )
				.computeFlows( true )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYCZT );
	}

	@Test
	void testCellpose4_XYCZ_NoStich_Mode3D()
	{
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0. )
				.do3D( true )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYCZ );
	}

	@Test
	void testCellpose4_XYCZT_NoStich_Mode3D()
	{
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0. )
				.do3D( true )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYCZT );
	}

	@Test
	void testCellpose4_XYCZT_NoStich()
	{
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0. )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYCZT );
	}

	private static final BiFunction< RandomAccessibleInterval< UnsignedByteType >, AxisInfo, CellposeOutput< UnsignedShortType > > cellpose3Runner( final Cellpose3Parameters params )
	{
		return ( img, axes ) -> {
			try
			{
				return Cellpose.cellpose3( img, axes, params, ApposeTaskListener.STD );
			}
			catch ( BuildException | IOException | InterruptedException | TaskException e )
			{
				e.printStackTrace();
			}
			return null;
		};
	}

	private static final BiFunction< RandomAccessibleInterval< UnsignedByteType >, AxisInfo, CellposeOutput< UnsignedShortType > > cellpose4Runner( final Cellpose4Parameters params )
	{
		return ( img, axes ) -> {
			try
			{
				return Cellpose.cellpose4( img, axes, params, ApposeTaskListener.STD );
			}
			catch ( BuildException | IOException | InterruptedException | TaskException e )
			{
				e.printStackTrace();
			}
			return null;
		};
	}

	private void test( final BiFunction< RandomAccessibleInterval< UnsignedByteType >, AxisInfo, CellposeOutput< UnsignedShortType > > runner, final CellposeTestDims dims )
	{
		final RandomAccessibleInterval< UnsignedByteType > input = createTestImgForDims( dims );
		final AxisInfo inputAxes = dims.axes;
		final List< Integer > inputAxesList = List.of( inputAxes.X(), inputAxes.Y(), inputAxes.C(), inputAxes.Z(), inputAxes.T() );
		final CellposeOutput< UnsignedShortType > outputs = runner.apply( input, dims.axes );

		/*
		 * Labels.
		 */
		final RandomAccessibleInterval< UnsignedShortType > labels = outputs.labels;
		final AxisInfo outputAxes = AxisInfo.XYCZT;
		final List< Integer > outputAxesList = List.of( outputAxes.X(), outputAxes.Y(), outputAxes.C(), outputAxes.Z(), outputAxes.T() );

		// Test output dimensions
		final long[] inDims = input.dimensionsAsLongArray();
		final long[] labelsDims = labels.dimensionsAsLongArray();
		final String str = "XYCZT";
		for ( int d = 0; d < 5; d++ )
		{
			final char dimName = str.charAt( d );
			final int currentInputAxes = inputAxesList.get( d );
			final int currentOutputAxes = outputAxesList.get( d );

			if ( dimName == 'C' )
				assertEquals( "For case " + dims.name() + ": Label output should have only one channel.",
						1, labelsDims[ currentOutputAxes ] );
			else
				assertEquals( "For case " + dims.name() + ": Label output and input must have the same " + dimName + " size.",
						inDims[ currentInputAxes ], labelsDims[ currentOutputAxes ] );
		}

		/*
		 * Flows/
		 */

		final RandomAccessibleInterval< UnsignedByteType > flows = outputs.flows;
		// Test output dimensions
		final long[] outFlowDims = flows.dimensionsAsLongArray();
		for ( int d = 0; d < 5; d++ )
		{
			final char dimName = str.charAt( d );
			final int currentInputAxes = inputAxesList.get( d );
			final int currentOutputAxes = outputAxesList.get( d );

			if ( dimName == 'C' )
				// C must always be there, and of dim 3.
				assertEquals( "For case " + dims.name() + ": Flow output must have 3 channels.",
						3, outFlowDims[ currentOutputAxes ] );
			else
				assertEquals( "For case " + dims.name() + ": Flow output and input must have the same " + dimName + " size.",
						inDims[ currentInputAxes ], outFlowDims[ currentOutputAxes ] );
		}
	}

	private static final long XY_SIZE = 128;

	private static final long Z_SIZE = 16;

	private static final long C_SIZE = 2;

	private static final long T_SIZE = 5;

	private static enum CellposeTestDims
	{
		XY( new long[] { XY_SIZE, XY_SIZE }, AxisInfo.XY ),
		XYC( new long[] { XY_SIZE, XY_SIZE, C_SIZE }, AxisInfo.XYC ),

		XYT( new long[] { XY_SIZE, XY_SIZE, T_SIZE }, AxisInfo.XYT ),
		XYCT( new long[] { XY_SIZE, XY_SIZE, C_SIZE, T_SIZE }, AxisInfo.XYCT ),

		XYZ( new long[] { XY_SIZE, XY_SIZE, Z_SIZE }, AxisInfo.XYZ ),
		XYCZ( new long[] { XY_SIZE, XY_SIZE, C_SIZE, Z_SIZE }, AxisInfo.XYCZ ),

		XYZT( new long[] { XY_SIZE, XY_SIZE, Z_SIZE, T_SIZE }, AxisInfo.XYZT ),
		XYCZT( new long[] { XY_SIZE, XY_SIZE, C_SIZE, Z_SIZE, T_SIZE }, AxisInfo.XYCZT ),
		;

		private final long[] dims;

		private final AxisInfo axes;

		CellposeTestDims( final long[] dims, final AxisInfo axes )
		{
			this.dims = dims;
			this.axes = axes;
		}
	}

	public static RandomAccessibleInterval< UnsignedByteType > createTestImgForDims( final CellposeTestDims dims )
	{
		final Img< UnsignedByteType > img = ArrayImgs.unsignedBytes( dims.dims );
		// Write a circle at every timepoint.
		writeBlob( img, dims.axes );
		return img;
	}

	private static void writeBlob( final RandomAccessibleInterval< UnsignedByteType > img, final AxisInfo axes )
	{
		RandomAccessibleInterval< UnsignedByteType > view = img;
		if ( axes.T() >= 0 )
		{
			for ( long t = 0; t < img.dimension( axes.T() ); t++ )
			{

				view = Views.hyperSlice( img, axes.T(), t );
				processTimepoint( view, axes, ( int ) t );
			}
		}
		else
		{
			processTimepoint( img, axes, 0 );
		}
	}

	private static void processTimepoint( final RandomAccessibleInterval< UnsignedByteType > img, final AxisInfo axes, final int tp )
	{
		if ( axes.C() < 0 )
		{
			processChannel( img, axes, tp );
		}
		else
		{
			// Write only in channel 0
			final RandomAccessibleInterval< UnsignedByteType > view = Views.hyperSlice( img, axes.C(), 0 );
			processChannel( view, axes, tp );
		}
	}

	private static void processChannel( final RandomAccessibleInterval< UnsignedByteType > imgPlus, final AxisInfo axes, final int tp )
	{
		if ( axes.Z() < 0 )
		{
			writeCircle( imgPlus, tp );
		}
		else
		{
			// Write circle at the middle +/- 3
			for ( long z = Z_SIZE / 2 - 3; z <= Z_SIZE / 2 + 3; z++ )
			{
				final RandomAccessibleInterval< UnsignedByteType > view = Views.hyperSlice( imgPlus, axes.Z(), z );
				writeCircle( view, tp );
			}
		}
	}

	private static void writeCircle( final RandomAccessibleInterval< UnsignedByteType > img, final int tp )
	{
		assert img.numDimensions() == 2;

		final int radius = 30;
		// Slight shift with time.
		final int[] center = new int[] { ( int ) ( XY_SIZE / 2. + 5. * tp ), ( int ) ( XY_SIZE / 2. ) };
		final FinalInterval blob = Intervals.createMinMax( center[ 0 ] - radius, center[ 1 ] - radius, center[ 0 ] + radius, center[ 1 ] + radius );
		Views.interval( img, blob ).forEach( p -> p.set( 200 ) );
	}

	// Used for debugging only.
	public static void main( final String[] args )
	{
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.build();

		try
		{
			final CellposeTestDims[] toTest = new CellposeTestDims[] { CellposeTestDims.XY };
//			final CellposeTestDims[] toTest = CellposeTestDims.values();
			for ( final CellposeTestDims dims : toTest )
			{
				final RandomAccessibleInterval< UnsignedByteType > img = createTestImgForDims( dims );
				System.out.println( '\n' + dims.axes.toString() );
				System.out.println( "Testing case " + dims.name() );

				final CellposeOutput< ? > output = Cellpose.cellpose3( img, dims.axes, params, ApposeTaskListener.STD );

				System.out.println( "\nInput shape:           " + Util.printInterval( img ) );
				System.out.println( "Get Labels with shape: " + Util.printInterval( output.labels ) );
				System.out.println( " and flows with shape: " + Util.printInterval( output.flows ) );
			}
			System.out.println( "\nDone." );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
