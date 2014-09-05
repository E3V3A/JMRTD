<%@ Page Language="C#" AutoEventWireup="true" CodeBehind="Default.aspx.cs" Inherits="IDPWebsite._Default" ValidateRequest="false"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" >
<head runat="server">
    <title>Auth EP - IDP</title>
    <link rel="Stylesheet" type="text/css" href="Default.css" />
</head>
<body>
    <form id="form1" runat="server">
    <object type="application/x-informationCard" name="xmlToken">
        <param name="tokenType" value="urn:oasis:names:tc:SAML:1.0:assertion" />
        <param name="issuer" value="http://schemas.xmlsoap.org/ws/2005/05/identity/issuer/self" />
        <param name="requiredClaims" value= "
            http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress
		    http://schemas.xmlsoap.org/ws/2005/05/identity/claims/privatepersonalidentifier
            "/>
        </object>
    <div id="banner" class="banner">
        Auth EP - IdP</div>
    <div id="content" class="content">
        Create a new Passport backed information card.
        <br />
        <table>
            <tr>
                <td>Document Number:</td>
                <td><asp:TextBox ID="DocnumberBox" runat="server"></asp:TextBox></td>
            </tr>
            <tr>
                <td>Date of Birth:</td>
                <td><asp:TextBox ID="DateOfBirthBox" runat="server"></asp:TextBox></td>
                <td>ddmmyy</td>
            </tr>
            <tr>
                <td>Date of Expiry:</td>
                <td><asp:TextBox ID="DateOfExpiryBox" runat="server"></asp:TextBox></td>
                <td>ddmmyy</td>
            </tr>
        </table>
        <asp:Button ID="Button1" runat="server" Text="Create Card" 
            onclick="Button1_Click" />
        <br />
        
    </div>
    <div id="passportimage" class="passportimage"><asp:Image ID="Image1" runat="server" ImageUrl="images/passports.jpg" /></div>
	<hr>
	<asp:Image ID="Image2" runat="server" ImageUrl="images/idplogo.png" />
    </form>
</body>
</html>
