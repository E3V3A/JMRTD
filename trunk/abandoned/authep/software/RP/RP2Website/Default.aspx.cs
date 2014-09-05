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
                string bsn = t.Claims["http://schemas.authep.nl/claims/personalnumber"];
                BSNLabel.Text = "Uw BSN is: " + bsn + " we zullen u gauw verder 'helpen'.";
            }
        }
    }
}
