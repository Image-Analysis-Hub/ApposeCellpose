package org.apposed.appose.cellpose;

import java.util.function.Consumer;

import org.apposed.appose.Builder.ProgressConsumer;
import org.apposed.appose.TaskEvent;

/**
 * Interface for listeners that want to be notified about the progress and
 * results of Appose tasks. This can be used to update the user interface, log
 * progress, or perform other actions based on the Appose task's lifecycle
 * events.
 */
public interface ApposeTaskListener
{

	public static final ApposeTaskListener STD = new StdApposeTaskListener();

	public static final ApposeTaskListener VOID = new VoidApposeTaskListener();

	/**
	 * Returns a consumer that will be called with task events related to the
	 * execution of an Appose task, and that writes messages in the outputs
	 * defined in this class.
	 * 
	 * @return a new task event consumer.
	 */
	Consumer< TaskEvent > taskListener();

	/**
	 * Returns a consumer that will be called with output messages related to
	 * the execution of a Appose task, and that writes messages in the outputs
	 * defined in this class.
	 * 
	 * @return a new output message consumer.
	 */
	Consumer< String > outputListener();

	/**
	 * Returns a consumer that will be called with error messages related to the
	 * execution of a Appose task, and that writes messages in the outputs
	 * defined in this class.
	 * 
	 * @return a new error message consumer.
	 */
	Consumer< String > errorListener();

	/**
	 * Returns a consumer that will be called with progress updates related to
	 * the execution of a Appose task, and that writes messages in the outputs
	 * defined in this class.
	 * 
	 * @return a new progress update consumer.
	 */
	ProgressConsumer progressListener();

	/**
	 * Writes a message to the outputs defined in this class.
	 * 
	 * @param msg
	 *            the message to write.
	 */
	void message( String msg );

	static class VoidApposeTaskListener implements ApposeTaskListener
	{

	@Override
        public Consumer< TaskEvent > taskListener()
        {
            return event -> {};
        }

        @Override
        public Consumer< String > outputListener()
        {
            return msg -> {};
        }

        @Override
        public Consumer< String > errorListener()
        {
            return msg -> {};
        }

        @Override
        public ProgressConsumer progressListener()
        {
            return ( t, c, m ) -> {};
        }

		@Override
		public void message( final String msg )
		{}
	}
	
	static class StdApposeTaskListener implements ApposeTaskListener
	{
		@Override
		public Consumer< TaskEvent > taskListener()
		{
			return event -> {
				if ( event.message != null && !event.message.isEmpty() )
					message( event.responseType + " - " + event.message );
				if ( event.maximum > 0 )
					progress( ( double ) event.current / event.maximum );
			};
		}

		@Override
		public Consumer< String > outputListener()
		{
			return msg -> message( msg );
		}

		@Override
		public Consumer< String > errorListener()
		{
			return msg -> error( msg );
		}

		private void error( final String msg )
		{
			System.out.println( msg );
		}

		@Override
		public ProgressConsumer progressListener()
		{
			return ( t, c, m ) -> {
				message( t + ": " + String.format( "%.1f\\%", ( double ) c / m ) );
			};
		}

		@Override
		public void message( final String msg )
		{
			System.out.println( msg );
		}

		private void progress( final double d )
		{
			System.out.println( String.format( "%.1f\\%", d ) );
		}
	}
}
