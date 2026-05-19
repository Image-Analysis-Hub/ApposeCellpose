package org.apposed.appose.cellpose;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

/**
 * Represents the output of Cellpose. Stores masks and flows possibly.
 */
public class CellposeOutput< T extends IntegerType< T > & NativeType< T > >
{

	/**
	 * The labels output from Cellpose. Can be {@link UnsignedShortType} or
	 * {@link UnsignedIntType}.
	 */
	public final RandomAccessibleInterval< T > labels;

	/**
	 * The flows output from Cellpose. Always 3 channels.
	 */
	public final RandomAccessibleInterval< UnsignedByteType > flows;

	/**
	 * The axis order of the output. Always XYCZT.
	 */
	public final AxisInfo axisInfo = AxisInfo.XYCZT;
	
	public CellposeOutput( final RandomAccessibleInterval< T > labels )
	{
		this( labels, null );
	}

	public CellposeOutput( final RandomAccessibleInterval< T > labels, final RandomAccessibleInterval< UnsignedByteType > flows )
	{
		this.labels = labels;
		this.flows = flows;
	}
}
