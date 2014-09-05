using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Packaging;
using System.Text;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using nl.telin.authep.network;
using nl.telin.authep.lib;
using Org.BouncyCastle.Cms;
using Org.BouncyCastle.Asn1;
using Org.BouncyCastle.Asn1.Cms;
using Org.BouncyCastle.Asn1.Icao;
using Org.BouncyCastle.Crypto.Digests;
using Org.BouncyCastle.Crypto.Engines;
using Org.BouncyCastle.Crypto.Parameters;
using Org.BouncyCastle.Crypto.Signers;

namespace AuthEPNetworkTest
{
    class Program
    {
        static void Main(string[] args)
        {
            string bacstore = @"D:\AuthEP\svn\software\IDP\IDPWebsite\Bac\";
            string ppid = "IHfP1FWxNwwvRTIBQ58xVVUnRAKZlWNPrJcHxzTF0k8=";
            string host = "localhost";
            int port = 9303;
            if (args.Length > 0)
                host = args[0];
            if (args.Length > 1)
                port = int.Parse(args[1]);

            StreamReader reader = File.OpenText(bacstore + ppid + ".bac");
            string docNumber = reader.ReadLine();
            string dateOfBirth = reader.ReadLine();
            string dateOfExpiry = reader.ReadLine();
            reader.Close();

            NetworkClient client = new NetworkClient(host, port);
            client.SendBac(docNumber, dateOfBirth, dateOfExpiry);

            List<IDGFile> dgFiles = new List<IDGFile>();
            DG1File dg1 = new DG1File(client.GetDG(IDGFile.EF_DG1_TAG));
            DG15File dg15 = new DG15File(client.GetDG(IDGFile.EF_DG15_TAG));
            dgFiles.Add(dg1);
            dgFiles.Add(dg15);
            SODFile sod = new SODFile(client.GetDG(IDGFile.EF_SOD_TAG));

            Console.WriteLine("Hello " + dg1.MRZ.getPrimaryIdentifier());
            bool hashCheck = Verification.CheckHash(dgFiles, sod);
            Console.WriteLine("Hash check result - " + hashCheck);

            if (sod.CheckDocSignature())
            {
                Console.WriteLine("SOd signature Check - PASSED!");
                Console.WriteLine("Issuing state - {0}", dg1.MRZ.getIssuingState().getName());
            }
            else
                Console.WriteLine("SOd signature Check - FAILED!");

            Random random = new Random();
            byte[] message = new byte[8];
            random.NextBytes(message);
            byte[] signature = client.SendChallenge(message);
            bool aaCheck = Verification.CheckAA(dg15.PublicKey, message, signature);
            Console.WriteLine("AA Check - " + aaCheck);
            client.Dispose();
        }
    }
}
