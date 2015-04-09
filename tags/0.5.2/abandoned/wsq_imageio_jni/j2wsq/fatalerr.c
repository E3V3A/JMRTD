/***********************************************************************
      LIBRARY: UTIL - General Purpose Utility Routines

      FILE:    FATALERR.C
      AUTHOR:  Michael Garris
      DATE:    12/19/1990

      Contains routines responsible for exiting upon an application error.

      ROUTINES:
#cat: fatalerr - generic application error handler that prints a specified
#cat:            message to stderr and exits with a status of 1.

***********************************************************************/

/* LINTLIBRARY */

#include <stdio.h>
#include <stdlib.h>
#include "util.h"

void fatalerr(char *s1,char *s2,char *s3)
{

  (void) fflush(stdout);
   if (s2 == (char *) NULL)
	(void) fprintf(stderr,"ERROR: %s\n",s1);
   else if (s3 == (char *) NULL)
	(void) fprintf(stderr,"ERROR: %s: %s\n",s1,s2);
   else
	(void) fprintf(stderr,"ERROR: %s: %s: %s\n",s1,s2,s3);
   (void) fflush(stderr);

   exit(1);
}
