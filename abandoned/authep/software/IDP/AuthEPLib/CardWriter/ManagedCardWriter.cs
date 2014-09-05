using System;
using System.Collections.Generic;
using System.Windows.Forms;
using System.Security.Cryptography.X509Certificates;
using System.IO;
using System.Xml;
//using System.DirectoryServices;
using System.Text;

namespace nl.telin.authep.managedcardwriter
{
    /// <summary>
    /// <b>ManagedCardWriter</b> can create managed information cards. The structure and values of these cards
    /// are defined in a ini file. The only variable is the user's personal private identifier (ppid).
    /// </summary>
    public static class ManagedCardWriter
    {
        /// <summary>
        /// Create a managed information card.
        /// </summary>
        /// <param name="inputfilename">The ini file that defines the card.</param>
        /// <param name="outputfilename">Path where the card should be written to.</param>
        /// <param name="ppid">PPID of this card's owner.</param>
        /// <returns>True if card is succesfull written (or else false).</returns>
        public static bool CreateCard( string inputfilename , string outputfilename, string ppid )
        {
            try
                {
                    Console.WriteLine("Reading card config from \n   " + inputfilename);
                    IniFile spec = new IniFile(inputfilename);
                    
                    // create the card.
                    ManagedInformationCard card = new ManagedInformationCard((DefaultValues.CardType)Enum.Parse(typeof(DefaultValues.CardType), spec["card"]["type"].value));
                    card.CardName = spec["details"]["name"].value;
                    card.CardId = spec["details"]["id"].value;
                    card.CardVersion = spec["details"]["version"].value;
                    
                    // load the image
                    FileInfo f = new FileInfo(spec["details"]["image"].value);
                    //
                    // Check file for correct extension
                    //
                    switch (f.Extension)
                    {
                        case ".jpg":
                            card.MimeType = "image/jpeg";
                            break;
                        case ".png":
                            card.MimeType = "image/png";
                            break;
                        case ".gif":
                            card.MimeType = "image/gif";
                            break;
                        case ".bmp":
                            card.MimeType = "image/bmp";
                            break;
                        case ".tiff":
                            card.MimeType = "image/tiff";
                            break;
                        default:
                            throw new BadImageFormatException("Image File " + f.FullName + " image format not supported");
                    }

                    try
                    {
                        if (!f.Exists)
                        {
                            FileInfo f2 = new FileInfo(new FileInfo(inputfilename).Directory.FullName + "\\" + spec["details"]["image"].value);
                            if (f2.Exists)
                                f = f2;
                        }
                        byte[] data = new byte[f.Length];
                        using (FileStream fstream = File.OpenRead(f.FullName))
                        {
                            fstream.Read(data, 0, data.Length);
                            card.CardLogo = data;
                        }
                    }
                    catch (Exception)
                    {
                        throw new FileLoadException("Could not retrieve the image data from the file:"+f.FullName);
                    }

                    card.IssuerId = spec["issuer"]["address"].value;
                    card.MexUri = spec["issuer"]["MexAddress"].value;

                    card.IssuerName = spec["issuer"]["name"].value;
                    card.PrivacyNoticeAt = spec["issuer"]["privacypolicy"].value; 
                    X509Certificate2 certificate = null;
                    f = new FileInfo( spec["issuer"]["certificate"].value );
                    if( f.Exists )
                    {
                        try
                        {
                            certificate = new X509Certificate2( spec["issuer"]["certificate"].value );
                        }
                        catch( System.Security.Cryptography.CryptographicException )
                        {
                            try
                            {
                                certificate = new X509Certificate2( spec["issuer"]["certificate"].value, spec["issuer"]["certificatepassword"].value );
                            }
                            catch( Exception )
                            {
                                throw new Exception( "Could not open the certificate file:"+ spec["issuer"]["certificate"].value +". Make sure the file exists and the password is correct" );
                            }
                        }
                    }
                    if( certificate == null )
                    {
                        StoreName storeName = StoreName.My;
                        StoreLocation storeLocation = StoreLocation.LocalMachine;

                        //load from store
                        string[] certspec = spec["issuer"]["certificate"].value.Split("/".ToCharArray() , StringSplitOptions.RemoveEmptyEntries );
                        try{
                            storeLocation = (StoreLocation)Enum.Parse( typeof(StoreLocation),certspec[0],true);
                        } catch( Exception)
                        {
                            throw new Exception("No Certificate Location: "+certspec[0]);
                        }
                        try{
                            storeName = (StoreName)Enum.Parse( typeof(StoreName),certspec[1],true);
                        } catch( Exception)
                        {
                            throw new Exception("No Certificate Store: "+certspec[1]+" in "+certspec[0]);
                        }

                        X509Store s = new X509Store(storeName,storeLocation);
                        s.Open(OpenFlags.MaxAllowed);
                        foreach( X509Certificate2 xCert in s.Certificates )
                        {
                            if( xCert.Subject.StartsWith( "CN=" +certspec[2] ) )
                            {
                                certificate = xCert;
                                break;
                            }
                        }
                        if( certificate == null )
                            throw new Exception( string.Format( "Could not find certificate {0} in {1}:{2}" , certspec[2] , certspec[0], certspec[1] ));
                    }
                    if (spec["claims"].keys.Length == 0)
                        throw new Exception("No claims listed.");

                    foreach (Key k in spec["claims"].keys)
                    {
                        if( DefaultValues.Claims.ContainsKey( k.value.Trim() ) )
                            card.SupportedClaims.Add(DefaultValues.Claims[k.value.Trim()]);
                        else
                        {
                            if( spec[k.value].name.Length == 0 )
                                throw new Exception("Can't find claim specification for [" + k.value + "]");

                            if( spec[k.value]["display"].value.Length == 0 )
                                throw new Exception("Can't find claim display value for claim["+k.value+"]");

                            if( spec[k.value]["description"].value.Length == 0 )
                                throw new Exception("Can't find claim description for claim ["+k.value+"] display:["+ spec[k.value]["display"].value  +"]");

                            card.SupportedClaims.Add(new ClaimInfo(k.value, spec[k.value]["display"].value , spec[k.value]["description"].value ) );
                        }
                    }

                    if (spec["tokentypes"].keys.Length == 0)
                        throw new Exception("No token types listed.");

                    card.TokenTypes = new string[spec["tokentypes"].keys.Length];

                    for (int i = 0; i < spec["tokentypes"].keys.Length; i++  )
                        card.TokenTypes[i] = spec["tokentypes"].keys[i].value.Trim();


                    if (spec["tokendetails"]["requiresappliesto"].value.Equals("true", StringComparison.CurrentCultureIgnoreCase) )
                        card.RequireAppliesTo = true;

                    card.CredentialHint = spec["Credentials"]["hint"].value;
                    if (card.CardType == DefaultValues.CardType.UserNamePassword)
                    {
                        //card.CredentialIdentifier = spec["Credentials"]["value"].value;    
                        card.CredentialIdentifier = ppid;
                        // print out sid too for the fun of it.
                        
                    }

                    if (card.CardType == DefaultValues.CardType.SelfIssuedAuth)
                    {
                        //card.CredentialIdentifier = spec["Credentials"]["value"].value;
                        card.CredentialIdentifier = ppid;
                    }

                    if (card.CardType == DefaultValues.CardType.SmartCard)
                    {
                        X509Certificate2 smartcardcertificate = null;
                        f = new FileInfo(spec["Credentials"]["value"].value);
                        if (f.Exists)
                        {
                            try
                            {
                                smartcardcertificate = new X509Certificate2(spec["Credentials"]["value"].value);
                            }
                            catch (System.Security.Cryptography.CryptographicException)
                            {
                                try
                                {
                                    smartcardcertificate = new X509Certificate2(spec["Credentials"]["value"].value, spec["Credentials"]["certificatepassword"].value);
                                }
                                catch (Exception)
                                {
                                    throw new Exception("Could not open the smartcard certificate file:" + spec["Credentials"]["value"].value + ". Make sure the file exists and the password is correct");
                                }
                            }
                        }
                            StoreName storeName = StoreName.My;
                            StoreLocation storeLocation = StoreLocation.CurrentUser;

                        if (smartcardcertificate == null && spec["Credentials"]["value"].value.Split("/".ToCharArray(), StringSplitOptions.RemoveEmptyEntries).Length == 3 )
                        {

                            //load from store
                            string[] certspec = spec["Credentials"]["value"].value.Split("/".ToCharArray(), StringSplitOptions.RemoveEmptyEntries);
                            try
                            {
                                storeLocation = (StoreLocation)Enum.Parse(typeof(StoreLocation), certspec[0], true);
                            }
                            catch (Exception)
                            {
                                throw new Exception("No Smartcard Certificate Location: " + certspec[0]);
                            }
                            try
                            {
                                storeName = (StoreName)Enum.Parse(typeof(StoreName), certspec[1], true);
                            }
                            catch (Exception)
                            {
                                throw new Exception("No Smartcard Certificate Store: " + certspec[1] + " in " + certspec[0]);
                            }



                            X509Store s = new X509Store(storeName, storeLocation);
                            s.Open(OpenFlags.MaxAllowed);
                            foreach (X509Certificate2 xCert in s.Certificates)
                            {
                                if (xCert.Subject.StartsWith("CN=" + certspec[2]))
                                {
                                    smartcardcertificate = xCert;
                                    break;
                                }
                            }
                        }

                        if (smartcardcertificate == null)
                        {
                         X509Store s = new X509Store(storeName, storeLocation);
                         s.Open(OpenFlags.MaxAllowed);
                         foreach (X509Certificate2 xCert in s.Certificates)
                         {
                          if (xCert.Thumbprint.Equals(spec["Credentials"]["value"].value, StringComparison.CurrentCultureIgnoreCase))
                          {
                           smartcardcertificate = xCert;
                           break;
                          }
                         }

                         if (smartcardcertificate == null)
                         {
                          Console.WriteLine("Did not find smart card certificate, setting smartcard certificate hash to [" + spec["Credentials"]["value"].value + "]");
                          card.CredentialIdentifier = spec["Credentials"]["value"].value;
                         }
                         else
                         {
                          Console.WriteLine("Found smart card certificate, setting smartcard certificate hash to [" + Convert.ToBase64String(smartcardcertificate.GetCertHash()) + "]");
                          card.CredentialIdentifier = Convert.ToBase64String(smartcardcertificate.GetCertHash());
                         }

                        }
                        else
                        {
                         Console.WriteLine("Found smart card certificate, setting smartcard certificate hash to [" + Convert.ToBase64String(smartcardcertificate.GetCertHash()) + "]");
                         card.CredentialIdentifier = Convert.ToBase64String(smartcardcertificate.GetCertHash());
                        }
                    }
                    card.SerializeAndSign(outputfilename, certificate);
                    Console.WriteLine("Card written to \n   " + outputfilename);
                    return true;
                }
                catch( Exception e )
                {
                    Console.WriteLine("Exception:\n"+e.Message);
                    Console.WriteLine("\n"+e.StackTrace);
                }
            return false;
            }
    }
}