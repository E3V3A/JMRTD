using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.IdentityModel.Claims;
using System.Web;
using System.Web.UI;
using System.Web.UI.WebControls;
using System.Text;
using System.Security.Cryptography;
using nl.telin.authep.informationcards;
using nl.telin.authep.managedcardwriter;

namespace IDPWebsite
{
    public partial class _Default : System.Web.UI.Page
    {
        protected void Page_Load(object sender, EventArgs e)
        {
            string xmlCard = Request.Params["xmlToken"];
            if (xmlCard != null)
            {
                Token t = new Token(xmlCard);
                string cardConfig = Request.MapPath("App_Data\\AuthEPCard.ini");
                string cardoutDir = Request.MapPath("Cards\\");
                string cardOutFile = Guid.NewGuid().ToString() + DateTime.Now.Ticks + ".crd";
                string cardOut = cardoutDir + cardOutFile;

                string ppid = t.Claims[ClaimTypes.PPID];
                if (ManagedCardWriter.CreateCard(cardConfig, cardOut, ppid))
                {
                    string docNumber = DocnumberBox.Text;
                    string dateOfBirth = DateOfBirthBox.Text;
                    string dateOfExpiry = DateOfExpiryBox.Text;
                    if (docNumber == "" || dateOfBirth == "" || dateOfExpiry == "")
                        throw new Exception("Please fill in Document Number, Date of Birth and Date of Expiry.");

                    string base64PPID = BytesToHex(UTF8Encoding.UTF8.GetBytes(ppid));
                    StreamWriter writer = File.CreateText(Request.MapPath("Bac\\" + base64PPID + ".bac"));
                    writer.WriteLine(docNumber);
                    writer.WriteLine(dateOfBirth);
                    writer.WriteLine(dateOfExpiry);
                    writer.Close();
                    Response.Redirect("~/Cards/" + cardOutFile);
                }
                else
                    throw new Exception("Error while creating your card, please try again later.");
            }
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

        private void ClearFields()
        {
            DocnumberBox.Text = "";
            DateOfBirthBox.Text = "";
            DateOfExpiryBox.Text = "";
        }

        protected void Button1_Click(object sender, EventArgs e)
        {

        }
    }
}