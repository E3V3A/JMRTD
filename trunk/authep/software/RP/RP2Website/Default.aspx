<%@ Page Language="C#" AutoEventWireup="true" CodeBehind="Default.aspx.cs" Inherits="RPWebsite._Default" ValidateRequest="false" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" >
<head runat="server">
    <title>Auth EP - RP</title>
    <link rel="Stylesheet" type="text/css" href="Default.css" />
</head>
<body>
    <form id="form1" runat="server">
    <object type="application/x-informationCard" name="xmlToken">
        <param name="tokenType" value="urn:oasis:names:tc:SAML:1.0:assertion" />
        <param name="issuer" value="http://www.authep.nl:8000/sample/trust/selfissuedsaml/sts" />
        <param name="requiredClaims" value= "
            http://schemas.authep.nl/claims/personalnumber
            "/>
        </object>
    <div id="banner" class="banner">
        Auth EP - RP</div>
    <div id="content" class="content">
        Geef uw BSN! 
        <br />
        <input type="submit" name="InfoCardSignin" value="Verify" id="InfoCardSignin" />
        <br />
        <asp:Label ID="BSNLabel" runat="server" Text=""></asp:Label><br />
        <asp:Label ID="ResultLabel" runat="server" Text=""></asp:Label>
    </div>
    <div id="booze" class="booze">
        <asp:Image ID="Image1" runat="server" ImageUrl="~/images/belastingdienst.jpg" />
    </div>
    <div id="applet" class="applet">
        <applet code=nl.telin.authep.InterfacerApplet.class name=AuthEPApplet archive="http://www.authep.nl/authep-idp/Applet/authepinterfacer.jar, lib/jmrtd.jar, lib/bcprov-jdk16-140.jar"
			width=300 height=300>
			<param name="bgcolor" value="ffffff">
			<param name="fontcolor" value="000000">
			Your browser is not Java enabled.
		</applet>
    </div>
    </form>
</body>
</html>
