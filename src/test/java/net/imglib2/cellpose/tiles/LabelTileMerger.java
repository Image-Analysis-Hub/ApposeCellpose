package net.imglib2.cellpose.tiles;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * Utility for stitching overlapping label-image tiles into a single canvas.
 *
 * <p>
 * Each input tile contains local instance labels, with {@code 0} representing
 * background and positive values representing object ids. Tiles are written
 * into the provided output canvas according to the corresponding world-space
 * intervals. If labeled objects overlap across tile boundaries, they are merged
 * using an overlap-based IoU criterion.
 * <p>
 * The output canvas is modified in place.
 * </p>
 */
public class LabelTileMerger
{
	private LabelTileMerger()
	{
		// utility class
	}

	/**
	 * Merges label tiles into a single canvas, using overlap-based IoU to unify
	 * objects crossing tile boundaries.
	 *
	 * @param tiles
	 *            list of label tiles
	 * @param intervals
	 *            world-space interval of each tile, same order as {@code tiles}
	 * @param iouThresh
	 *            IoU threshold in [0, 1]
	 * @param canvas
	 *            output canvas, initialized to 0 and large enough to contain
	 *            all intervals
	 */
	public static < T extends IntegerType< T >, R extends IntegerType< R > > void mergeTilesIntoCanvas(
			final List< RandomAccessibleInterval< T > > tiles,
			final List< Interval > intervals,
			final double iouThresh,
			final RandomAccessibleInterval< R > canvas )
	{
		validateInputs( tiles, intervals, iouThresh, canvas );

		final RandomAccess< R > canvasRA = canvas.randomAccess( canvas );
		final UnionFind uf = new UnionFind();

		int offset = 0;

		for ( int t = 0; t < tiles.size(); t++ )
		{
			final RandomAccessibleInterval< T > tile = tiles.get( t );
			final Interval interval = intervals.get( t );

			final int tileMax = maxLabel( tile );
			if ( tileMax == 0 )
				continue;

			for ( int lbl = 1; lbl <= tileMax; lbl++ )
				uf.add( lbl + offset );

			final RandomAccessibleInterval< T > tileInWorld = translateToInterval( tile, interval );

			final Map< Integer, Integer > cntOld = new HashMap<>();
			final Map< Integer, Integer > cntNew = new HashMap<>();
			final Map< Long, Integer > cntCo = new HashMap<>();

			final Cursor< T > tileCursor = tileInWorld.localizingCursor();

			while ( tileCursor.hasNext() )
			{
				tileCursor.fwd();

				final int tileLabel = tileCursor.get().getInteger();
				if ( tileLabel == 0 )
					continue;

				final int globalNew = tileLabel + offset;

				canvasRA.setPosition( tileCursor );
				final int canvasVal = canvasRA.get().getInteger();

				if ( canvasVal > 0 )
				{
					cntOld.merge( canvasVal, 1, Integer::sum );
					cntNew.merge( globalNew, 1, Integer::sum );
					cntCo.merge( packPair( canvasVal, globalNew ), 1, Integer::sum );
				}
				else
				{
					canvasRA.get().setInteger( globalNew );
				}
			}

			for ( final Map.Entry< Long, Integer > e : cntCo.entrySet() )
			{
				final int oldLbl = unpackHigh( e.getKey() );
				final int newLbl = unpackLow( e.getKey() );
				final int intersection = e.getValue();
				final int union = cntOld.get( oldLbl ) + cntNew.get( newLbl ) - intersection;

				if ( union > 0 && ( double ) intersection / union >= iouThresh )
					uf.union( oldLbl, newLbl );
			}

			offset += tileMax;
		}

		relabel( canvas, uf );
	}

	private static < T > RandomAccessibleInterval< T > translateToInterval(
			final RandomAccessibleInterval< T > tile,
			final Interval interval )
	{
		final int n = tile.numDimensions();
		final long[] shift = new long[ n ];
		for ( int d = 0; d < n; d++ )
			shift[ d ] = interval.min( d ) - tile.min( d );
		return Views.translate( tile, shift );
	}

	private static < T extends IntegerType< T >, R extends IntegerType< R > > void validateInputs(
			final List< RandomAccessibleInterval< T > > tiles,
			final List< Interval > intervals,
			final double iouThresh,
			final RandomAccessibleInterval< R > canvas )
	{
		if ( tiles == null || intervals == null || canvas == null )
			throw new IllegalArgumentException( "tiles, intervals, and canvas must be non-null" );

		if ( tiles.size() != intervals.size() )
			throw new IllegalArgumentException( "tiles and intervals must have the same size" );

		if ( iouThresh < 0.0 || iouThresh > 1.0 )
			throw new IllegalArgumentException( "iouThresh must be in [0, 1]" );

		for ( int t = 0; t < tiles.size(); t++ )
		{
			final RandomAccessibleInterval< T > tile = tiles.get( t );
			final Interval interval = intervals.get( t );

			if ( tile.numDimensions() != interval.numDimensions() )
				throw new IllegalArgumentException( "Tile and interval dimension mismatch at index " + t );

			if ( tile.numDimensions() != canvas.numDimensions() )
				throw new IllegalArgumentException( "Tile and canvas dimension mismatch at index " + t );

			if ( !Intervals.equalDimensions( ( Dimensions ) tile, ( Dimensions ) interval ) )
				throw new IllegalArgumentException( "Tile and interval size mismatch at index " + t );

			for ( int d = 0; d < canvas.numDimensions(); d++ )
			{
				if ( interval.min( d ) < canvas.min( d ) || interval.max( d ) > canvas.max( d ) )
					throw new IllegalArgumentException( "Interval at index " + t + " lies outside canvas" );
			}
		}
	}

	private static < R extends IntegerType< R > > void relabel(
			final RandomAccessibleInterval< R > canvas,
			final UnionFind uf )
	{
		final Set< Integer > roots = new LinkedHashSet<>();
		final Cursor< R > scan = canvas.cursor();
		while ( scan.hasNext() )
		{
			final int v = scan.next().getInteger();
			if ( v > 0 )
				roots.add( uf.find( v ) );
		}

		final Map< Integer, Integer > rootToCompact = new HashMap<>();
		int id = 1;
		for ( final int r : roots )
			rootToCompact.put( r, id++ );

		final Cursor< R > write = canvas.cursor();
		while ( write.hasNext() )
		{
			final R px = write.next();
			final int v = px.getInteger();
			if ( v > 0 )
				px.setInteger( rootToCompact.get( uf.find( v ) ) );
		}
	}

	private static < T extends IntegerType< T > > int maxLabel( final RandomAccessibleInterval< T > tile )
	{
		int max = 0;
		final Cursor< T > c = tile.cursor();
		while ( c.hasNext() )
			max = Math.max( max, c.next().getInteger() );
		return max;
	}

	private static long packPair( int a, int b )
	{
		if ( a > b )
		{
			final int tmp = a;
			a = b;
			b = tmp;
		}
		return ( ( long ) a << 32 ) | ( b & 0xFFFFFFFFL );
	}

	private static int unpackHigh( final long key )
	{
		return ( int ) ( key >>> 32 );
	}

	private static int unpackLow( final long key )
	{
		return ( int ) key;
	}

	private static class UnionFind
	{
		private final Map< Integer, Integer > parent = new HashMap<>();

		private final Map< Integer, Integer > rank = new HashMap<>();

		void add( final int x )
		{
			parent.putIfAbsent( x, x );
			rank.putIfAbsent( x, 0 );
		}

		int find( int x )
		{
			add( x );
			while ( parent.get( x ) != x )
			{
				final int grandparent = parent.get( parent.get( x ) );
				parent.put( x, grandparent );
				x = grandparent;
			}
			return x;
		}

		void union( final int a, final int b )
		{
			int ra = find( a );
			int rb = find( b );
			if ( ra == rb )
				return;

			final int rankA = rank.get( ra );
			final int rankB = rank.get( rb );

			if ( rankA < rankB )
			{
				final int tmp = ra;
				ra = rb;
				rb = tmp;
			}

			parent.put( rb, ra );
			if ( rankA == rankB )
				rank.put( ra, rankA + 1 );
		}
	}
}
