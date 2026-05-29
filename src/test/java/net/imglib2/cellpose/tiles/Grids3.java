package net.imglib2.cellpose.tiles;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

public class Grids3
{

	/**
	 * Splits a larger interval into a list of sub-intervals ("blocks") whose
	 * size is at most {@code blockSize} in each dimension, with a given pixel
	 * overlap.
	 * <p>
	 * Example: if dimension size = 100, blockSize = 32, overlap = 8, then the
	 * step is 24, and blocks are generated so they cover the whole interval,
	 * with consecutive blocks overlapping by 8 pixels.
	 * <p>
	 * The last block in each dimension is clamped to the interval max, so block
	 * sizes may be smaller than {@code blockSize} at the boundaries.
	 *
	 * @param interval
	 *            the interval to cover.
	 * @param blockSize
	 *            maximum block size per dimension.
	 * @param overlap
	 *            overlap in pixels per dimension.
	 * @return list of intervals covering the input interval.
	 */
	public static List< Interval > padWithOverlap(
			final Interval interval,
			final long blockSize,
			final long overlap )
	{
		final int n = interval.numDimensions();
		final long[] blockSizes = new long[ n ];
		final long[] overlaps = new long[ n ];

		for ( int d = 0; d < n; d++ )
		{
			blockSizes[ d ] = blockSize;
			overlaps[ d ] = overlap;
		}
		return padWithOverlap( interval, blockSizes, overlaps );
	}

	/**
	 * Splits a larger interval into a list of sub-intervals ("blocks") whose
	 * size is at most {@code blockSize[d]} in each dimension, with a given
	 * pixel overlap {@code overlap[d]} in each dimension.
	 * 
	 * @param interval
	 *            the interval to cover.
	 * @param blockSize
	 *            maximum block size per dimension.
	 * @param overlap
	 *            overlap in pixels per dimension.
	 * @return list of intervals covering the input interval.
	 */
	public static List< Interval > padWithOverlap(
			final Interval interval,
			final long[] blockSize,
			final long[] overlap )
	{
		final int n = interval.numDimensions();

		if ( blockSize.length != n || overlap.length != n )
			throw new IllegalArgumentException( "blockSize and overlap must match interval dimensionality" );

		final List< long[] > startsPerDim = new ArrayList<>( n );

		for ( int d = 0; d < n; d++ )
		{
			if ( blockSize[ d ] <= 0 )
				throw new IllegalArgumentException( "blockSize must be > 0 in every dimension" );
			if ( overlap[ d ] < 0 )
				throw new IllegalArgumentException( "overlap must be >= 0 in every dimension" );
			if ( overlap[ d ] >= blockSize[ d ] )
				throw new IllegalArgumentException( "overlap must be smaller than blockSize in every dimension" );

			startsPerDim.add( computeStarts(
					interval.min( d ),
					interval.max( d ),
					blockSize[ d ],
					overlap[ d ] ) );
		}

		final List< Interval > result = new ArrayList<>();
		final long[] currentMin = new long[ n ];
		buildIntervalsRecursive( interval, blockSize, startsPerDim, 0, currentMin, result );
		return result;
	}

	private static long[] computeStarts(
			final long min,
			final long max,
			final long blockSize,
			final long overlap )
	{
		final long step = blockSize - overlap;
		final List< Long > starts = new ArrayList<>();

		long pos = min;
		while ( pos <= max )
		{
			starts.add( pos );
			pos += step;
		}

		final long[] out = new long[ starts.size() ];
		for ( int i = 0; i < starts.size(); i++ )
			out[ i ] = starts.get( i );

		return out;
	}

	private static void buildIntervalsRecursive(
			final Interval interval,
			final long[] blockSize,
			final List< long[] > startsPerDim,
			final int dim,
			final long[] currentMin,
			final List< Interval > result )
	{
		final int n = interval.numDimensions();

		if ( dim == n )
		{
			final long[] min = currentMin.clone();
			final long[] max = new long[ n ];

			for ( int d = 0; d < n; d++ )
				max[ d ] = Math.min( min[ d ] + blockSize[ d ] - 1, interval.max( d ) );

			result.add( new FinalInterval( min, max ) );
			return;
		}

		for ( final long start : startsPerDim.get( dim ) )
		{
			currentMin[ dim ] = start;
			buildIntervalsRecursive( interval, blockSize, startsPerDim, dim + 1, currentMin, result );
		}
	}

	public static void main( final String[] args )
	{
		final Interval big = Intervals.createMinSize( 0, 0, 1024, 1024 );
		final long blockSize = 512;
		final long overlap = 10;

		final List< Interval > blocks = padWithOverlap( big, blockSize, overlap );

		System.out.println( "To pad : " + Util.printInterval( big ) );
		for ( final Interval block : blocks )
			System.out.println( Util.printInterval( block ) );
	}
}
