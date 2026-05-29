package net.imglib2.cellpose.tiles;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.view.Views;

/**
 * Merges instance-segmentation label tiles defined by ImgLib2 Intervals.
 *
 * Assumptions: - Label type is IntType (0 = background, >0 = instance id). -
 * Tiles may overlap; objects crossing tile boundaries are re-unified. - Output
 * canvas covers the bounding box of all intervals.
 */
public class LabelTileMerger
{

	/**
	 * Merges label tiles into a single canvas, using IoU-based union for
	 * objects crossing tile boundaries.
	 * 
	 * @param <T>
	 *            input tile pixel type (must be an IntegerType).
	 * @param <R>
	 *            canvas pixel type (must be an IntegerType).
	 * @param tiles
	 *            list of label tiles (RandomAccessibleIntervals) at origin (0,
	 *            0).
	 * @param intervals
	 *            list of tile intervals defining the location of each tile in
	 *            the canvas. Must be the same size as {@code tiles}, and
	 *            intervals must be consistent with tile sizes.
	 * @param iouThresh
	 *            IoU threshold for merging objects across tile boundaries. If
	 *            the intersection-over-union of two objects (one in canvas, one
	 *            in tile) is above this threshold, they are merged into one
	 *            object in the output. Set to 0 to merge all touching objects,
	 *            or to 1 to merge only perfectly overlapping objects.
	 * @param canvas
	 *            the output canvas where to merge the tiles. It is the caller's
	 *            responsibility to ensure that the canvas covers the bounding
	 *            box of all intervals, and that it is initialized to 0
	 *            (background) before calling this method.
	 */
	public static < T extends IntegerType< T >, R extends IntegerType< R > > void mergeTilesIntoCanvas(
			final List< RandomAccessibleInterval< T > > tiles,
			final List< Interval > intervals,
			final double iouThresh,
			final RandomAccessibleInterval< R > canvas )
	{

		// Random access into the output canvas.
		final RandomAccess< R > canvasRA = canvas.randomAccess( canvas );

		// The union-finf for labels.
		final UnionFind uf = new UnionFind();
		int offset = 0;

		for ( int t = 0; t < tiles.size(); t++ )
		{
			final RandomAccessibleInterval< T > tile = tiles.get( t );
			final Interval interval = intervals.get( t );

			final int tileMax = maxLabel( tile );
			if ( tileMax == 0 )
				continue; // Tile has no objects.

			// Register all global labels in union-find
			for ( int lbl = 1; lbl <= tileMax; lbl++ )
				uf.add( lbl + offset );

			// Translate tile cursor into world (canvas) coordinates
			final RandomAccessibleInterval< T > tileInWorld = Views.translate( tile, interval.minAsLongArray() );

			// Co-occurrence counts for IoU (within overlap zone only)
			final Map< Long, Integer > cntOld = new HashMap<>();
			final Map< Long, Integer > cntNew = new HashMap<>();
			final Map< Long, Integer > cntCo = new HashMap<>();

			final Cursor< T > tileCursor = tileInWorld.localizingCursor();

			while ( tileCursor.hasNext() )
			{
				tileCursor.fwd();
				final int tileLabel = tileCursor.get().getInteger();
				if ( tileLabel == 0 )
					continue;

				final int globalNew = tileLabel + offset;

				// Move canvas RA to the same world position
				canvasRA.setPosition( tileCursor );
				final int canvasVal = canvasRA.get().getInteger();

				if ( canvasVal > 0 )
				{
					// Overlap zone: accumulate co-occurrence statistics
					final long key = packPair( canvasVal, globalNew );
					cntOld.merge( ( long ) canvasVal, 1, Integer::sum );
					cntNew.merge( ( long ) globalNew, 1, Integer::sum );
					cntCo.merge( key, 1, Integer::sum );
				}
				else
				{
					// Empty canvas pixel: write global label directly
					canvasRA.get().setInteger( globalNew );
				}
			}

			// IoU-based union for overlapping label pairs
			for ( final Map.Entry< Long, Integer > e : cntCo.entrySet() )
			{
				final long key = e.getKey();
				final int oldLbl = unpackHigh( key );
				final int newLbl = unpackLow( key );
				final int intersection = e.getValue();
				final int union = cntOld.get( ( long ) oldLbl ) + cntNew.get( ( long ) newLbl ) - intersection;

				if ( ( double ) intersection / union >= iouThresh )
					uf.union( oldLbl, newLbl );
			}
			offset += tileMax + 1;
		}

		// Relabel canvas: remap every pixel through find().
		relabel( canvas, uf );
	}

	private static < R extends IntegerType< R > > void relabel(
			final RandomAccessibleInterval< R > canvas,
			final UnionFind uf )
	{
		// Collect all roots, assign compact ids
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

		// Second pass: write compact labels
		final Cursor< R > write = canvas.cursor();
		while ( write.hasNext() )
		{
			final R px = write.next();
			if ( px.getInteger() > 0 )
				px.setInteger( rootToCompact.get( uf.find( px.getInteger() ) ) );
		}
	}

	/** Maximum label value in a tile (0 if empty). */
	private static < T extends IntegerType< T > > int maxLabel( final RandomAccessibleInterval< T > tile )
	{
		return tile.stream().mapToInt( IntegerType::getInteger ).max().getAsInt();
	}

	private static long packPair( int a, int b )
	{
		// canonical order so (a,b) and (b,a) are the same key
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
		return ( int ) ( key & 0xFFFFFFFFL );
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
				// path halving (one-pass compression)
				final int grandparent = parent.get( parent.get( x ) );
				parent.put( x, grandparent );
				x = grandparent;
			}
			return x;
		}

		void union( final int a, final int b )
		{
			int ra = find( a ), rb = find( b );
			if ( ra == rb )
				return;
			final int rankA = rank.get( ra ), rankB = rank.get( rb );
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
