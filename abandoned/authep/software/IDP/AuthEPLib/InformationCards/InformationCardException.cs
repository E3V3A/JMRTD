/*
 * Copyright (c) Microsoft Corporation.  All rights reserved.
 * 
 * This code is licensed under the Microsoft Permissive License (Ms-PL)
 * 
 * SEE: http://www.microsoft.com/resources/sharedsource/licensingbasics/permissivelicense.mspx
 * 
 * or the EULA.TXT file that comes with this software.
 */

using System;
using System.Runtime.Serialization;

namespace nl.telin.authep.informationcards
{
    /// <summary>
    /// An exception class for Information Cards.
    /// </summary>
    [Serializable]
    public class InformationCardException : Exception
    {   
        /// <summary>
        /// Initializes a new instance of the InformationCardException.
        /// </summary>
        public InformationCardException()
        {
        }

        /// <summary>
        /// Initializes a new instance of the InformationCardException class with a specified
        ///     error message.
        /// </summary>
        /// <param name="message">The message that describes the error.</param>
        public InformationCardException(string message)
            : base(message)
        {
        }

        /// <summary>
        /// Initializes a new instance of the InformationCardException class with serialized data.
        /// </summary>
        /// <param name="info">The System.Runtime.Serialization.SerializationInfo that holds the serialized
        ///     object data about the exception being thrown.</param>
        /// <param name="context">The System.Runtime.Serialization.StreamingContext that contains contextual
        ///     information about the source or destination.</param>
        /// <exception cref="System.Runtime.Serialization.SerializationException">The class name is null or InformationCardException.HResult is zero (0).</exception>
        /// <exception cref="System.ArgumentNullException">The info parameter is null.</exception>
        protected InformationCardException(SerializationInfo info, StreamingContext context)
            : base(info, context)
        {
        }

        /// <summary>
        /// Initializes a new instance of the InformationCardException class with a specified
        ///     error message and a reference to the inner exception that is the cause of
        ///     this exception.
        /// </summary>
        /// <param name="message">The error message that explains the reason for the exception.</param>
        /// <param name="innerException">The exception that is the cause of the current exception, or a null reference
        ///     (Nothing in Visual Basic) if no inner exception is specified.</param>
        public InformationCardException(string message, Exception innerException)
            : base(message, innerException)
        {
        }

    }
}