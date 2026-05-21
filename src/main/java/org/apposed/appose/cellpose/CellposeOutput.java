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
	 * The flows output from Cellpose. Always 3 channels. Can be null if the
	 * flows were not returned by Cellpose.
	 */
	public final RandomAccessibleInterval< UnsignedByteType > flows;

	/**
	 * The axes of the labels output.
	 */
	public final AxisInfo axesLabels;

	/**
	 * The axes of the flows output. Can be null if the flows were not returned
	 * by Cellpose.
	 */
	public final AxisInfo axesFlows;
	
	public CellposeOutput( final RandomAccessibleInterval< T > labels, final AxisInfo axesLabels )
	{
		this( labels, axesLabels, null, null );
	}

	public CellposeOutput( final RandomAccessibleInterval< T > labels, final AxisInfo axesLabels, final RandomAccessibleInterval< UnsignedByteType > flows, final AxisInfo axesFlows )
	{
		this.labels = labels;
		this.axesLabels = axesLabels;
		this.flows = flows;
		this.axesFlows = axesFlows;
	}
}
