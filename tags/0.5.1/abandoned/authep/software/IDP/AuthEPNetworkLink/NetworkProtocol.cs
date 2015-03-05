using System;
using System.Collections.Generic;
using System.Text;

using nl.telin.authep.lib;

namespace nl.telin.authep.network
{
    /// <summary>
    /// Contains constants and helper methods for the networkprotocol between the STS and
    /// AuthEPInterfacer program that's responsible for fetching password details.
    /// </summary>
    public class NetworkProtocol
    {
        /// <summary>
        /// Close the network link
        /// </summary>
        public const int CLOSE = 0;
        /// <summary>
        /// Request one of the passport files.
        /// </summary>
        public const int REQUEST_FILE = 1;
        /// <summary>
        /// Send a challenge to the passport to verify it's authenticity.
        /// </summary>
        public const int SEND_CHALLENGE = 2;
        /// <summary>
        /// Sends the BAC information to the passport.
        /// </summary>
        public const int SEND_BAC = 3;
    }
}
