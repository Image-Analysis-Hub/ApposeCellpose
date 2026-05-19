/*-
 * #%L
 * Running Cellpose with a Fiji plugin based on Appose.
 * %%
 * Copyright (C) 2026 Appose developpers
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the My Company nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package org.apposed.appose.cellpose;

import java.util.List;

import net.imglib2.Dimensions;

public record AxisInfo( int X, int Y, int C, int Z, int T )
{

	public static final AxisInfo XY = new AxisInfo( 0, 1, -1, -1, -1 );

	public static final AxisInfo XYC = new AxisInfo( 0, 1, 2, -1, -1 );

	public static final AxisInfo XYZ = new AxisInfo( 0, 1, -1, 2, -1 );

	public static final AxisInfo XYT = new AxisInfo( 0, 1, -1, -1, 2 );

	public static final AxisInfo XYCZ = new AxisInfo( 0, 1, 2, 3, -1 );

	public static final AxisInfo XYCT = new AxisInfo( 0, 1, 2, -1, 3 );

	public static final AxisInfo XYZT = new AxisInfo( 0, 1, -1, 2, 3 );

	public static final AxisInfo XYCZT = new AxisInfo( 0, 1, 2, 3, 4 );

	/**
	 * Returns a new AxisInfo with the same values as this one, but with
	 * dimensionality swapped the Python order. T
	 * 
	 * @return a new AxisInfo.
	 */
	public AxisInfo toPython()
	{
		final int nDims = nDims();
		final int nX = X < 0 ? -1 : nDims - X;
		final int nY = Y < 0 ? -1 : nDims - Y;
		final int nZ = Z < 0 ? -1 : nDims - Z;
		final int nC = C < 0 ? -1 : nDims - C;
		final int nT = T < 0 ? -1 : nDims - T;
		return new AxisInfo( nX, nY, nZ, nC, nT );
	}

	/**
	 * Returns the number of non singleton dimensions, i.e. the number of
	 * dimensions with size > 1.
	 * 
	 * @return the number of non singleton dimensions.
	 */
	public int nDims()
	{
		int nd = 0;
		for ( final int d : new int[] { X, Y, Z, C, T } )
			if ( d > 1 )
				nd++;
		return nd;
	}

	/**
	 * Returns the number of channels in the specified image, provided this
	 * AxisInfo properly represents the axes of the image. If the image does not
	 * have a channel axis, this method returns 1 (there is always at least one
	 * channel).
	 * 
	 * @param img
	 *            the image.
	 * @return the number of channels in the image.
	 */
	public long nChannels( final Dimensions img )
	{
		if ( C < 0 )
			return 1l;
		return img.dimension( C );
	}

	/**
	 * Returns the number of time points in the specified image, provided this
	 * AxisInfo properly represents the axes of the image. If the image does not
	 * have a time axis, this method returns 1 (there is always at least one
	 * time point).
	 * 
	 * @param img
	 *            the image.
	 * @return the number of time points in the image.
	 */
	public int nTimePoints( final Dimensions img )
	{
		if ( T < 0 )
			return 1;
		return ( int ) img.dimension( T );
	}

	/**
	 * Returns the number of Z slices in the specified image, provided this
	 * AxisInfo properly represents the axes of the image. If the image does not
	 * have a Z axis, this method returns 1 (there is always at least one Z
	 * slice).
	 * 
	 * @param img
	 *            the image.
	 * @return the number of Z slices in the image.
	 */
	public int nZ( final Dimensions input )
	{
		if ( Z < 0 )
			return 1;
		return ( int ) input.dimension( Z );
	}

	/**
	 * Returns a new AxisInfo with the same values as this one, but with the
	 * time axis removed, i.e. with the same values for X,Y,Z,C and with T set
	 * to -1.
	 * 
	 * @return a new AxisInfo with the time axis removed.
	 */
	public AxisInfo removeTimeDim()
	{
		return new AxisInfo( X, Y, C, Z, -1 );
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		final String str = "XYCZT";
		final List< Integer > list = List.of( X, Y, C, Z, T );
		for ( int i = 0; i < 5; i++ )
		{
			final int d = list.get( i );
			if ( d >= 0 )
				sb.append( str.charAt( i ) );
		}
		return sb.toString();
	}
}
