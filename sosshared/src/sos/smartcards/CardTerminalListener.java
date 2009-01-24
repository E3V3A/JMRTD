package sos.smartcards;

import java.util.EventListener;

/**
 * Interface for card insertion and removal event observers.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: $
 */
public interface CardTerminalListener extends EventListener
{
	/**
	 * Called when card inserted.
	 *
	 * @param ce insertion event
	 */
	void cardInserted(CardEvent ce);

	/**
	 * Called when card removed.
	 *
	 * @param ce removal event
	 */
	void cardRemoved(CardEvent ce);
}
