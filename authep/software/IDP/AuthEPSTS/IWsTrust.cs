using System;
using System.ServiceModel;
using System.ServiceModel.Channels;

namespace nl.telin.authep.sts
{
    /// <summary>
    /// Defines the contract that a class must implement for a WSTrust conversation.
    /// </summary>
    [ServiceContract(Name = "IWSTrustContract", Namespace = Constants.WSTrust.NamespaceUri.Uri)]
    public interface IWSTrustContract
    {
        /// <summary>
        /// The WS-Trust Cancel binding. 
        /// </summary>
        /// <param name="request">A RequestSecurityToken (or RequestSecurityTokenResponse) message, with WS-Addressing Action http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Cancel.</param>
        /// <returns>A RequestSecurityTokenResponse message.</returns>
        [OperationContract(Name = "Cancel", Action = Constants.WSTrust.Actions.Cancel, ReplyAction = Constants.WSTrust.Actions.CancelResponse)]
        Message Cancel(Message request);

        /// <summary>
        /// The WS-Trust Issue binding.
        /// </summary>
        /// <param name="request">A RequestSecurityToken (or RequestSecurityTokenResponse) message, with WS-Addressing Action http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Issue.</param>
        /// <returns>A RequestSecurityTokenResponse message.</returns>
        [OperationContract(Name = "Issue", Action = Constants.WSTrust.Actions.Issue, ReplyAction = Constants.WSTrust.Actions.IssueResponse)]
        Message Issue(Message request);

        /// <summary>
        /// The WS-Trust Renew binding.
        /// </summary>
        /// <param name="request">A RequestSecurityToken (or RequestSecurityTokenResponse) message, with WS-Addressing Action http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Renew.</param>
        /// <returns>A RequestSecurityTokenResponse message.</returns>
        [OperationContract(Name = "Renew", Action = Constants.WSTrust.Actions.Renew, ReplyAction = Constants.WSTrust.Actions.RenewResponse)]
        Message Renew(Message request);

        /// <summary>
        /// The WS-Trust Validate binding. 
        /// </summary>
        /// <param name="request">A RequestSecurityToken (or RequestSecurityTokenResponse) message, with WS-Addressing Action http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Validate.</param>
        /// <returns>A RequestSecurityTokenResponse message.</returns>
        [OperationContract(Name = "Validate", Action = Constants.WSTrust.Actions.Validate, ReplyAction = Constants.WSTrust.Actions.ValidateResponse)]
        Message Validate(Message request);
    }
}
