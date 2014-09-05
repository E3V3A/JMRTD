<%@ Page Language="C#" AutoEventWireup="true" CodeBehind="Default.aspx.cs" Inherits="RPWebsite._Default" ValidateRequest="false" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" >
<head runat="server">
    <title>Auth EP - RP</title>
    <link rel="Stylesheet" type="text/css" href="Default.css" />
</head>
    <script language = "JavaScript">
	function show() //you can give any name
	{
		var win;
		win=window.open("https://www.authep.nl/authep-idp/Applet/Applet.html","title","menubar=0,resizeable=0,width=300,height=300");
	}
	</script>
<body onload="show()">
    <form id="form1" runat="server">
    <object type="application/x-informationCard" name="xmlToken">
        <param name="tokenType" value="urn:oasis:names:tc:SAML:1.0:assertion" />
        <param name="issuer" value="http://www.authep.nl:8000/sample/trust/selfissuedsaml/sts" />
        <param name="requiredClaims" value= "
			http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname
            http://schemas.xmlsoap.org/ws/2005/05/identity/claims/dateofbirth
            "/>
        </object>
    <div id="banner" class="banner">
        Auth EP - RP</div>
    <div id="content" class="content">
        Verify your age! 
        <br />
        <input type="submit" name="InfoCardSignin" value="Verify" id="InfoCardSignin" />
        <br />
        <asp:Label ID="SurnameLabel" runat="server" Text=""></asp:Label><br />
        <asp:Label ID="DateOfBirthLabel" runat="server" Text=""></asp:Label><br />
        <asp:Label ID="ResultLabel" runat="server" Text=""></asp:Label>
    </div>
    <div id="booze" class="booze">
        <asp:Image ID="Image1" runat="server" ImageUrl="~/images/booze.jpg" />
    </div>
    </form>
</body>
</html>
