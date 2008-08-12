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
 * $Id: ACR122TerminalFactorySpi.java,v 1.20 2008/06/27 14:52:24 woj Exp $
 */

package ds.smartcards.acr122;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import javax.smartcardio.*;

public class ACR122TerminalFactorySpi extends TerminalFactorySpi {

    /** Two constants to represent card's presence */
    private static int PRESENT = 1;

    private static int ABSENT = 0;

    /** The Control IDentifier byte for ISO14443 protocol, seem to be always 0x01 */
    private static final byte CID = (byte) 0x01;

    /**
     * The maximum size of the block sent out to the card. Could probably be
     * larger.
     */
    private static final int TR_BLOCK_SIZE = 32;

    /**
     * The maximum timeout for the card to reply to an APDU. Set to 10 seconds.
     * This could probably set to infinite, but it may happen that the card
     * session will lock (I have seen this happening). If the card needs more
     * than the given time to process an APDU it has to explicitly ask for a time
     * extension (I assume), see below in ACRChannel.transmit.
     */
    private static final long TIMEOUT = 10000;

    /**
     * Constants and commands found in the ACR122 documentation:
     * http://www.acr122.com/NFC-smart-card-reader/ACR122-API-Manual/API_ACR122.pdf.
     * Some (RAW_SEND) are not documented there. More generally these are
     * commands for the PN532 controller.
     */
    private static final byte ACR_CLA = (byte) 0xFF;

    private static final byte ACR_BYTE = (byte) 0xD4;

    private static final byte ACR_BYTE_ACK = (byte) 0xD5;

    private static final byte RAW_SEND = 0x42;

    private static final byte RAW_SEND_ACK = 0x43;

    private static final byte POLL_ACK = 0x4B;

    private static final byte RESP_BYTE_OK = 0x00;

    private static final byte[] READER_ID = new byte[] { ACR_CLA, 0x00, 0x48,
            0x00, 0x00 };

    private static final byte[] POLL_COMMAND = new byte[] { ACR_BYTE, 0x4A,
            0x01, 0x00 };

    private static final byte[] ANTENNA_OFF = new byte[] { ACR_BYTE, 0x32,
            0x01, 0x00 };

    private static final byte[] TIMEOUT_ONE = new byte[] { ACR_BYTE, 0x32,
            0x05, 0x00, 0x00, 0x00 };

    private static final byte[] DESELECT = new byte[] { ACR_BYTE, 0x44, 0x01 };

    private static final byte[] BAUD_424 = new byte[] { ACR_BYTE, 0x4E, 0x01,
            0x02, 0x02 };

    private static final byte[] LEVEL4_OFF = new byte[] { ACR_BYTE, 0x12, 0x24 };

    private static final byte[] RATS = new byte[] { ACR_BYTE, RAW_SEND,
            (byte) 0xE0, 0x51 };

    private static final byte[] PING = new byte[] { ACR_BYTE, RAW_SEND,
            (byte) 0xB0, 0x01 };

    private static final byte[] ACK = new byte[] { ACR_BYTE, RAW_SEND,
            (byte) 0xA0, 0x01 };

    // These are not used at the moment:
    private static final byte[] ANTENNA_ON = new byte[] { ACR_BYTE, 0x32, 0x01,
            0x01 };

    private static final byte[] LEVEL4_ON = new byte[] { ACR_BYTE, 0x12, 0x34 };

    private static final byte[] BAUD_212 = new byte[] { ACR_BYTE, 0x4E, 0x01,
            0x01, 0x01 };

    private static final byte[] POLL_COMMAND_B = new byte[] { ACR_BYTE, 0x4A,
            0x01, 0x03, 0x00 };

    /** Wraps a command to be sent to the ACR terminal with FF 00 00 00 Len */
    private static byte[] ACRcommand(byte[] command) {
        byte[] result = new byte[command.length + 5];
        result[0] = ACR_CLA;
        result[4] = (byte) command.length;
        System.arraycopy(command, 0, result, 5, command.length);
        return result;
    }

    /** Debugging and infoing */
    private static final boolean DEBUG = false;

    private static final boolean INFO = false;

    private static void debug(Object o) {
        if (DEBUG) {
            System.out.println("DEBUG: " + o.toString());
        }
    }

    private static void info(Object o) {
        if (INFO) {
            System.out.println("INFO: " + o.toString());
        }
    }

    private static String byteArrayToString(byte[] a, boolean space) {
        if (a == null)
            return "NULL";
        String sep = space ? " " : "";
        String result = "";
        String onebyte = null;
        for (int i = 0; i < a.length; i++) {
            onebyte = Integer.toHexString(a[i]);
            if (onebyte.length() == 1)
                onebyte = "0" + onebyte;
            else
                onebyte = onebyte.substring(onebyte.length() - 2);
            result = result + onebyte.toUpperCase() + sep;
        }
        return result;
    }

    /** Singleton instance of the CardTerminals class */
    private static CardTerminals instance = null;

    public ACR122TerminalFactorySpi(Object parameter) {
        debug("SPI construction.");
    }

    public CardTerminals engineTerminals() {
        debug("engineTerminals");
        if (instance == null) {
            instance = new ACR122CardTerminals();
        }
        return instance;
    }

    private class ACR122CardTerminals extends CardTerminals {

        /** There is only one terminal - the virtual ACR122 card (ATR: 3B00) */
        private ACR122CardTerminal terminal = new ACR122CardTerminal();

        /** Lists the terminals that match the state. */
        public List<CardTerminal> list(State state) throws CardException {
            if (terminal.virtualCard != null) {
                if (state == CardTerminals.State.CARD_INSERTION
                        && terminal.state == PRESENT && terminal.inserted) {
                    return list();
                } else if (state == CardTerminals.State.CARD_REMOVAL
                        && terminal.state == ABSENT && terminal.removed) {
                    return list();
                } else if (state == CardTerminals.State.CARD_PRESENT
                        && terminal.state == PRESENT) {
                    return list();
                } else if (state == CardTerminals.State.CARD_ABSENT
                        && terminal.state == ABSENT) {
                    return list();
                } else if (state == CardTerminals.State.ALL) {
                    return list();
                }

            }
            return new LinkedList<CardTerminal>();
        }

        /** Lists all avaialable terminals */
        public List<CardTerminal> list() throws CardException {            
            List<CardTerminal> list = new LinkedList<CardTerminal>();
            if(terminal.virtualCard != null) {
              list.add(terminal);
            }
            return list;
        }

        /**
         * Wait for a change (card insertion or removal) in any of the
         * terminals.
         * 
         * @return whether the change occured before timeout expired
         */
        public boolean waitForChange(long timeout) throws CardException {
            return terminal.waitForChange(timeout);
        }

    }

    private class ACR122CardTerminal extends CardTerminal {

        /** Card present or absent state */
        private int state = ABSENT;

        /** Name of the terminal */
        private String name = null;

        /**
         * Whether a card has been inserted or removed since the last call to
         * waitFor...()
         */
        private boolean inserted = false;

        private boolean removed = false;

        /**
         * The ACR virtual card (ATR 3B00) and channel we will be talking to to
         * set the protocol parameters and to communicate with the actual card.
         */
        private Card virtualCard = null;

        private CardChannel channel = null;

        /** The ATR / ATR of the current card */
        private byte[] ats = null;

        private byte[] uid = null;

        /**
         * The sequence byte that is used in the ISO14443 protocol. Alternates
         * between 0x0A and 0x0B. (Actuall it is a one bit the byte that
         * alternates, but the rest of the byte never changes in our case.
         */
        private byte sequenceByte = (byte) 0x0a;

        private void flipSequenceByte() {
            if (sequenceByte == (byte) 0x0a) {
                sequenceByte = (byte) 0x0b;
            } else {
                sequenceByte = (byte) 0x0a;
            }
        }

        /**
         * Construct the terminal. First finds the actual PC/SC ACR terminal
         * through the regular Sun PCSC provider. Then initializes a connection
         * to the virtual card and sets some preliminary protocol paramers
         * (reset the antenna, get the reader id and store it, set timeout to
         * zero (see ACR122 API docs), and switch off the automatic ISO14443-4
         * activation.
         */
        private ACR122CardTerminal() {
            try {
                TerminalFactory tf = TerminalFactory.getInstance("PC/SC", null,
                        "SunPCSC");
                for (CardTerminal terminal : tf.terminals().list(
                        CardTerminals.State.CARD_PRESENT)) {
                    debug("Terminal: " + terminal.getName());
                    if (terminal.getName().indexOf("ACR") == -1)
                        continue;
                    virtualCard = terminal.connect("T=0");
                    channel = virtualCard.getBasicChannel();
                    channel.transmit(new CommandAPDU(ACRcommand(ANTENNA_OFF)));
                    byte[] res = channel.transmit(new CommandAPDU(READER_ID))
                            .getBytes();
                    name = new String(res, 0, res.length - 2);
                    channel.transmit(new CommandAPDU(ACRcommand(TIMEOUT_ONE)));
                    channel.transmit(new CommandAPDU(ACRcommand(LEVEL4_OFF)));
                }
            } catch (Exception e1) {
                virtualCard = null;
                debug("Exception occured: " + e1);
            }

        }

        /**
         * Connect to the card. For now ignore the protocol parameter, only
         * store.
         */
        public Card connect(String protocol) throws CardException {
            if (virtualCard == null)
                throw new CardException("Reader not initilised.");
            if (!isCardPresent()) {
                throw new CardException("No card present.");
            }
            // Spit out the UID:
            info("Card UID: " + byteArrayToString(uid, false));
            return new ACRCard(ats, this, protocol, channel);
        }

        public String getName() {
            return name;
        }

        public String toString() {
            return getName();
        }

        /**
         * Checks if there is a card present. If the card is not present an
         * attempt is made to initialize a new card, get the ATR and UID.
         * 
         * @return true iff there is a card present.
         */
        public boolean isCardPresent() throws CardException {
            if (virtualCard == null)
                throw new CardException("Reader not initilised.");
            byte[] res = null;
            synchronized (channel) {
                byte[] ping = getPing();
                debug("Ping sent: " + byteArrayToString(ping, false));
                res = channel.transmit(new CommandAPDU(ACRcommand(ping)))
                        .getBytes();
                // Calculate the correct echo byte:
                byte pByte = ping[2];
                if (pByte == (byte) 0xBA) {
                    pByte = (byte) 0xAB;
                } else {
                    pByte = (byte) 0xAA;
                }

                debug("Ping result: " + byteArrayToString(res, false));

                // If the echo is correct, the card is present
                if (res[3] == pByte && res[4] == CID) {
                    state = PRESENT;
                } else {
                    // Poll the card
                    res = channel.transmit(
                            new CommandAPDU(ACRcommand(POLL_COMMAND)))
                            .getBytes();
                    debug("Poll result: " + byteArrayToString(res, false));
                    int index = 0;
                    if (res[index] == ACR_BYTE_ACK)
                        index++;
                    if (res[index] == POLL_ACK)
                        index++;
                    if (res[index] == (byte) 0x00) {
                        state = ABSENT;
                    } else {
                        // The card is there, get its UID and ATS/ATR, set the
                        // communication speed.
                        state = PRESENT;
                        sequenceByte = (byte) 0x0A;
                        index += 3;
                        int uidLen = res[index++];
                        uid = new byte[uidLen];
                        System.arraycopy(res, index, uid, 0, uidLen);
                        index += uidLen;
                        channel.transmit(new CommandAPDU(ACRcommand(BAUD_424)));
                        res = channel.transmit(
                                new CommandAPDU(ACRcommand(RATS))).getBytes();
                        index = 3;
                        ats = new byte[res[index++]];
                        System.arraycopy(res, index, ats, 0, ats.length);
                    }
                }
                return (state == PRESENT);
            }
        }

        /**
         * Wait for a given card presence state (present or absent) with a
         * timeout. Also note whether the new state (after the required state
         * has been reached) is different from the previous one.
         * 
         * @param timeout
         *            the timeout
         * @param present
         *            whether we wait for presence or absence
         * @return whether the card change occured before the timeout expired
         * @throws CardException
         */
        private boolean waitForCardState(long timeout, final boolean present)
                throws CardException {
            int lastState = state;
            if (timeout < 0)
                throw new IllegalArgumentException("Negative timeout.");
            boolean result = true;

            final Object lockThis = this;
            Thread t = new Thread() {
                public void run() {
                    try {
                        while (true) {
                            if (interrupted()) {
                                break;
                            }
                            if (isCardPresent() == present) {
                                synchronized (lockThis) {
                                    lockThis.notify();
                                }
                                break;
                            }
                        }
                    } catch (CardException ex) {
                        debug("Exception occured: " + ex);
                    }
                }

            };
            t.start();

            synchronized (this) {
                try {
                    this.wait(timeout);
                } catch (InterruptedException ie) {

                }
            }
            if (t.isAlive()) {
                result = false;
                t.interrupt();
            }
            removed = false;
            inserted = false;
            inserted = (state == PRESENT && lastState == ABSENT);
            removed = (state == ABSENT && lastState == PRESENT);
            return result;
        }

        /**
         * Wait for a card presence state to change.
         * 
         * @param timeout
         *            the timeout
         * @return whether the card change occured before the timeout expired
         * @throws CardException
         */
        public boolean waitForCardAbsent(long timeout) throws CardException {
            return waitForCardState(timeout, false);
        }

        /**
         * Wait for a card to be present.
         * 
         * @param timeout
         *            the timeout
         * @return whether the card change occured before the timeout expired
         * @throws CardException
         */
        public boolean waitForCardPresent(long timeout) throws CardException {
            return waitForCardState(timeout, true);
        }

        /**
         * Wait for a card to be absent.
         * 
         * @param timeout
         *            the timeout
         * @return whether the card change occured before the timeout expired
         * @throws CardException
         */
        public boolean waitForChange(long timeout) throws CardException {
            if (state == PRESENT)
                return waitForCardAbsent(timeout);
            else
                return waitForCardPresent(timeout);
        }

        private byte[] getPing() {
            byte[] ping = new byte[PING.length];
            System.arraycopy(PING, 0, ping, 0, PING.length);
            ping[2] |= sequenceByte;
            return ping;
        }

        private byte[] getAck() {
            byte[] ack = new byte[ACK.length];
            System.arraycopy(ACK, 0, ack, 0, ACK.length);
            ack[2] |= sequenceByte;
            return ack;
        }

    }

    private class ACRCard extends Card {

        /** The card's ATR */
        private ATR atr = null;

        /** The owning terminal */
        private ACR122CardTerminal terminal = null;

        /** The protocol: "T=0", "T=1", "T=CL". For now we ignore it. */
        private String protocol = null;

        /**
         * The virtual card channel we will be talking to to communicate with
         * the actual card.
         */
        private CardChannel virtualChannel = null;

        private ACRCard(byte[] ats, ACR122CardTerminal terminal,
                String protocol, CardChannel channel) {
            super();
            atr = new ATR(ats);
            this.terminal = terminal;
            this.protocol = protocol;
            virtualChannel = channel;
        }

        /**
         * Returns the ATR of the card.
         * 
         * @return the ATR of the card.
         */
        public ATR getATR() {
            if (terminal.state == ABSENT)
                throw new IllegalStateException(
                        "Card is not present or disconnected.");
            return atr;
        }

        /**
         * Gains exclusive access to the card. NB. The ACR reader can probably
         * talk to more than one card at the same time. I am not sure the
         * smardcardio API allows this.
         */
        public void beginExclusive() throws CardException {
            if (terminal.state == ABSENT)
                throw new IllegalStateException(
                        "Card is not present or disconnected.");
            virtualChannel.getCard().beginExclusive();
        }

        /**
         * Releases the exclusive access to the card.
         */
        public void endExclusive() throws CardException {
            if (terminal.state == ABSENT)
                throw new IllegalStateException(
                        "Card is not present or disconnected.");
            virtualChannel.getCard().endExclusive();
        }

        /** Disconnect the card, possibly reset the reader. */
        public void disconnect(boolean reset) throws CardException {
            if (terminal.state == ABSENT)
                throw new IllegalStateException(
                        "Card is not present or already disconnected.");
            virtualChannel.transmit(new CommandAPDU(ACRcommand(DESELECT)));
            if (reset) {
                virtualChannel
                        .transmit(new CommandAPDU(ACRcommand(ANTENNA_OFF)));
            }
            terminal.state = ABSENT;
        }

        public String getProtocol() {
            if (terminal.state == ABSENT)
                throw new IllegalStateException(
                        "Card is not present or disconnected.");
            return protocol;
        }

        /**
         * Builds the basic logical channel (0) to communicate with the
         * application on the card.
         */
        public CardChannel getBasicChannel() {
            if (terminal.state == ABSENT)
                throw new IllegalStateException(
                        "Card is not present or disconnected.");
            try {
                return new ACRChannel(this, true);
            } catch (CardException ce) {
                // This comment is right. When the ACRChannel constructor is
                // called with
                // basic == true then no exception can possibly occur.
                debug("This couldn't have happened :D");
                return null;
            }
        }

        /**
         * Opens a new logical channel (1+) to communicate with the application
         * on the card.
         */
        public CardChannel openLogicalChannel() throws CardException {
            if (terminal.state == ABSENT)
                throw new IllegalStateException(
                        "Card is not present or disconnected.");
            return new ACRChannel(this, false);
        }

        /**
         * Transmits a control command to the card. For now we assume that we
         * transmit a raw data to the ACR virtual card.
         */
        public byte[] transmitControlCommand(int controlId, byte[] cmd)
                throws CardException {
            if (terminal.state == ABSENT)
                throw new IllegalStateException(
                        "Card is not present or disconnected.");
            return virtualChannel.transmit(new CommandAPDU(cmd)).getBytes();
        }
    }

    /**
     * This is where the actual communication with the card using the ISO14443-4
     * protocol happens. This is a bit messy :D
     */
    private class ACRChannel extends CardChannel {

        /** The "parent" card. */
        private ACRCard card = null;

        /** The channel number (0 = basic). */
        private int number = 0;

        /** Temp. buffer for APDU communication. */
        private byte[] temp = new byte[300];

        /**
         * Construct a new channel. For channels > 0 (non-basic) use the MANAGE
         * CHANNEL command to open a new channel.
         */
        private ACRChannel(ACRCard card, boolean basic) throws CardException {
            this.card = card;
            if (basic) {
                return;
            }
            ResponseAPDU resp = transmit(new CommandAPDU(0, 0x70, 0, 0, 1));
            if (resp.getSW() != 0x9000) {
                throw new CardException("Channel could not be opened.");
            }
            number = resp.getBytes()[0];
        }

        /** Close the channel. Uses the MANAGE CHANNEL command. */
        public void close() throws CardException {
            if (number == 0) {
                throw new CardException("Cannot close the basic channel.");
            }
            ResponseAPDU resp = transmit(new CommandAPDU(0, 0x70, 0x80,
                    (byte) number, 1));
            if (resp.getSW() != 0x9000) {
                throw new CardException("Channel could not be closed.");
            }
        }

        /**
         * Returns the "parent" card.
         * 
         * @return the "parent" card.
         */
        public Card getCard() {
            return card;
        }

        /**
         * Returns the channel number.
         * 
         * @return the channel number.
         */
        public int getChannelNumber() {
            return number;
        }

        /**
         * This method is used to set the terminal state to "card absent". It is
         * called by all the methods that may suspect that the card may have
         * been removed, due to e.g. certain communication problems.
         * 
         * @param setState
         *            should the terminal state be set
         * @throws CardException
         */
        private void noCard(boolean setState) throws CardException {
            if (setState) {
                card.terminal.state = ABSENT;
            }
            throw new CardException("No card present?");
        }

        /**
         * Wraps a portion of an APDU into a ACR send APDU command. Parameter
         * last indicates if this is the last portion, in which case the
         * chaining bit is set to 0.
         */
        private byte[] wrapAPDU(byte[] apdu, int index, int length, boolean last) {
            byte[] c = new byte[length + 4 + 5];
            c[0] = ACR_CLA;
            c[4] = (byte) (length + 4);
            c[5] = ACR_BYTE;
            c[6] = RAW_SEND;
            c[7] = last ? card.terminal.sequenceByte
                    : (byte) (card.terminal.sequenceByte | (byte) 0x10);
            c[8] = CID;
            System.arraycopy(apdu, index, c, 9, length);
            debug("wrapAPDU: " + byteArrayToString(c, false));
            return c;
        }

        /**
         * Unwraps a portion of the response APDU sent back by the ACR terminal.
         * Checks the wrapping bytes for correctness of the protocol.
         */
        private byte[] unwrapAPDU(byte[] r) throws CardException {
            debug("unwrapAPDU 1: " + byteArrayToString(r, false));
            if (r[0] != ACR_BYTE_ACK
                    || r[1] != RAW_SEND_ACK
                    || r[2] != RESP_BYTE_OK
                    || (byte) (r[3] & (byte) 0x0F) != card.terminal.sequenceByte
                    || r[4] != CID) {
                noCard(true);
            }
            byte[] result = new byte[r.length - 7];
            System.arraycopy(r, 5, result, 0, result.length);
            debug("unwrapAPDU 2: " + byteArrayToString(result, false));
            return result;
        }

        /** Checks the chaining acknowlegment response. */
        void checkChainACK(ResponseAPDU resp) throws CardException {
            if (resp.getBytes()[2] != RESP_BYTE_OK
                    || (byte) (resp.getBytes()[3] & 0x0F) != card.terminal.sequenceByte) {
                noCard(true);
            }
        }

        /**
         * Transmits the APDU cmd over this logical channel.
         * 
         * @return the response APDU.
         */
        public ResponseAPDU transmit(CommandAPDU cmd) throws CardException {
            if (card.terminal.state == ABSENT) {
                noCard(false);
            }
            synchronized (card.virtualChannel) {
                byte[] apdu = cmd.getBytes();
                apdu[0] = (byte) (apdu[0] | (byte) number);

                int totalBlocks = apdu.length / TR_BLOCK_SIZE;
                int lastSize = TR_BLOCK_SIZE;
                if (totalBlocks * TR_BLOCK_SIZE < apdu.length) {
                    totalBlocks++;
                    lastSize = apdu.length % TR_BLOCK_SIZE;
                }
                // First send all the blocks minus the last one with the
                // chaining bit set, check the
                // acknowlegnemnts on each send.
                for (int i = 0; i < totalBlocks - 1; i++) {
                    byte[] block = wrapAPDU(apdu, i * TR_BLOCK_SIZE,
                            TR_BLOCK_SIZE, false);
                    ResponseAPDU resp = card.virtualChannel
                            .transmit(new CommandAPDU(block));
                    checkChainACK(resp);
                    card.terminal.flipSequenceByte();
                }

                // Send the last block without the chaining bit set
                byte[] block = wrapAPDU(apdu,
                        (totalBlocks - 1) * TR_BLOCK_SIZE, lastSize, true);
                ResponseAPDU resp = card.virtualChannel
                        .transmit(new CommandAPDU(block));

                int index = 0;

                // Here we are at the point where we start receiving the
                // response APDU.
                // If the card is not ready with the response we give it TIMEOUT
                // milliseconds
                // to come up with one. It is also at this point that the card
                // may wish to
                // request a processing time extension. In this case we restart
                // counting the timeout.
                long start = System.currentTimeMillis();
                CommandAPDU c = new CommandAPDU(ACRcommand(card.terminal
                        .getPing()));
                CommandAPDU cext = null;
                while (resp.getBytes()[2] != RESP_BYTE_OK
                        || (byte) (resp.getBytes()[3] & 0xF0) == (byte) 0xF0) {
                    if (resp.getBytes()[2] != RESP_BYTE_OK) {
                        debug("response retry ping: "
                                + byteArrayToString(c.getBytes(), false));
                        resp = card.virtualChannel.transmit(c);
                        debug("response retry resp: "
                                + byteArrayToString(resp.getBytes(), false));
                    } else {
                        cext = new CommandAPDU(ACRcommand(new byte[] {
                                (byte) 0xd4, 0x42, resp.getBytes()[3],
                                resp.getBytes()[4], resp.getBytes()[5] }));
                        debug("response waitext ping: "
                                + byteArrayToString(cext.getBytes(), false));
                        resp = card.virtualChannel.transmit(cext);
                        debug("response waitext resp: "
                                + byteArrayToString(resp.getBytes(), false));
                        start = System.currentTimeMillis();
                    }
                    long current = System.currentTimeMillis();
                    if (current - start > TIMEOUT) {
                        noCard(true);
                    }
                }

                // The answer should be ready, get it from terminal piece by
                // piece, acknowlege every piece.
                byte[] r = null;
                while ((byte) (resp.getBytes()[3] & 0xF0) == (byte) 0x10) {
                    r = unwrapAPDU(resp.getBytes());
                    System.arraycopy(r, 0, temp, index, r.length);
                    index += r.length;
                    card.terminal.flipSequenceByte();
                    byte[] ack = card.terminal.getAck();
                    debug("chain resp ack: " + byteArrayToString(ack, false));
                    resp = card.virtualChannel.transmit(new CommandAPDU(
                            ACRcommand(ack)));
                    debug("chain resp: "
                            + byteArrayToString(resp.getBytes(), false));
                }
                r = unwrapAPDU(resp.getBytes());
                card.terminal.flipSequenceByte();
                System.arraycopy(r, 0, temp, index, r.length);
                index += r.length;
                r = new byte[index];
                System.arraycopy(temp, 0, r, 0, r.length);
                return new ResponseAPDU(r);
            }
        }

        /**
         * Transmit an APDU placed in the cmd buffer, put the response in the
         * resp buffer.
         * 
         * @retrun the number of bytes in the response.
         */
        public int transmit(ByteBuffer cmd, ByteBuffer resp)
                throws CardException {
            ResponseAPDU r = transmit(new CommandAPDU(cmd.array()));
            resp.put(r.getBytes());
            return r.getBytes().length;
        }

    }

}
