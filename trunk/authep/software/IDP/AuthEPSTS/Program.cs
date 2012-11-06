using System;
using System.ServiceModel;
using System.ServiceModel.Dispatcher;
using System.Configuration;
using System.ServiceModel.Description;
using System.ServiceModel.Channels;
using System.ServiceModel.Security.Tokens;
using System.Collections.Generic;
using System.Security.Cryptography.X509Certificates;

[assembly: CLSCompliant(true)]
namespace nl.telin.authep.sts
{
    public class Program
    {
        /// <summary>
        /// Gets the certificate specified by the thumbprint from the application settings
        /// </summary>
        /// <returns>Certificate to use for signing tokens</returns>
        public static X509Certificate2 SigningCertificate
        {
            get
            {
                string thumbprint = ConfigurationManager.AppSettings["certificateThumbprint"];
                if (string.IsNullOrEmpty(thumbprint))
                    throw new ArgumentException("thumbprint is not specified in configuration");

                X509Store store = new X509Store(StoreName.My, StoreLocation.LocalMachine);
                store.Open(OpenFlags.ReadOnly);
                X509Certificate2Collection collection = store.Certificates.Find(X509FindType.FindByThumbprint, thumbprint, true);
                if (collection.Count != 1)
                {
                    throw new NotSupportedException("There must be exactly one certificate");
                }
                return collection[0];
            }
        }


        /// <summary>
        /// Get the issuer from the application settings
        /// </summary>
        /// <returns>issuer string to use for signing tokens</returns>
        public static string Issuer
        {
            get
            {
                string result = ConfigurationManager.AppSettings["issuer"];
                if (string.IsNullOrEmpty(result))
                    throw new ArgumentException("issuer is not specified in configuration");
                return result;
            }
        }

        static void AcceptClients()
        {

        }

        /// <summary>
        /// Start the STS
        /// </summary>
        /// <param name="args">no command line arguments are used at this moment</param>
        static void Main(string[] args)
        {
            try
            {
                // start the socket acceptor
                NetworkListener nl = new NetworkListener();

                // Get base address from app settings in configuration
                Uri baseAddress = new Uri(ConfigurationManager.AppSettings["baseAddress"]);
                Uri baseMexAddress = new Uri(ConfigurationManager.AppSettings["baseMexAddress"]);

                List<ServiceHost> hostList = new List<ServiceHost>();
                try
                {
                    //
                    // Start the STSs
                    //
                    //hostList.Add(StartSTS(typeof(CertificateAuthSTS), "smartcard", baseAddress, baseMexAddress));
                    hostList.Add(StartSTS(typeof(SelfIssuedSamlAuthSTS), "selfissuedsaml", baseAddress, baseMexAddress));
                    //hostList.Add(StartSTS(typeof(UserNameAuthSTS), "usernamepassword", baseAddress, baseMexAddress));

                    Console.Write("\n\nPress <ENTER> to terminate services\n\n");
                    Console.ReadLine();
                }
                finally
                {
                    foreach (ServiceHost host in hostList)
                    {
                        host.Close();
                    }
                }
            }
            catch (Exception e)
            {
                Console.WriteLine("An unexpected fault was encountered:\n{0}", e.ToString());
            }
        }

        private static ServiceHost StartSTS(Type type, string stsLabel, Uri baseAddress, Uri baseMexAddress)
        {

            // Create the service host
            Uri stsAddress = new Uri(baseAddress.AbsoluteUri + "/" + stsLabel);
            ServiceHost serviceHost = new ServiceHost(type, stsAddress);
            
            // Don't require derived keys for the issue method
            ServiceEndpoint stsEndpoint = serviceHost.Description.Endpoints.Find(typeof(nl.telin.authep.sts.IWSTrustContract));
            BindingElementCollection bindingElements = stsEndpoint.Binding.CreateBindingElements();
            SecurityBindingElement sbe = bindingElements.Find<SecurityBindingElement>();
            RsaSecurityTokenParameters rsaParams = new RsaSecurityTokenParameters();
            rsaParams.InclusionMode = SecurityTokenInclusionMode.Never;
            rsaParams.RequireDerivedKeys = false;
            SupportingTokenParameters requirements = new SupportingTokenParameters();
            requirements.Endorsing.Add(rsaParams);
            sbe.OptionalOperationSupportingTokenParameters.Add(nl.telin.authep.sts.Constants.WSTrust.Actions.Issue, requirements);
            stsEndpoint.Binding = new CustomBinding(bindingElements);
            serviceHost.Credentials.ServiceCertificate.Certificate = SigningCertificate;

            // Add an https mex listener
            string mexAddress = baseMexAddress.AbsoluteUri + "/" + stsLabel + "/mex";
            serviceHost.AddServiceEndpoint(ServiceMetadataBehavior.MexContractName, MetadataExchangeBindings.CreateMexHttpsBinding(), mexAddress); 

            // Disable CRL
            serviceHost.Credentials.IssuedTokenAuthentication.RevocationMode = X509RevocationMode.NoCheck;

            // Open the service
            serviceHost.Open();

            // Display the endpoints
            foreach (ChannelDispatcher cd in serviceHost.ChannelDispatchers)
            {
                foreach (EndpointDispatcher ed in cd.Endpoints)
                {
                    Console.WriteLine("Listener = {0}, State = {1}", ed.EndpointAddress.ToString(), cd.State.ToString());
                }
            }

            return serviceHost;
        }
    }
}