using System;
using System.Configuration;
using System.IO;
using System.ServiceModel;
using System.ServiceModel.Channels;
using System.IdentityModel.Policy;
using System.IdentityModel.Claims;
using System.Text;
using RST = nl.telin.authep.sts.RequestSecurityToken;
using RSTR = nl.telin.authep.sts.RequestSecurityTokenResponse;

using nl.telin.authep.lib;
using nl.telin.authep.network;

namespace nl.telin.authep.sts
{
    /// <summary>
    /// An class representing a WSTrustResponder service for a STS that uses certificates for authentication.
    /// </summary>
    public class CertificateAuthSTS : WsTrustResponder {}
    /// <summary>
    /// An class representing a WSTrustResponder service for a STS that uses self signed cards for authentication.
    /// </summary>
    public class SelfIssuedSamlAuthSTS : WsTrustResponder {}
    /// <summary>
    /// An class representing a WSTrustResponder service for a STS that uses username and passwords for authentication.
    /// </summary>
    public class UserNameAuthSTS : WsTrustResponder {}

    /// <summary>
    /// An class representing a WSTrustResponder service. 
    /// </summary>
    public class WsTrustResponder : IWSTrustContract
    {
        /// <summary>
        /// The WS-Trust Cancel binding. 
        /// </summary>
        /// <param name="request">A RequestSecurityToken (or RequestSecurityTokenResponse) message, with WS-Addressing Action http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Cancel</param>
        /// <returns>A RequestSecurityTokenResponse message.</returns>
        public Message Cancel(Message request)
        {
            throw new FaultException("Action not implemented");
        }

        public static string BytesToHex(byte[] bytes)
        {
            StringBuilder hexString = new StringBuilder(bytes.Length);
            for (int i = 0; i < bytes.Length; i++)
            {
                hexString.Append(bytes[i].ToString("X2"));
            }
            return hexString.ToString();
        }

        /// <summary>
        /// The WS-Trust Issue binding.
        /// </summary>
        /// <param name="request">A RequestSecurityToken (or RequestSecurityTokenResponse) message, with WS-Addressing Action http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Issue </param>
        /// <returns>A RequestSecurityTokenResponse message.</returns>
        public Message Issue(Message request)
        {
            try
            {
                OperationContext context = OperationContext.Current;
                MessageProperties messageProperties = context.IncomingMessageProperties;
                RemoteEndpointMessageProperty endpointProperty = 
                    messageProperties[RemoteEndpointMessageProperty.Name] as RemoteEndpointMessageProperty;
                Console.WriteLine("Request from {0}:{1}", endpointProperty.Address, endpointProperty.Port);
                                
                if (request == null)
                {
                    throw new ArgumentNullException("request");
                }
                
                //Console.WriteLine("REQUEST: " + request.ToString());

                // Parse the incoming request, an RST
                RST rst = new RST(request.GetReaderAtBodyContents());

                //Console.WriteLine("new request (" + DateTime.Now.ToLongTimeString() + ") " + rst.KeyType);
                Console.WriteLine();
                // Try to find the PPID in the claimsets
                string ppid = "";
                AuthorizationContext ctx = OperationContext.Current.ServiceSecurityContext.AuthorizationContext;
                
                foreach (ClaimSet claimSet in ctx.ClaimSets)
                {
                    foreach (Claim c in claimSet)
                    {
                        if (c.ClaimType == "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/privatepersonalidentifier")
                            ppid = c.Resource.ToString();
                        Console.WriteLine("incoming claim: " + c.ClaimType + " resource: " + c.Resource.ToString());
                    }
                }
                string ppidBase64 = BytesToHex(UTF8Encoding.UTF8.GetBytes(ppid));
                Console.WriteLine("ppid: " + ppid + " hex: "+ppidBase64);
                string bacPath = ConfigurationManager.AppSettings["bacstore"] + ppidBase64 + ".bac";
                Console.WriteLine("BacPath: " + bacPath);
                StreamReader reader = File.OpenText(bacPath);
                string docNumber = reader.ReadLine();
                string dateOfBirth = reader.ReadLine();
                string dateOfExpiry = reader.ReadLine();
                reader.Close();
                Console.WriteLine("BAC: " + docNumber + "<<<" + dateOfBirth + "<<<" + dateOfExpiry);

                //NetworkClient client = new NetworkClient(endpointProperty.Address, 9303);
                NetworkClient client = new NetworkClient(NetworkListener.IncomingClients[endpointProperty.Address]);
                Console.WriteLine("NetworkClient found: " + client.ToString());
                client.SendBac(docNumber, dateOfBirth, dateOfExpiry);
                Console.WriteLine("BAC Send");
                DG1File dg1 = new DG1File(client.GetDG(IDGFile.EF_DG1_TAG));
                Console.WriteLine("DG1 Received");
                DG15File dg15 = new DG15File(client.GetDG(IDGFile.EF_DG15_TAG));
                Console.WriteLine("DG15 Received");
                SODFile sod = new SODFile(client.GetDG(IDGFile.EF_SOD_TAG));
                Console.WriteLine("SOD Received");
                bool sodCheck = sod.CheckDocSignature();
                Console.WriteLine("SOD DOC SIGNATURE CHECK: " + sodCheck);
                bool hashCheck = Verification.CheckHash(dg1, sod);
                Console.WriteLine("HASH CHECK DG1: " + hashCheck);
                Random random = new Random();
                byte[] message = new byte[8];
                random.NextBytes(message);
                byte[] signature = client.SendChallenge(message);
                bool aaCheck = Verification.CheckAA(dg15.PublicKey, message, signature);
                Console.WriteLine("AA CHECK: " + aaCheck);
                client.Dispose();

                RSTR rstr =null;
                // Process the request and generate an RSTR
                if (hashCheck && sodCheck && aaCheck)
                    rstr = new RSTR(rst, ppid, dg1.MRZ);
                else
                    return null;

                // Generate a response message
                Message response = Message.CreateMessage(MessageVersion.Default, Constants.WSTrust.Actions.IssueResponse, rstr);

                // Set the RelatesTo
                if ( request.Headers.MessageId != null )
                {
                    response.Headers.RelatesTo = request.Headers.MessageId;
                } 
                else 
                {
                    // not supported in this sample
                    throw new NotSupportedException("Caller must provide a Message Id");
                }

                // Send back to the caller
                return response;
            }
            catch (Exception e)
            {
                throw WSTrustFaultException.FromException(e);
            }
        }

        /// <summary>
        /// The WS-Trust Renew binding. 
        /// </summary>
        /// <param name="request">A RequestSecurityToken (or RequestSecurityTokenResponse) message, with WS-Addressing Action http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Renew </param>
        /// <returns>A RequestSecurityTokenResponse message.</returns>
        public Message Renew(Message request)
        {
            throw new FaultException<string>("Action Not Supported");
        }

        /// <summary>
        /// The WS-Trust Validate binding. 
        /// </summary>
        /// <param name="request">A RequestSecurityToken (or RequestSecurityTokenResponse) message, with WS-Addressing Action http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Validate </param>
        /// <returns>A RequestSecurityTokenResponse message.</returns>
        public Message Validate(Message request)
        {
            throw new FaultException<string>("Action Not Supported");
        }
    }
}
