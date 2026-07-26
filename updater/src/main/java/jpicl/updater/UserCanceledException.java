package jpicl.updater;

/**
 * Thrown by a progress listener to request that a long-running update operation be cancelled.
 */
public class UserCanceledException extends Exception {
	public UserCanceledException() {
		super("Operation cancelled by user");
	}

	public UserCanceledException(String message) {
		super(message);
	}
}
