package sos.smartcards;

import java.util.EventObject;

/**
 * Event for card insertion and removal.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: $
 */
public class CardEvent extends EventObject
{
	private static final long serialVersionUID = -5645277246646615351L;

	/** Event type constant. */
	public static final int REMOVED = 0, INSERTED = 1;

	private int type;
	private CardService service;

	/**
	 * Creates an event.
	 *
	 * @param type event type
	 * @param service event source
	 */
	public CardEvent(int type, CardService service) {
		super(service);
		this.type = type;
		this.service = service;
	}

	/**
	 * Gets the event type.
	 *
	 * @return event type
	 */
	public int getType() {
		return type;
	}

	/**
	 * Gets the event source.
	 *
	 * @return event source
	 */
	public CardService getService() {
		return service;
	}

	/**
	 * Gets a textual representation of this event.
	 * 
	 * @return a textual representation of this event
	 */
	public String toString() {
		switch (type) {
		case REMOVED: return "Card removed from " + service;
		case INSERTED: return "Card inserted in " + service;
		}
		return "CardEvent " + service;
	}

	/**
	 * Whether this event is equal to the event in <code>other</code>.
	 * 
	 * @return a boolean
	 */
	public boolean equals(Object other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (other.getClass() != CardEvent.class) { return false; }
		CardEvent otherCardEvent = (CardEvent)other;
		return type == otherCardEvent.type && service.equals(otherCardEvent.service);
	}
	
	/**
	 * Gets a hash code for this event.
	 * 
	 * @return a hash code for this event
	 */
	public int hashCode() {
		return 5 * service.hashCode() + 7 * type;
	}
}
