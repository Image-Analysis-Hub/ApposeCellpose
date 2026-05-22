/*-
 * #%L
 * Running Cellpose 3 and 4 from Java with Appose, using ImgLib2 data structure.
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
package net.imglib2.cellpose;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apposed.appose.BuildException;
import org.apposed.appose.TaskException;

import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class Cellpose
{

	/**
	 * Core method to run Cellpose 3 or Cellpose-SAM, depending on the
	 * specification of the script and environment to use. To be used by other
	 * methods in this class.
	 * 
	 * @param <T>
	 *            the pixel type of the input image.
	 * @param img
	 *            the input image. X and Y axes must be at positions 0 and 1
	 *            respectively.
	 * @param axisInfo
	 *            the AxisInfo of the input image.
	 * @param params
	 *            the parameters to run Cellpose with.
	 * @param pythonScriptPath
	 *            the path to the Python script to run (e.g. "/cp3.py" or
	 *            "/cp4.py").
	 * @param envName
	 *            the name of the Python environment to create and use (e.g.
	 *            "cp3" or "cp4").
	 * @return a list containing the label image, and optionally the flows
	 *         image. If flows are not computed, the list will contain only the
	 *         label image.
	 */
	private static < T extends RealType< T > & NativeType< T >, R extends IntegerType< R > & NativeType< R > > CellposeOutput< R > run(
			final RandomAccessibleInterval< T > input,
			final AxisInfo axisInfo,
			final CellposeParameters params,
			final String pythonScriptPath,
			final String envName,
			final ApposeTaskListener listener ) throws BuildException, IOException, InterruptedException, TaskException
	{
		if ( axisInfo.X() != 0 || axisInfo.Y() != 1 )
			throw new IllegalArgumentException( "X and Y axes must be at positions 0 and 1 respectively." );

		try (final CellposeRunner runner = new CellposeRunner( params, pythonScriptPath, envName, listener ))
		{
			runner.init();

			// Do we have a 5D image? If yes we process time by time.
			final long nt = axisInfo.nTimePoints( input );
			final long nz = axisInfo.nZ( input );

			if ( nt > 1 && nz > 1 )
			{
				/*
				 * One issue is that we don't know in advance what the type of
				 * the labels output is going to be. It can be uint32 or uint64,
				 * the latter happening if there are more that 65k labels in one
				 * time-point. And this can happen at any time-point.
				 * 
				 * For now, we are optimistic, and assume it is only uint16 for
				 * 5D use cases. Other use cases are unaffected.
				 */

				// Placeholder for labels output: XYZT.
				final long[] inputDims = input.dimensionsAsLongArray();
				final long[] ldims = new long[] {
						inputDims[ axisInfo.X() ],
						inputDims[ axisInfo.Y() ],
						inputDims[ axisInfo.Z() ],
						inputDims[ axisInfo.T() ] };
				final Dimensions labelsDim = FinalDimensions.wrap( ldims );
				final Img< UnsignedShortType > outputLabels = Util.getArrayOrCellImgFactory( labelsDim, new UnsignedShortType() ).create( ldims );

				// Placeholder for flows output if needed.
				final Img< UnsignedByteType > outputFlows;
				if ( params.computeFlows )
				{
					// XYCZT, with nC = 3 for the 3 flows.
					final long[] fdims = new long[] {
							ldims[ 0 ],
							ldims[ 1 ],
							3,
							ldims[ 2 ],
							ldims[ 3 ] };
					// 3 channels in the flows output
					outputFlows = Util.getArrayOrCellImgFactory( labelsDim, new UnsignedByteType() ).create( fdims );
				}
				else
				{
					outputFlows = null;
				}

				/*
				 * Process time point by time point.
				 */

				for ( int t = 0; t < nt; t++ )
				{
					// Input reslice.
					final RandomAccessibleInterval< T > inputTp = Views.hyperSlice( input, axisInfo.T(), t );
					final AxisInfo axisInfoTp = axisInfo.removeTimeDim();

					// Labels output reslice.
					final RandomAccessibleInterval< UnsignedShortType > outputLabelsTp = Views.hyperSlice( outputLabels, 3, t );

					// Flows output reslice.
					final RandomAccessibleInterval< UnsignedByteType > outputFlowsTp;
					if ( params.computeFlows )
						outputFlowsTp = Views.hyperSlice( outputFlows, 4, t );
					else
						outputFlowsTp = null;

					// In a CellposeOutput.
					@SuppressWarnings( { "rawtypes", "unchecked" } )
					final CellposeOutput< R > outputTp = new CellposeOutput(
							outputLabelsTp,
							axisInfoTp.removeChannelDim(),
							outputFlowsTp,
							( axisInfoTp.C() < 0 ) ? axisInfoTp.insertChannelDim( 2 ) : axisInfoTp );

					// Exec and write output in the right place.
					runner.run( inputTp, axisInfoTp, outputTp );
				}

				// Return all time-points.
				@SuppressWarnings( { "rawtypes", "unchecked" } )
				final CellposeOutput< R > out = new CellposeOutput(
						outputLabels,
						axisInfo.removeChannelDim(),
						outputFlows,
						( axisInfo.C() < 0 ) ? axisInfo.insertChannelDim( 2 ) : axisInfo );
				return out;
			}
			else
			{
				// Otherwise process in one go.
				return runner.run( input, axisInfo, null );
			}
		}
	}

	/**
	 * Run Cellpose 3 with the given parameters on the given image, and return
	 * the resulting label image, and optionally the flows.
	 * 
	 * @param <T>
	 *            the pixel type of the input image.
	 * @param img
	 *            the input image. X and Y axes must be at positions 0 and 1
	 *            respectively. If not, a {@link IllegalArgumentException} is
	 *            thrown.
	 * @param axisInfo
	 *            the AxisInfo of the input image.
	 * @param params
	 *            the parameters to run Cellpose with.
	 * @param listener
	 *            the listener to receive progress updates and messages during
	 *            the execution of the Cellpose task.
	 * 
	 * @return a {@link CellposeOutput} object containing the label image, and
	 *         optionally the flows image.
	 * 
	 * @throws BuildException
	 *             if installing and building the Python environment fails.
	 * @throws IOException
	 *             if reading the Python scripts or environment specifications
	 *             fails.
	 * @throws InterruptedException
	 *             if the Python process is interrupted while running.
	 * @throws TaskException
	 *             if executing the Python script fails.
	 */
	public static < T extends RealType< T > & NativeType< T >, R extends IntegerType< R > & NativeType< R > > CellposeOutput< R > cellpose3(
			final RandomAccessibleInterval< T > img,
			final AxisInfo axisInfo,
			final Cellpose3Parameters params,
			final ApposeTaskListener listener ) throws BuildException, IOException, InterruptedException, TaskException
	{
		final String envName = "cp3" + getBestTorchConfig();
		final String pythonScriptPath = "/cp3.py";
		return run( img, axisInfo, params, pythonScriptPath, envName, listener );
	}

	/**
	 * Run Cellpose-SAM with the given parameters on the given image, and return
	 * the resulting label image, and optionally the flows.
	 * 
	 * @param <T>
	 *            the pixel type of the input image.
	 * @param img
	 *            the input image. X and Y axes must be at positions 0 and 1
	 *            respectively. If not, a {@link IllegalArgumentException} is
	 *            thrown.
	 * @param axisInfo
	 *            the AxisInfo of the input image.
	 * @param params
	 *            the parameters to run Cellpose with.
	 * @param listener
	 *            the listener to receive progress updates and messages during
	 *            the execution of the Cellpose task.
	 * 
	 * @return a {@link CellposeOutput} object containing the label image, and
	 *         optionally the flows image.
	 * 
	 * @throws BuildException
	 *             if installing and building the Python environment fails.
	 * @throws IOException
	 *             if reading the Python scripts or environment specifications
	 *             fails.
	 * @throws InterruptedException
	 *             if the Python process is interrupted while running.
	 * @throws TaskException
	 *             if executing the Python script fails.
	 */
	public static < T extends RealType< T > & NativeType< T >, R extends IntegerType< R > & NativeType< R > > CellposeOutput< R > cellpose4(
			final RandomAccessibleInterval< T > img,
			final AxisInfo axisInfo,
			final Cellpose4Parameters params,
			final ApposeTaskListener listener ) throws BuildException, IOException, InterruptedException, TaskException
	{
		final String envName = "cp4" + getBestTorchConfig();
		final String pythonScriptPath = "/cp4.py";
		return run( img, axisInfo, params, pythonScriptPath, envName, listener );
	}

	private static String getBestTorchConfig()
	{
		// if MacOS, return "-cpu"
		if ( getOperatingSystem() == OperatingSystem.MACOS )
			return "-cpu";
		// getCudaVersion() already returns the mapped suffix (e.g. "126")
		final String cudaVersion = getCudaVersion();
		if ( cudaVersion != null )
			return "-cu" + cudaVersion;
		// else, return "-cpu"
		return "-cpu";
	}

	public enum OperatingSystem
	{
		WINDOWS, LINUX, MACOS, UNKNOWN
	}

	/**
	 * Returns the current operating system.
	 */
	private static OperatingSystem getOperatingSystem()
	{
		final String os = System.getProperty( "os.name" ).toLowerCase();
		if ( os.contains( "mac" ) || os.contains( "darwin" ) )
			return OperatingSystem.MACOS;
		if ( os.contains( "win" ) )
			return OperatingSystem.WINDOWS;
		if ( os.contains( "nux" ) || os.contains( "nix" ) || os.contains( "aix" ) )
			return OperatingSystem.LINUX;
		return OperatingSystem.UNKNOWN;
	}

	/**
	 * Returns the CUDA version available on the system by querying
	 * {@code nvidia-smi}, or {@code null} if CUDA is not available or the OS is
	 * macOS. The returned value is already mapped to the pixi environment
	 * suffix (e.g. {@code "126"}, {@code "130"}).
	 * <p>
	 * {@code nvidia-smi} is preferred over {@code nvcc} because it reflects the
	 * driver-supported CUDA version and is present on any system with a GPU
	 * driver installed, even without the full CUDA toolkit.
	 *
	 * @return a pixi suffix string such as {@code "126"}, or {@code null}.
	 */
	private static String getCudaVersion()
	{
		if ( getOperatingSystem() == OperatingSystem.MACOS )
			return null;
		try
		{
			final ProcessBuilder pb = new ProcessBuilder( "nvidia-smi" );
			pb.redirectErrorStream( true );
			final Process process = pb.start();
			final StringBuilder output = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader( process.getInputStream() ) ))
			{
				String line;
				while ( ( line = reader.readLine() ) != null )
					output.append( line ).append( "\n" );
			}
			process.waitFor();
			// nvidia-smi header contains e.g. "CUDA Version: 12.6"
			final Matcher m = Pattern
					.compile( "CUDA Version:\\s*(\\d+\\.\\d+)" )
					.matcher( output );
			if ( m.find() )
				return mapCudaVersion( m.group( 1 ) );
		}
		catch ( final IOException | InterruptedException e )
		{
			// nvidia-smi not found or failed — CUDA not available
		}
		return null;
	}

	/**
	 * Maps raw CUDA version strings (as reported by {@code nvidia-smi}) to the
	 * pixi environment suffix. Only versions listed here are supported; any
	 * other version returns {@code null}.
	 */
	static final Map< String, String > CUDA_VERSION_MAP;
	static
	{
		CUDA_VERSION_MAP = new HashMap<>();
		CUDA_VERSION_MAP.put( "12", "126" );
		CUDA_VERSION_MAP.put( "13", "130" );
	}

	/**
	 * Maps a raw CUDA version string to the pixi environment suffix using
	 * {@link #CUDA_VERSION_MAP}.
	 *
	 * @return the mapped suffix, or {@code null} if the version is not
	 *         recognized.
	 */
	private static String mapCudaVersion( final String rawVersion )
	{
		// Only pass the major version (e.g. "12" from "12.6") to the map, as
		// minor versions are not distinguished in the pixi environments.
		final String majorVersion = rawVersion.split( "\\." )[ 0 ];
		return CUDA_VERSION_MAP.get( majorVersion );
	}
}
