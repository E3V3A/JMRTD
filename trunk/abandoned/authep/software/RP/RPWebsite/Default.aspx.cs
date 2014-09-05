using System;
using System.Collections.Generic;
using System.IdentityModel.Claims;
using System.Linq;
using System.Web;
using System.Web.UI;
using System.Web.UI.WebControls;
using nl.telin.authep.informationcards;

namespace RPWebsite
{
    public partial class _Default : System.Web.UI.Page
    {
        protected void Page_Load(object sender, EventArgs e)
        {
            string xmlCard = Request.Params["xmlToken"];
            if (xmlCard != null)
            {
                Token t = new Token(xmlCard);
                long dateOfBirthTicks = long.Parse(t.Claims[ClaimTypes.DateOfBirth]);
                DateTime dateOfBirth = new DateTime(dateOfBirthTicks);
                int age = (int)(DateTime.Now - dateOfBirth).Days / 365;
                
                SurnameLabel.Text = "Name: "+t.Claims[ClaimTypes.Surname].Replace("<"," ");
                DateOfBirthLabel.Text = "Born: "+dateOfBirth.ToShortDateString()+" you are: "+ age+" years.";
                if (age > 18)
                    ResultLabel.Text = "You are old enough! Go buy some!";
                else
                    ResultLabel.Text = "Sorry only cola for you.";
            }
        }
    }
}
