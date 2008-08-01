package sos.smartcards;

import java.util.EventObject;

public class CardEvent extends EventObject
{
	private static final long serialVersionUID = -5645277246646615351L;

	public static final int REMOVED = 0, INSERTED = 1;

	private int type;
	private CardService service;

	public CardEvent(int type, CardService service) {
		super(service);
		this.type = type;
		this.service = service;
	}

	public int getType() {
		return type;
	}

	public CardService getService() {
		return service;
	}

	public String toString() {
		switch (type) {
		case REMOVED: return "Card removed from " + service;
		case INSERTED: return "Card inserted in " + service;
		}
		return "CardEvent " + service;
	}

	public boolean equals(Object other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (other.getClass() != CardEvent.class) { return false; }
		CardEvent otherCardEvent = (CardEvent)other;
		return type == otherCardEvent.type && service.equals(otherCardEvent.service);
	}
}
