/*
 * ACR122 terminal driver for javax.smartcardio framework.

 * Copyright (C) 2008  Wojciech Mostowski
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id: ACR122Provider.java,v 1.4 2008/06/27 14:41:08 woj Exp $
 */

package ds.smartcards.acr122;

import java.security.Provider;

public final class ACR122Provider extends Provider {
    public ACR122Provider() {
        super("DS", 0.9d,
                "TerminalFactory Provider for the ACR122 Smart Card Reader");
        put("TerminalFactory.ACR",
                "ds.smartcards.acr122.ACR122TerminalFactorySpi");
    }
}
