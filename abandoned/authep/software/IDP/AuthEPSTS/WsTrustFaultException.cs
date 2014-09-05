using System;
using System.ServiceModel;


namespace nl.telin.authep.sts
{
    /// <summary>
    /// A base class for WSFault exceptions. 
    /// </summary>
    [Serializable]
    public abstract class WSTrustFaultException : FaultException
    {
        /// <summary>
        /// Builds a WSTrust fault exception using the information in the specified exception. 
        /// </summary>
        /// <param name="ex">The exception to wrap in a <see cref="WSTrustFaultException"/>.</param>
        /// <returns></returns>
        public static WSTrustFaultException FromException( Exception ex )
        {
            WSTrustFaultException faultException = ex as WSTrustFaultException;

            if ( faultException == null )
            {
                faultException = new RequestFailedFaultException( ex );
            }

            return faultException;
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="WSTrustFaultException"/> class. 
        /// </summary>
        /// <param name="reason">The fault reason.</param>
        /// <param name="code">The fault code.</param>
        protected WSTrustFaultException( FaultReason reason, FaultCode code )
            : base( reason, code )
        {
        }
    }

    /// <summary>
    /// Thrown when an invalid request is recieved. 
    /// </summary>
    [Serializable]
    public class InvalidRequestFaultException : WSTrustFaultException
    {
        /// <summary>
        /// Initializes a new instance of the <see cref="InvalidRequestFaultException"/> class. 
        /// </summary>
        public InvalidRequestFaultException()
            : base( new FaultReason( "The request was invalid or malformed" ), new FaultCode( "sender", "http://www.w3.org/2003/05/soap-envelope", new FaultCode( "InvalidRequest", Constants.WSTrust.NamespaceUri.Uri ) ) )
        {
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="InvalidRequestFaultException"/> class. 
        /// </summary>
        /// <param name="innerException">The inner exception.</param>
        public InvalidRequestFaultException( Exception innerException )
            : base( new FaultReason( "The request was invalid or malformed" ), new FaultCode( "sender", "http://www.w3.org/2003/05/soap-envelope", new FaultCode( "InvalidRequest", Constants.WSTrust.NamespaceUri.Uri ) ) )
        {
        }
    }

    /// <summary>
    /// Thrown when the request fails. 
    /// </summary>
    [Serializable]
    public class RequestFailedFaultException : WSTrustFaultException
    {
        /// <summary>
        /// Initializes a new instance of the <see cref="RequestFailedFaultException"/> class. 
        /// </summary>
        public RequestFailedFaultException()
            : base( new FaultReason( "The specified request failed" ), new FaultCode( "sender", "http://www.w3.org/2003/05/soap-envelope", new FaultCode( "RequestFailed", Constants.WSTrust.NamespaceUri.Uri ) ) )
        {
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="RequestFailedFaultException"/> class. 
        /// </summary>
        /// <param name="innerException">The inner exception.</param>
        public RequestFailedFaultException( Exception innerException )
            : base( new FaultReason( "The specified request failed" ), new FaultCode( "sender", "http://www.w3.org/2003/05/soap-envelope", new FaultCode( "RequestFailed", Constants.WSTrust.NamespaceUri.Uri ) ) )
        {
        }
    }
}
