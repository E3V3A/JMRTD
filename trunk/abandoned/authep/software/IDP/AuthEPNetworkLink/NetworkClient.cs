using System;
using System.Collections.Generic;
using System.IO;
using System.Net.Sockets;
using System.Text;
using System.Threading;

using nl.telin.authep.lib;

namespace nl.telin.authep.network
{
    /// <summary>
    /// A <b>NetworkClient</b> can connect to an AuthEPInterfacer (Java program that's used
    /// for communicating between a RFID reader and elektronic passport).
    /// </summary>
    public class NetworkClient : IDisposable
    {
        private TcpClient _client;
        private NetworkStream _stream;
        private BinaryReader _reader;
        private string _host;
        private int _port;

        /// <summary>
        /// Create a new instance of <b>NetworkClient</b> using the specified hostname and port.
        /// </summary>
        /// <param name="host">Hostname of the AuthEPInterfacer</param>
        /// <param name="port">Port number to use</param>
        public NetworkClient(string host, int port)
        {
            _host = host;
            _port = port;
            Connect();
        }

        public NetworkClient(TcpClient client)
        {
            _client = client;
            _stream = _client.GetStream();
            _reader = new BinaryReader(_stream);
        }

        private void Connect()
        {
            _client = new TcpClient(_host, _port);
            _stream = _client.GetStream();
            _reader = new BinaryReader(_stream);
        }

        private void Disconnect()
        {
            try
            {
                _client.Close();
                _stream.Close();
                _client = null;
                _stream = null;
            }
            catch { }
        }

        private void WriteString(string data)
        {
            byte[] buffer = UTF8Encoding.UTF8.GetBytes(data);
            _stream.WriteByte((byte) buffer.Length);
            _stream.Write(buffer, 0, buffer.Length);
        }

        public void SendBac(string documentNumber, string dateOfBirth, string dateOfExpiry)
        {
            _stream.WriteByte(NetworkProtocol.SEND_BAC);
            WriteString(documentNumber);
            WriteString(dateOfBirth);
            WriteString(dateOfExpiry);
        }

        /// <summary>
        /// Get a EF_DG file from the passport.
        /// </summary>
        /// <param name="dgTag">Tag of the file to get.</param>
        /// <returns>The EF_DG file as a byte array.</returns>
        public byte[] GetDG(byte dgTag)
        {
            _stream.WriteByte(NetworkProtocol.REQUEST_FILE);
            _stream.WriteByte(dgTag);

            byte[] buffer = new byte[1024];

            while (!_stream.DataAvailable)
                Thread.Sleep(10);

            int length = (int)_reader.ReadUInt32();
            byte[] data = new byte[length];
            _stream.Read(data, 0, length);
            return data;
        }

        /// <summary>
        /// Send a challenge to the passport to be used in active authentication.
        /// </summary>
        /// <param name="challenge">Message for that the passport should sign.</param>
        /// <returns>Signature over the message from the passport.</returns>
        public byte[] SendChallenge(byte[] challenge)
        {
            if (challenge.Length > byte.MaxValue) throw new ArgumentException("Challenge should not be longer then 256 bytes");
            _stream.WriteByte(NetworkProtocol.SEND_CHALLENGE);
            _stream.WriteByte((byte)challenge.Length);
            _stream.Write(challenge, 0, challenge.Length);
            while (!_stream.DataAvailable)
                Thread.Sleep(10);

            int length = (int)_reader.ReadUInt32();
            byte[] response = new byte[length];
            _stream.Read(response, 0, length);
            return response;
        }

        public override string ToString()
        {
            if (_client != null)
                return _client.Client.RemoteEndPoint.ToString();
            else if (_host != null && _host != "")
                return _host;
            else
                return "Not connected!";
        }

        #region IDisposable Members

        /// <summary>
        /// Closes this network connection and disposes of any resources that it uses.
        /// </summary>
        public void Dispose()
        {
            _stream.WriteByte(NetworkProtocol.CLOSE);
            Disconnect();
        }

        #endregion
    }
}
